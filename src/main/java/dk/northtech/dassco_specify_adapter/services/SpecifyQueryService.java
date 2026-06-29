package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.assets.SpecifyProperties;
import dk.northtech.dassco_specify_adapter.configuration.SpecifySyncProperties;
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
    private final SpecifySyncProperties specifySyncProperties;
    private final ZoneId specifyTimezone;
//    AssetFileService assetFileService;

//    KeycloakService keycloakService;
    final SpecifyEndpointService specifyEndpointService;
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ObjectWriter writer = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();

    private static final Logger logger = LoggerFactory.getLogger(SpecifyQueryService.class);
    private static final DateTimeFormatter SPECIFY_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Inject
    public SpecifyQueryService(SpecifyEndpointService specifyEndpointService,
                               SpecifyProperties specifyProperties,
                               SpecifySyncProperties specifySyncProperties) {
        this.specifyEndpointService = specifyEndpointService;
        this.specifyProperties = specifyProperties;
        this.specifySyncProperties = specifySyncProperties;
        validateSyncProperties(specifySyncProperties);
        this.specifyTimezone = ZoneId.of(specifySyncProperties.specifyTimezone().trim());
    }



    public List<SpecifyAttachmentContext> findUpdatedAttachmentContextsSince(ResolvedSpecifyTarget target, @NotNull Instant lastSyncTimestamp) {
        LoginInfo loginInfo = specifyEndpointService.login(target.institutionConfig().id());
        Integer specifyCollectionId = loginInfo.collections.get(target.collectionConfig().name());
        if (specifyCollectionId == null) {
            throw new SpecifyAdapterException("No collection was found in specify for collection config '" + target.collectionConfig().name() + "'", AcknowledgeStatus.MAPPING_ERROR);
        }

        SpecifyCollectionLogin specifyLogin = specifyEndpointService.loginToCollection(target.institutionConfig().id(), specifyCollectionId, loginInfo.csrftoken);
        LocalDateTime lastSyncLocal = LocalDateTime.ofInstant(lastSyncTimestamp, specifyTimezone);
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
                if (updatedAttachmentCount > specifySyncProperties.maxUpdatedAttachments()) {
                    throw new RuntimeException("Found more than " + specifySyncProperties.maxUpdatedAttachments() + " attachments updated since last sync");
                }
                Agent createdByAgent =  getAgent(specifyLogin, attachment.createdbyagent, agentByUri);
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
                    contexts.add(new SpecifyAttachmentContext(attachment, createdByAgent, collectionObject, prepTypes, collectionObjectAttachment.id));
                }
            }

            if (!allAfterLastSync || result.objects.size() < specifySyncProperties.attachmentPageSize()) {
                break;
            }
            offset += specifySyncProperties.attachmentPageSize();
        }
        return contexts;
    }

    private void validateSyncProperties(SpecifySyncProperties properties) {
        if (properties.maxUpdatedAttachments() <= 0) {
            throw new IllegalStateException("specify-sync.maxUpdatedAttachments must be greater than 0");
        }
        if (properties.attachmentPageSize() <= 0) {
            throw new IllegalStateException("specify-sync.attachmentPageSize must be greater than 0");
        }
        if (properties.specifyTimezone() == null || properties.specifyTimezone().isBlank()) {
            throw new IllegalStateException("specify-sync.specifyTimezone must be configured");
        }
        try {
            ZoneId.of(properties.specifyTimezone().trim());
        } catch (Exception exception) {
            throw new IllegalStateException("specify-sync.specifyTimezone is invalid", exception);
        }
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
                    .uri(URI.create(login.rootUrl() + "/stored_query/ephemeral/"))
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
                .uri(URI.create(login.rootUrl() + "/api/specify/collectionobject/" + collectionObjectId + "/"))
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
                    .uri(URI.create(login.rootUrl() + "/api/specify/collectionobjectattachment/"))
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
