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
import jakarta.ws.rs.core.SecurityContext;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.tika.Tika;
import org.json.JSONArray;
import org.json.JSONObject;
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
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

@Service
public class SpecifyEndpointService {
    SpecifyProperties specifyProperties;
    AssetFileService assetFileService;

    KeycloakService keycloakService;
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ObjectWriter writer = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();

    private static final Logger logger = LoggerFactory.getLogger(SpecifyEndpointService.class);

    @Inject
    public SpecifyEndpointService(SpecifyProperties specifyProperties,
                                  AssetFileService assetFileService,
                                  KeycloakService keycloakService) {
        this.specifyProperties = specifyProperties;
        this.assetFileService = assetFileService;

        this.keycloakService = keycloakService;
    }
    public SpecifyCollectionLogin loginToCollection(String collection) {
        LoginInfo loginInfo = login();
        int specifyCollectionId = 0;
        if (loginInfo.collections.containsKey(collection)) {
            specifyCollectionId = loginInfo.collections.get(collection);
        } else {
            throw new SpecifyAdapterException("No collection was found in specify", AcknowledgeStatus.MAPPING_ERROR);
        }
        logger.info("Logging into collection {}", collection);
        logger.info("Specify collection id {}", specifyCollectionId);
        return loginToCollection(specifyCollectionId, loginInfo.csrftoken);
    }

    public List<AssetSpecimen> pushImageToSpecify(CollectionObjectAttachment collectionObjectAttachment, Asset arsAsset, boolean deleteAttachment) {
        SpecifyCollectionLogin specifyLogin = loginToCollection(arsAsset.collection);

        UploadParams uploadParams = null;
        UploadParams tombstoneParams = null;
//        for(DasscoFile dasscoFile : dasscoFiles) {


        List<AssetSpecimen> specimenWithIds = new ArrayList<>();
        for (AssetSpecimen assetSpecimen : arsAsset.asset_specimen) {
            logger.info("Updating specimen: " + assetSpecimen
            );
            CollectionObjectAttachment collectionObjectAttachmentWithIds = null;
            //Check if attachment has been deleted outside of ars
            // 5: Get Collection Object (if it exists!):
            CollectionObject collectionObject = getCollectionObject(specifyLogin, assetSpecimen.specimen.barcode());
            if (assetSpecimen.specify_collection_object_attachment_id != null && (arsAsset.date_asset_deleted != null || assetSpecimen.asset_detached)) {
                // tombstone
                logger.info("In tombstone");

                for (CollectionObjectAttachment coath : collectionObject.collectionobjectattachments) {
                    if (coath.id.equals(assetSpecimen.specify_collection_object_attachment_id)) {
                        coath.collectionmemberid = collectionObject.collectionmemberid;
                        coath.collectionobject = "/api/specify/collectionobject/" + collectionObject.id;
//                    attachmentToUpdate.version = attachmentToUpdate.version == null ? 1 : attachmentToUpdate.version;
                        moveValuesToExisting(collectionObjectAttachment.attachment, coath.attachment);
                        if (tombstoneParams == null) {
                            tombstoneParams = tombstoneAttachment(specifyLogin, coath, arsAsset);
                            collectionObjectAttachment.attachment.mimetype = coath.attachment.mimetype;
                        }
                        coath.attachment.attachmentlocation = tombstoneParams.attachmentLocation;
                        logger.info("Tombstoning collectionObjectAttachment: {}", coath.toString());
                        putCollectionObjectAttachment(coath, specifyLogin);
                        AssetSpecimen updated = new AssetSpecimen(assetSpecimen.asset_detached, null, assetSpecimen.asset_preparation_type, assetSpecimen.specimen_pid, assetSpecimen.asset_guid);
                        updated.specimen = assetSpecimen.specimen;

                        specimenWithIds.add(updated);
                    }
                }

            } else if (assetSpecimen.specify_collection_object_attachment_id != null) {
                // update
                logger.info("In update attachment");

                for (CollectionObjectAttachment coath : collectionObject.collectionobjectattachments) {
                    if (coath.id.equals(assetSpecimen.specify_collection_object_attachment_id)) {
                        logger.info("found attachment to update");
                        coath.collectionmemberid = collectionObject.collectionmemberid;
                        coath.collectionobject = "/api/specify/collectionobject/" + collectionObject.id;
//                    attachmentToUpdate.version = attachmentToUpdate.version == null ? 1 : attachmentToUpdate.version;
//                    attachmentToUpdate.attachment.version = attachmentToUpdate.attachment.version == null || attachmentToUpdate.attachment.version == 0 ? 2 : attachmentToUpdate.attachment.version;
                        moveValuesToExisting(collectionObjectAttachment.attachment, coath.attachment);
                        if (uploadParams == null) {
                            uploadParams = uploadFile(specifyLogin, coath, arsAsset);
                            collectionObjectAttachment.attachment.mimetype = coath.attachment.mimetype;
                        }
                        coath.attachment.attachmentlocation = uploadParams.attachmentLocation;
                        logger.info("Updating collectionObjectAttachment: {}", collectionObjectAttachment.toString());
                        CollectionObjectAttachment coaWithId = putCollectionObjectAttachment(coath, specifyLogin);
                        AssetSpecimen updated = new AssetSpecimen(assetSpecimen.asset_detached, coaWithId.id, assetSpecimen.asset_preparation_type, assetSpecimen.specimen_pid, assetSpecimen.asset_guid);
                        updated.specimen = assetSpecimen.specimen;
                        specimenWithIds.add(updated);
                    }
                }
            } else if (arsAsset.date_asset_deleted == null && !assetSpecimen.asset_detached) {
                // create
                logger.info("Creating new attachment in specify");
                collectionObjectAttachment.collectionmemberid = collectionObject.collectionmemberid;
                collectionObjectAttachment.collectionobject = "/api/specify/collectionobject/" + collectionObject.id;
                collectionObjectAttachment.version = 1;
                collectionObjectAttachment.attachment.version = 1;
                if (uploadParams == null) {
                    uploadParams = uploadFile(specifyLogin, collectionObjectAttachment, arsAsset);
                    collectionObjectAttachment.attachment.mimetype = collectionObjectAttachment.attachment.mimetype;
                }
                collectionObjectAttachment.attachment.attachmentlocation = uploadParams.attachmentLocation;
                CollectionObjectAttachment coaWithId = postCollectionObjectAttachment(collectionObjectAttachment, specifyLogin);
                AssetSpecimen newAssetSpecimen = new AssetSpecimen(assetSpecimen.asset_detached, coaWithId.id, assetSpecimen.asset_preparation_type, assetSpecimen.specimen_pid, assetSpecimen.asset_guid);
                newAssetSpecimen.specimen = assetSpecimen.specimen;
                specimenWithIds.add(newAssetSpecimen);
            }

            collectionObjectAttachmentWithIds = null;
            if (deleteAttachment) {
                // delete
            }

            // 13: Log out the user:
        }
        logout(specifyLogin);
        return specimenWithIds;
    }

