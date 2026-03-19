package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.assets.SpecifyProperties;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.*;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyAttachmentContext;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SpecifyQueryService {
    private final SpecifyProperties specifyProperties;
//    AssetFileService assetFileService;

//    KeycloakService keycloakService;
    final SpecifyEndpointService specifyEndpointService;
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ObjectWriter writer = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();

    private static final Logger logger = LoggerFactory.getLogger(SpecifyQueryService.class);
    private static final String VASCULAR_PLANTS_COLLECTION = "NHMD Vascular Plants";
    private static final int ATTACHMENT_PAGE_SIZE = 20;
    private static final int MAX_UPDATED_ATTACHMENTS = 100;
    private static final DateTimeFormatter SPECIFY_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId SPECIFY_TIMEZONE = ZoneId.of("Europe/Copenhagen");
    @Inject
    public SpecifyQueryService(SpecifyEndpointService specifyEndpointService, SpecifyProperties specifyProperties) {
        this.specifyEndpointService = specifyEndpointService;
        this.specifyProperties = specifyProperties;
    }

    public List<CollectionObject> findCollectionObjectsToSync(@NotNull String fromTimestamp, String toTimestamp) {

        // 2: Log In to Specify:
        LoginInfo loginInfo = specifyEndpointService.login();
//        String csrfToken = loginMap.get("csrftoken").toString();
        // 3: Get Asset Institution and Collection Mapping:
        String collection = VASCULAR_PLANTS_COLLECTION;
        //        Object collectionObj = loginMap.get("collections");

        int specifyCollectionId = 0;
//        if (collectionObj instanceof JSONObject collections) {
        if (loginInfo.collections.containsKey(collection)) {
            specifyCollectionId = loginInfo.collections.get(collection);
        } else {
            throw new SpecifyAdapterException("No collection was found in specify", AcknowledgeStatus.MAPPING_ERROR);
        }
//        }
        logger.info("Logging into collection {}", collection);
        logger.info("Specify collection id {}", specifyCollectionId);
        // 4: Login to Collection:
        SpecifyCollectionLogin specifyLogin = specifyEndpointService.loginToCollection(specifyCollectionId, loginInfo.csrftoken);
        String json = UPDATED_SINCE_QUERY;
        String postbody = json.replace("<from_timestamp>", fromTimestamp).replace("<to_timestamp>", toTimestamp);
        List<CollectionObject> foundCollectionObjects = new ArrayList<>();
        SpecifyQueryResult specifyQueryResult = querySpecify(postbody, specifyLogin);
        specifyQueryResult.results.forEach(x -> {
            if(x.size() == 2) {
                logger.info("found collectionObject with id {}", x.get(0)  );
                logger.info("found collectionObject with last modified {}", x.get(1)  );
                Optional<CollectionObject> collectionObjectOpt = getCollectionObject(specifyLogin, (Integer) x.get(0));
                logger.info("found collectionObject {}", collectionObjectOpt.isPresent());
                collectionObjectOpt.ifPresent(foundCollectionObjects::add);
            }
        });
        return foundCollectionObjects;
    }

    public List<SpecifyAttachmentContext> findUpdatedAttachmentContextsSince(@NotNull Instant lastSyncTimestamp) {
        LoginInfo loginInfo = specifyEndpointService.login();
        int specifyCollectionId;
        if (loginInfo.collections.containsKey(VASCULAR_PLANTS_COLLECTION)) {
            specifyCollectionId = loginInfo.collections.get(VASCULAR_PLANTS_COLLECTION);
        } else {
            throw new SpecifyAdapterException("No collection was found in specify", AcknowledgeStatus.MAPPING_ERROR);
        }

        SpecifyCollectionLogin specifyLogin = specifyEndpointService.loginToCollection(specifyCollectionId, loginInfo.csrftoken);
        LocalDateTime lastSyncLocal = LocalDateTime.ofInstant(lastSyncTimestamp, SPECIFY_TIMEZONE);
        List<SpecifyAttachmentContext> contexts = new ArrayList<>();
        Map<String, Agent> agentByUri = new HashMap<>();
        Map<String, PrepType> prepTypeByUri = new HashMap<>();
        Map<Integer, List<PrepType>> prepTypesByCollectionObjectId = new HashMap<>();
        Map<String, CollectionObject> collectionObjectByUri = new HashMap<>();

        int offset = 0;
        int updatedAttachmentCount = 0;
        while (true) {
            AttachmentSearchResult result = specifyEndpointService.getSpecifyObject(
                    specifyLogin,
                    "/api/specify/attachment/?domainfilter=true&offset=" + offset + "&orderby=-timestampmodified",
                    AttachmentSearchResult.class
            );
            if (result == null || result.objects == null || result.objects.isEmpty()) {
                break;
            }

            boolean allAfterLastSync = true;
            for (Attachment attachment : result.objects) {
                if (!isAfterLastSync(attachment.timestampmodified, lastSyncLocal)) {
                    allAfterLastSync = false;
                    continue;
                }

                updatedAttachmentCount++;
                if (updatedAttachmentCount > MAX_UPDATED_ATTACHMENTS) {
                    throw new RuntimeException("Found more than " + MAX_UPDATED_ATTACHMENTS + " attachments updated since last sync");
                }

                Agent modifiedByAgent = getAgent(specifyLogin, attachment.modifiedbyagent, agentByUri);
                CollectionObjectAttachmentSearchResult collectionObjectAttachmentResult = getCollectionObjectAttachments(specifyLogin, attachment.collectionobjectattachments);

                if (collectionObjectAttachmentResult == null || collectionObjectAttachmentResult.objects == null || collectionObjectAttachmentResult.objects.isEmpty()) {
                    continue;
                }

                for (CollectionObjectAttachment collectionObjectAttachment : collectionObjectAttachmentResult.objects) {
                    if (collectionObjectAttachment.collectionobject == null) {
                        continue;
                    }
                    CollectionObject collectionObject = collectionObjectByUri.computeIfAbsent(
                            collectionObjectAttachment.collectionobject,
                            uri -> specifyEndpointService.getSpecifyObject(specifyLogin, uri, CollectionObject.class)
                    );
                    List<PrepType> prepTypes = prepTypesByCollectionObjectId.computeIfAbsent(
                            collectionObject.id,
                            id -> getPrepTypesForCollectionObject(collectionObject, specifyLogin, prepTypeByUri)
                    );
                    contexts.add(new SpecifyAttachmentContext(attachment, modifiedByAgent, collectionObject, prepTypes, collectionObjectAttachment.id));
                }
            }

            if (!allAfterLastSync || result.objects.size() < ATTACHMENT_PAGE_SIZE) {
                break;
            }
            offset += ATTACHMENT_PAGE_SIZE;
        }
        return contexts;
    }

    private boolean isAfterLastSync(String specifyTimestamp, LocalDateTime lastSyncLocal) {
        if (specifyTimestamp == null) {
            return false;
        }
        LocalDateTime modified = LocalDateTime.parse(specifyTimestamp, SPECIFY_DATE_FORMAT);
        return modified.isAfter(lastSyncLocal);
    }

    private Agent getAgent(SpecifyCollectionLogin login, String agentUri, Map<String, Agent> agentByUri) {
        if (agentUri == null) {
            return null;
        }
        return agentByUri.computeIfAbsent(agentUri, uri -> specifyEndpointService.getSpecifyObject(login, uri, Agent.class));
    }

    private CollectionObjectAttachmentSearchResult getCollectionObjectAttachments(SpecifyCollectionLogin login, String collectionObjectAttachmentsUri) {
        if (collectionObjectAttachmentsUri == null) {
            return null;
        }
        return specifyEndpointService.getSpecifyObject(login, collectionObjectAttachmentsUri, CollectionObjectAttachmentSearchResult.class);
    }

    private List<PrepType> getPrepTypesForCollectionObject(CollectionObject collectionObject, SpecifyCollectionLogin login, Map<String, PrepType> prepTypeByUri) {
        List<PrepType> prepTypes = new ArrayList<>();
        if (collectionObject.preparations == null || collectionObject.preparations.isEmpty()) {
            return prepTypes;
        }
        for (Preparation preparation : collectionObject.preparations) {
            if (preparation.preptype == null) {
                continue;
            }
            PrepType prepType = prepTypeByUri.computeIfAbsent(
                    preparation.preptype,
                    uri -> specifyEndpointService.getSpecifyObject(login, uri, PrepType.class)
            );
            prepTypes.add(prepType);
        }
        return prepTypes;
    }



    public SpecifyQueryResult querySpecify(String postbody, SpecifyCollectionLogin login) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.specifyProperties.rootUrl() + "/stored_query/ephemeral/"))
                    .header("Cookie", "collection=" + login.collection() + ";csrftoken=" + login.csrftoken() + ";sessionid=" + login.sessionid())
                    .header("X-CSRFToken", login.csrftoken())
                    .POST(HttpRequest.BodyPublishers.ofString(postbody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.info(response.headers().toString());

                throw new RuntimeException("There was an error. Status: " + response.statusCode() + ". Error: " + response.body());
            }
            return mapper.readValue(response.body(), SpecifyQueryResult.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public CollectionObjectAttachment putCollectionObjectAttachment(CollectionObjectAttachment collectionObjectAttachment, SpecifyCollectionLogin login) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        logger.info("Updating existing collectionobjectattachment: {}", collectionObjectAttachment.id);
        try {
            String json = writer.writeValueAsString(collectionObjectAttachment);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collectionobjectattachment/" + collectionObjectAttachment.id + "/"))
                    .header("Cookie", "collection=" + login.collection() + ";csrftoken=" + login.csrftoken() + ";sessionid=" + login.sessionid())
                    .header("X-CSRFToken", login.csrftoken())
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.info(response.headers().toString());

                throw new RuntimeException("There was an error. Status: " + response.statusCode() + ". Error: " + response.body());
            }
            return mapper.readValue(response.body(), CollectionObjectAttachment.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }





    public void moveValuesToExisting(Attachment withARSValues, Attachment fromSpecify) {
        fromSpecify.remarks = withARSValues.remarks;
        fromSpecify.mimetype = withARSValues.mimetype;
        fromSpecify.origfilename = withARSValues.origfilename;
        fromSpecify.title = withARSValues.title;
        fromSpecify.ispublic = withARSValues.ispublic;
        fromSpecify.copyrightdate = withARSValues.copyrightdate;
        fromSpecify.copyrightholder = withARSValues.copyrightholder;
        fromSpecify.license = withARSValues.license;
        fromSpecify.credit = withARSValues.credit;
    }

//    https://specify-test3.science.ku.dk/api/specify/collectionobject/6555171/
    public Optional<CollectionObject> getCollectionObject(SpecifyCollectionLogin login, int collectionObjectId) {
        HttpClient httpClient = HttpClient.newBuilder()
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collectionobject/" + collectionObjectId + "/"))
                .header("Cookie", "collection=" + login.collection() + ";csrftoken=" + login.csrftoken() + ";sessionid=" + login.sessionid())
                .header("X-CSRFToken", login.csrftoken())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                CollectionObject collectionObject = mapper.readValue(response.body(), CollectionObject.class);
                return Optional.of(collectionObject);
            } else if(response.statusCode() == 404) {
                logger.info("CollectionObject with id {} not found", collectionObjectId);
            } else {
                logger.error("There was an error getting collectionObject. Status: " + response.statusCode());
                throw new RuntimeException("Error: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public CollectionObjectAttachment postCollectionObjectAttachment(CollectionObjectAttachment collectionObjectAttachment
            , SpecifyCollectionLogin login) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        try {
            String json = writer.writeValueAsString(collectionObjectAttachment);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collectionobjectattachment/"))
                    .header("Cookie", "collection=" + login.collection() + ";csrftoken=" + login.csrftoken() + ";sessionid=" + login.sessionid())
                    .header("X-CSRFToken", login.csrftoken())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            System.out.println(request);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new RuntimeException("There was an error. Status: " + response.statusCode() + ". Error: " + response.body());
            }
            return mapper.readValue(response.body(), CollectionObjectAttachment.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<CollectionObject> searchSpecify() {
        List<CollectionObject> collectionObjects = new ArrayList<>();

        return collectionObjects;
    }

    private static final String UPDATED_SINCE_QUERY = """
    {
        "name": "New Query",
        "contextname": "CollectionObject",
        "contexttableid": 1,
        "selectdistinct": false,
        "smushed": false,
        "countonly": false,
        "formatauditrecids": false,
        "specifyuser": "/api/specify/specifyuser/186/",
        "isfavorite": true,
        "ordinal": 32767,
        "fields": [
          {
            "tablelist": "1",
            "stringid": "1.collectionobject.timestampModified",
            "fieldname": "timestampModified",
            "isrelfld": false,
            "sorttype": 0,
            "position": 0,
            "isdisplay": true,
            "operstart": 2,
            "startvalue": "<from_timestamp>",
            "isnot": false,
            "isstrict": false
          }
        ],
        "_tablename": "SpQuery",
        "remarks": null,
        "searchsynonymy": null,
        "sqlstr": null,
        "timestampcreated": "2026-01-27",
        "timestampmodified": null,
        "version": 1,
        "createdbyagent": null,
        "modifiedbyagent": null,
        "offset": 0,
        "limit": 40
      }
""";
}

// TODO:
// Ask about Collections. Do they already know the collection number? Should I pass it? For now, it's hardcoded.
// Should username and password for login into collection be passed as a POST message or should they be in the application.properties file?
// Preparations? I have hardcoded preparation: none for this, but how will it work later on?
// Methods for CREATING a new Collection Object from scratch exist in the code. I wouldn't delete them for now, maybe they will be useful later.
