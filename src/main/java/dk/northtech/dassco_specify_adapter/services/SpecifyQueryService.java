package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.assets.SpecifyProperties;
import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.domain.specify.*;
import jakarta.inject.Inject;
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
import java.util.ArrayList;
import java.util.List;

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
    @Inject
    public SpecifyQueryService(SpecifyEndpointService specifyEndpointService, SpecifyProperties specifyProperties) {
        this.specifyEndpointService = specifyEndpointService;
        this.specifyProperties = specifyProperties;
    }

    public void findCollectionObjectsToSync() {

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
        String postbody = json.replace("<from_timestamp>", "2026-01-31");

        SpecifyQueryResult specifyQueryResult = querySpecify(postbody, specifyLogin);
        specifyQueryResult.results.forEach(x -> {
            if(x.size() == 2) {
                logger.info("found collectionObject with id {}", x.get(0)  );
                logger.info("found collectionObject with last modified {}", x.get(1)  );
            }
        });
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


    public CollectionObject getCollectionObject(SpecifyCollectionLogin login, String barcode) {
        HttpClient httpClient = HttpClient.newBuilder()
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collectionobject/?catalognumber=" + barcode))
                .header("Cookie", "collection=" + login.collection() + ";csrftoken=" + login.csrftoken() + ";sessionid=" + login.sessionid())
                .header("X-CSRFToken", login.csrftoken())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                CollectionObjectSearchResult collectionObjectSearchResult = mapper.readValue(response.body(), CollectionObjectSearchResult.class);
                if (!collectionObjectSearchResult.objects.isEmpty()) {
                    return collectionObjectSearchResult.objects.getFirst();
                } else {
                    throw new SpecifyAdapterException("The Specimen does not exist in Specify. Please create a Collection Object for this Specimen.", AcknowledgeStatus.SPECIMEN_NOT_FOUND_ERROR);
                }
            } else {
                throw new RuntimeException("Error: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