    private UploadParams tombstoneAttachment(SpecifyCollectionLogin specifyLogin, CollectionObjectAttachment attachmentToUpdate, Asset arsAsset) {
        String token = keycloakService.getUserServiceToken();
        // 6: Get files in ERDA:
//        List<String> files = assetFileService.getAssetFiles(arsAsset.asset_guid, token);
//        files.forEach(s -> logger.info("Asset has file: {}", s));
        // 7: Sanitize the list of files to only get the filenames:
        String fileName = arsAsset.asset_guid + "-tombstone.json";
        // 8: Get Upload Params:
        List<UploadParams> uploadParams = getUploadParams(specifyLogin, List.of(fileName));

//        Tika tika = new Tika();
//        String[] parts = files.get(0).split("/");
//        String fileInstitution = parts[2];
//        String fileCollection = parts[3];
//        String asset = parts[4];
//        String path = parts[5];
//        String filename = parts[parts.length - 1];
//        String mimeType = tika.detect(filename);
        // 10.b: Get token and attachmentLocation from the uploadParams:
//            JSONObject uploadParam = uploadParams.getJSONObject(i);
        UploadParams uploadParam = uploadParams.get(0);
        String attachmentLocation = uploadParam.attachmentLocation;
        String attachmentToken = uploadParam.token;
        // 10.c: Fetch the file:
        InputStream inputStream = null;
        attachmentToUpdate.attachment.mimetype = "application/json";
        try {
            inputStream = new ByteArrayInputStream(writer.writeValueAsBytes(arsAsset));
            // 10.d: Upload file to the asset server:
            uploadFile(attachmentToken, attachmentLocation, arsAsset.collection, inputStream, fileName);
        } catch (JsonProcessingException e) {
            logger.error("Failed to tombstone asset", e);
            throw new SpecifyAdapterException("Failed to write asset object to file", AcknowledgeStatus.FILE_UPLOAD_ERROR);
        }
        return uploadParam;
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


    public UploadParams uploadFile(SpecifyCollectionLogin specifyLogin, CollectionObjectAttachment collectionObjectAttachment, Asset arsAsset) {
        String token = keycloakService.getUserServiceToken();
        // 6: Get files in ERDA:
        List<String> files = assetFileService.getAssetFiles(arsAsset.asset_guid, token);
        files.forEach(s -> logger.info("Asset has file: {}", s));
        // 7: Sanitize the list of files to only get the filenames:
        List<String> filenames = files.stream().map(url -> url.substring(url.lastIndexOf('/') + 1)).toList();
        if (files.size() != 1) {
            throw new SpecifyAdapterException("The adapter can only handle Assets with one attachment", AcknowledgeStatus.FILE_UPLOAD_ERROR);
        }
        // 8: Get Upload Params:
        List<UploadParams> uploadParams = getUploadParams(specifyLogin, filenames);

        Tika tika = new Tika();
        String[] parts = files.get(0).split("/");
        String fileInstitution = parts[2];
        String fileCollection = parts[3];
        String asset = parts[4];
        String path = parts[5];
        String filename = parts[parts.length - 1];
        String mimeType = tika.detect(filename);
        collectionObjectAttachment.attachment.mimetype = mimeType;
        // 10.b: Get token and attachmentLocation from the uploadParams:
//            JSONObject uploadParam = uploadParams.getJSONObject(i);
        UploadParams uploadParam = uploadParams.get(0);
        String attachmentLocation = uploadParam.attachmentLocation;
        String attachmentToken = uploadParam.token;
        // 10.c: Fetch the file:
        InputStream inputStream = assetFileService.fetchFiles(fileInstitution, fileCollection, asset, path, token);
        // 10.d: Upload file to the asset server:
        uploadFile(attachmentToken, attachmentLocation, arsAsset.collection, inputStream, filename);
        return uploadParam;
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

    public LoginInfo login() {

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/context/login/"))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                LoginInfo loginInfo = mapper.readValue(response.body(), LoginInfo.class);
                List<HttpCookie> cookies = ((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies();
                for (HttpCookie cookie : cookies) {
                    if ("csrftoken".equalsIgnoreCase(cookie.getName())) {
                        loginInfo.csrftoken = cookie.getValue();
                        logger.info("csrftoken: {}", loginInfo.csrftoken);
                        return loginInfo;
                    }
                }
                throw new RuntimeException("There was no csrftoken cookie in the response.");
            } else {
                logger.error("There was an error while trying to login using the specify API, response code: " + response.statusCode());
                throw new RuntimeException("There has been an error logging in: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public SpecifyCollectionLogin loginToCollection(int collection, String csrfToken) {

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();

        HashMap<String, String> credentials = new HashMap<>();
        credentials.put("username", this.specifyProperties.username());
        credentials.put("password", this.specifyProperties.password());
        credentials.put("collection", String.valueOf(collection));
//        String requestBody = String.format(
//                "{\"username\":\"%s\",\"password\":\"%s\",\"collection\":%d}",
//                this.specifyProperties.username(),
//                this.specifyProperties.password(),
//                collection
//        );
        try {
            String requestBody = writer.writeValueAsString(credentials);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.specifyProperties.rootUrl() + "/context/login/"))
                    .header("X-CSRFToken", csrfToken)
                    .header("Cookie", "csrftoken=" + csrfToken)
                    .header("Content-Type", "application/json")
                    .header("Referer", this.specifyProperties.rootUrl() + "/")
                    .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();


            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                CookieStore cookieStore = cookieManager.getCookieStore();
                List<HttpCookie> cookies = cookieStore.getCookies();
                String sessionId = "";
                String collectionIdAsString = "";
                String newCsrfToken = null;
                for (HttpCookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase("csrftoken")) {
                        newCsrfToken = cookie.getValue();
                    }
                    if (cookie.getName().equalsIgnoreCase("sessionid")) {
                        sessionId = cookie.getValue();
                    }
                    if (cookie.getName().equalsIgnoreCase("collection")) {
                        collectionIdAsString = cookie.getValue();
                    }
                }
                return new SpecifyCollectionLogin(sessionId, newCsrfToken, collectionIdAsString);
            } else if (response.statusCode() == 403) {
                throw new RuntimeException("Forbidden. There has been a problem logging into the Collection. Most likely scenario is the CSRF Token being wrong.");
            }

            throw new RuntimeException("There has been an error.");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAgent(String csrfToken, String collectionId, String sessionId) {
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create((this.specifyProperties.rootUrl() + "/context/user.json")))
                .header("X-CSRFToken", csrfToken)
                .header("Cookie", "collection=" + collectionId + ";csrftoken=" + csrfToken + ";sessionid=" + sessionId)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONObject agent = jsonResponse.getJSONObject("agent");
                return agent.getString("resource_uri");

            } else if (response.statusCode() == 403) {
                throw new RuntimeException("Forbidden. Most likely scenario is a mistake with the CSRF Token.");
            } else {
                throw new RuntimeException("There was an error, response code was: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("There was an error fetching data from the logged in user: ", e);
        }
    }

    public void logout(SpecifyCollectionLogin login) {
        HttpClient httpClient = HttpClient.newBuilder().build();

        String requestBody = String.format(
                "{\"username\": null,\"password\": null,\"collection\":%s}",
                login.collection()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/context/login/"))
                .header("X-CSRFToken", login.csrftoken())
                .header("Cookie", "csrftoken=" + login.csrftoken())
                .header("Content-Type", "application/json")
                .header("Referer", this.specifyProperties.rootUrl() + "/")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                throw new RuntimeException("Forbidden. Most likely scenario is an error with the CSRF Token");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException();
        }
    }


    public List<UploadParams> getUploadParams(SpecifyCollectionLogin login, List<String> filenames) {

        HttpClient httpClient = HttpClient.newBuilder().build();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("filenames", new JSONArray(filenames));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/attachment_gw/get_upload_params/"))
                .header("X-CSRFToken", login.csrftoken())
                .header("Cookie", "collection=" + login.collection() + ";csrftoken=" + login.csrftoken() + ";sessionid=" + login.sessionid())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String json = response.body();
                return Arrays.asList(mapper.readValue(json, UploadParams[].class));
            } else if (response.statusCode() == 403) {
                throw new RuntimeException("Forbidden. Most likely scenario is a fail in the CSRF token.");
            } else {
                String json = response.body();
                logger.error(json);
                throw new RuntimeException("Something failed when getting the Upload Params");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public void uploadFile(String attachmentToken, String attachmentLocation, String collectionName, InputStream inputStream, String filename) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpPost uploadFile = new HttpPost(this.specifyProperties.assetServer() + "/fileupload");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("token", attachmentToken, ContentType.TEXT_PLAIN);
            builder.addTextBody("store", attachmentLocation, ContentType.TEXT_PLAIN);
            builder.addTextBody("type", "O", ContentType.TEXT_PLAIN);
            builder.addTextBody("coll", collectionName, ContentType.TEXT_PLAIN);
            builder.addBinaryBody("file", inputStream, ContentType.APPLICATION_OCTET_STREAM, filename);

            uploadFile.setEntity(builder.build());

            HttpEntity response = httpClient.execute(uploadFile, classicHttpResponse -> {
                int status = classicHttpResponse.getCode();
                HttpEntity entity = classicHttpResponse.getEntity();

                String responseBody = EntityUtils.toString(entity);

                if (status == 200) {
                    return classicHttpResponse.getEntity();
                } else {
                    throw new IOException("This happened: " + classicHttpResponse.getEntity().toString());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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

}

// TODO:
// Ask about Collections. Do they already know the collection number? Should I pass it? For now, it's hardcoded.
// Should username and password for login into collection be passed as a POST message or should they be in the application.properties file?
// Preparations? I have hardcoded preparation: none for this, but how will it work later on?
// Methods for CREATING a new Collection Object from scratch exist in the code. I wouldn't delete them for now, maybe they will be useful later.