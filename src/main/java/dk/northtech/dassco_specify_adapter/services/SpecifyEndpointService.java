package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.assets.SpecifyProperties;
import dk.northtech.dassco_specify_adapter.domain.Asset;
import dk.northtech.dassco_specify_adapter.domain.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SpecifyEndpointService {
    SpecifyProperties specifyProperties;
    AssetFileService assetFileService;
    AssetService assetService;

    @Inject
    public SpecifyEndpointService(SpecifyProperties specifyProperties,
                                  AssetFileService assetFileService,
                                  AssetService assetService){
        this.specifyProperties = specifyProperties;
        this.assetFileService = assetFileService;
        this.assetService = assetService;
    }

    public Response pushImageToSpecify(String assetGuid, User user){
        // 1. Get Asset Metadata:
        Asset erdaAsset = assetService.getAsset(assetGuid, user);
        // 2: Log In to Specify:
        Map<String, Object> loginMap = login();
        String csrfToken = loginMap.get("csrftoken").toString();
        // 3: Get Asset Institution and Collection Mapping:
        String institution = erdaAsset.institution;
        String collection = erdaAsset.collection;
        String collectionToSearch = institution + " " + collection;
        Object collectionObj = loginMap.get("collections");
        int specifyCollectionId = 0;
        if (collectionObj instanceof JSONObject collections){
            if (collections.has(collectionToSearch)){
                specifyCollectionId = collections.getInt(collectionToSearch);
            }
        }
        // 4: Login to Collection:
        List<HttpCookie> cookies = loginToCollection(specifyCollectionId, csrfToken);
        String sessionId = "";
        String collectionId = "";
        for (HttpCookie cookie : cookies){
            if (cookie.getName().equalsIgnoreCase("csrftoken")){
                csrfToken = cookie.getValue();
            }
            if (cookie.getName().equalsIgnoreCase("sessionid")){
                sessionId = cookie.getValue();
            }
            if (cookie.getName().equalsIgnoreCase("collection")){
                collectionId = cookie.getValue();
            }
        }
        // 5: Get Collection Object (if it exists!):
        String barcode = erdaAsset.specimens.getFirst().barcode();
        JSONObject collectionObject = getCollectionObject(csrfToken, collectionId, sessionId, barcode);
        // 6: Get files in ERDA:
        List<String> files = assetFileService.getAssetFiles(assetGuid, user);
        // 7: Sanitize the list of files to only get the filenames:
        List<String> filenames = files.stream().map(url -> url.substring(url.lastIndexOf('/') + 1)).toList();
        // 8: Get Upload Params:
        JSONArray uploadParams = getUploadParams(csrfToken, sessionId, collectionId, filenames);
        // 9: Get Collection Info:
        JSONObject collectionInfo = getCollectionInfo(csrfToken, sessionId, collectionId);
        String collectionName = collectionInfo.getString("collectionname");
        //String collectionResource = collectionInfo.getString("resource_uri");
        //String collectionDiscipline = collectionInfo.getString("discipline");
        Tika tika = new Tika();
        JSONArray collectionObjectAttachments = new JSONArray();
        // 10: For each File in filenames, get the Stream.
        for (int i = 0; i < files.size(); i++){
            // 10.a: Get institution, collection, asset and path:
            String[] parts = files.get(i).split("/");
            String fileInstitution = parts[2];
            String fileCollection = parts[3];
            String asset = parts[4];
            String path = parts[5];
            String filename = parts[parts.length - 1];
            // 10.b: Get token and attachmentLocation from the uploadParams:
            JSONObject uploadParam = uploadParams.getJSONObject(i);
            String attachmentLocation = uploadParam.getString("attachmentLocation");
            String attachmentToken = uploadParam.getString("token");
            // 10.c: Fetch the file:
            InputStream inputStream = assetFileService.fetchFiles(fileInstitution, fileCollection, asset, path, user);
            // 10.d: Upload file to the asset server:
            createFormData(attachmentToken, attachmentLocation, collectionName, inputStream, filename);
            // 10.e: Get mime type
            String mimeType = tika.detect(filename);
            // 10.f: Make the attachment resource:
            JSONObject attachmentResource = createAttachmentResource(attachmentLocation, mimeType, filename, i);
            collectionObjectAttachments.put(attachmentResource);
        }
        // 11: Add Attachments to CollectionObject:
        collectionObject.put("collectionobjectattachments", collectionObjectAttachments);
        // 12: PUT new collectionObject:
        int collectionObjectId = collectionObject.getInt("id");
        putCollectionObject(collectionId, csrfToken, sessionId, collectionObjectId, collectionObject);
        // 13: Log out the user:
        logout(csrfToken, collectionId);
        return Response.status(200).entity("Attachment from " + erdaAsset.asset_guid + " uploaded successfully to Collection Object with ID: " + collectionObjectId).build();
    }


    public Map<String, Object> login(){

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
            if (response.statusCode() == 200){
                Map<String, Object> responseMap = new HashMap<>();
                List<HttpCookie> cookies = ((CookieManager) CookieHandler.getDefault()).getCookieStore().getCookies();
                for (HttpCookie cookie : cookies){
                    if ("csrftoken".equalsIgnoreCase(cookie.getName())){
                        responseMap.put("csrftoken", cookie.getValue());
                        JSONObject jsonObject = new JSONObject(response.body());
                        responseMap.put("collections", jsonObject.getJSONObject("collections"));
                        return responseMap;
                    }
                }
                throw new RuntimeException("There was no csrftoken cookie in the response.");
            } else {
                throw new RuntimeException("There has been an error logging in: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<HttpCookie> loginToCollection(int collection, String csrfToken){

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();

        String requestBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"collection\":%d}",
                this.specifyProperties.username(),
                this.specifyProperties.password(),
                collection
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/context/login/"))
                .header("X-CSRFToken", csrfToken)
                .header("Cookie", "csrftoken=" + csrfToken)
                .header("Content-Type", "application/json")
                .header("Referer", this.specifyProperties.rootUrl() + "/")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204){
                CookieStore cookieStore = cookieManager.getCookieStore();
                List<HttpCookie> cookies = cookieStore.getCookies();
                return cookies;
            } else if (response.statusCode() == 403){
                throw new RuntimeException("Forbidden. There has been a problem logging into the Collection. Most likely scenario is the CSRF Token being wrong.");
            }
            throw new RuntimeException("There has been an error.");
        } catch (IOException | InterruptedException e){
            throw new RuntimeException(e);
        }
    }

    public String getAgent(String csrfToken, String collectionId, String sessionId){
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
            if (response.statusCode() == 200){
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONObject agent = jsonResponse.getJSONObject("agent");
                return agent.getString("resource_uri");

            } else if (response.statusCode() == 403){
                throw new RuntimeException("Forbidden. Most likely scenario is a mistake with the CSRF Token.");
            } else {
                throw new RuntimeException("There was an error, response code was: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e){
            throw new RuntimeException("There was an error fetching data from the logged in user: ", e);
        }
    }

    public void logout(String csrfToken, String collectionId){
        HttpClient httpClient = HttpClient.newBuilder().build();

        String requestBody = String.format(
                "{\"username\": null,\"password\": null,\"collection\":%s}",
                collectionId
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/context/login/"))
                .header("X-CSRFToken", csrfToken)
                .header("Cookie", "csrftoken=" + csrfToken)
                .header("Content-Type", "application/json")
                .header("Referer", this.specifyProperties.rootUrl() + "/")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403){
                throw new RuntimeException("Forbidden. Most likely scenario is an error with the CSRF Token");
            }
        } catch (IOException | InterruptedException e){
            throw new RuntimeException();
        }
    }

    public JSONObject createCollectionObject(String csrfToken, String collectionId, String sessionId, JSONObject collectionObject){

        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collectionobject/"))
                .header("X-CSRFToken", csrfToken)
                .header("Cookie", "collection=" + collectionId + ";csrftoken=" + csrfToken + ";sessionid=" + sessionId)
                .header("Content-Type", "application/json")
                .header("Referer", this.specifyProperties.rootUrl() + "/specify/view/collectionobject/new/")
                .POST(HttpRequest.BodyPublishers.ofString(collectionObject.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201){
                return new JSONObject(response.body());
            } else if (response.statusCode() == 403){
                throw new RuntimeException("Forbidden. Most likely scenario is that something is wrong with the CSRF Cookie.");
            } else {
                throw new RuntimeException("There was an error creating the Collection Object: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONArray getUploadParams(String csrfToken, String sessionId, String collectionId, List<String> filenames){

        HttpClient httpClient = HttpClient.newBuilder().build();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("filenames", new JSONArray(filenames));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/attachment_gw/get_upload_params/"))
                .header("X-CSRFToken", csrfToken)
                .header("Cookie", "collection=" + collectionId + ";csrftoken=" + csrfToken + ";sessionid=" + sessionId)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200){
                JSONArray jsonResponse = new JSONArray(response.body());
                return jsonResponse;
            } else if (response.statusCode() == 403){
                throw new RuntimeException("Forbidden. Most likely scenario is a fail in the CSRF token.");
            } else {
                throw new RuntimeException("Something failed when getting the Upload Params");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject createCollectionObjectJSONObject(String catalogNumber, JSONArray collectionObjectAttachments,
                                                       String agent, String collectionResource, String collectionDiscipline){
        // CataloguedDate: Today (YYYY-MM-DD)
        // Discipline: We are using Entomology for this. Find out how to know.
        // Collection. NHMD Entomology. Hardcoded.
        // Preptype. Let's use "none" for now.
        // Yesno1 (whatever that is), true.

        JSONObject jsonObject = new JSONObject();
        String today = LocalDate.now().toString();

        jsonObject.put("altcatalognumber", JSONObject.NULL);
        jsonObject.put("catalogeddate", today);
        jsonObject.put("catalogeddateprecision", 1);
        jsonObject.put("cataloger", agent);
        jsonObject.put("catalognumber", catalogNumber);
        jsonObject.put("collection", collectionResource);
        jsonObject.put("guid", JSONObject.NULL);
        jsonObject.put("objectcondition", JSONObject.NULL);
        jsonObject.put("projectnumber", JSONObject.NULL);
        jsonObject.put("remarks", JSONObject.NULL);
        jsonObject.put("text2", JSONObject.NULL);
        jsonObject.put("text3", JSONObject.NULL);
        jsonObject.put("yesno1", true);
        jsonObject.put("_tableName", "CollectionObject");

        // Create the collectingEvent Object:
        JSONObject collectingEvent = new JSONObject();
        collectingEvent.put("collectingeventattachments", new JSONArray());
        collectingEvent.put("collectors", new JSONArray());
        collectingEvent.put("discipline", collectionDiscipline);
        collectingEvent.put("enddateprecision", 1);
        collectingEvent.put("method", JSONObject.NULL);
        collectingEvent.put("remarks", JSONObject.NULL);
        collectingEvent.put("startdateprecision", 1);
        collectingEvent.put("stationfieldnumber", JSONObject.NULL);
        collectingEvent.put("text2", JSONObject.NULL);
        collectingEvent.put("_tablename", "CollectingEvent");

        jsonObject.put("collectingevent", collectingEvent);

        jsonObject.put("collectionobjectattachments", collectionObjectAttachments);

        // Create the "preparations" array:
        JSONArray preparations = new JSONArray();
        JSONObject preparation = new JSONObject();
        preparation.put("preparationattachments", new JSONArray());
        preparation.put("prepareddateprecision", 1);
        preparation.put("preptype", "/api/specify/preptype/159/");
        preparation.put("remarks", JSONObject.NULL);
        preparation.put("samplenumber", JSONObject.NULL);
        preparation.put("text1", JSONObject.NULL);
        preparation.put("_tableName", "Preparation");

        preparations.put(preparation);

        jsonObject.put("preparations", preparations);

        return jsonObject;
    }

    public JSONObject getCollectionInfo(String csrfToken, String sessionId, String collectionId){
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collection/" + collectionId + "/"))
                .header("Cookie", "collection=" + collectionId + ";csrftoken=" + csrfToken + ";sessionid=" + sessionId)
                .header("X-CSRFToken", csrfToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200){
                // Saving the Collection Object Name for later use:
                return new JSONObject(response.body());
            } else {
                throw new RuntimeException("There was an error getting the Collection Name");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void createFormData(String attachmentToken, String attachmentLocation, String collectionName, InputStream inputStream, String filename){
        try (CloseableHttpClient httpClient = HttpClients.createDefault()){

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

                if (status == 200){
                    return classicHttpResponse.getEntity();
                } else {
                    throw new IOException("This happened: " + classicHttpResponse.getEntity().toString());
                }
            });
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public JSONObject createAttachmentResource(String attachmentLocation, String mimeType, String filename, int index){
        JSONObject attachment = new JSONObject();
        attachment.put("attachmentlocation", attachmentLocation);
        attachment.put("mimetype", mimeType);
        attachment.put("origfilename", filename);
        attachment.put("title", filename);
        attachment.put("ispublic", true);
        attachment.put("tableid", 111);

        JSONObject attachmentResource = new JSONObject();
        attachmentResource.put("ordinal", index);
        attachmentResource.put("attachment", attachment);
        attachmentResource.put("_tableName", "CollectionObjectAttachment");

        attachmentResource.put("attachment", attachment);

        return attachmentResource;
    }

    public JSONObject getCollectionObject(String csrfToken, String collectionId, String sessionId, String barcode){
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collectionobject/?catalognumber=" + barcode))
                .header("Cookie", "collection=" + collectionId + ";csrftoken=" + csrfToken + ";sessionid=" + sessionId)
                .header("X-CSRFToken", csrfToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200){
                JSONObject responseObject = new JSONObject(response.body());
                JSONArray objectsArray = responseObject.getJSONArray("objects");
                if (!objectsArray.isEmpty()){
                    return objectsArray.getJSONObject(0);
                } else {
                    throw new RuntimeException("The Specimen does not exist in Specify. Please create a Collection Object for this Specimen.");
                }
            } else {
                throw new RuntimeException("Error: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void putCollectionObject(String collectionId, String csrfToken, String sessionId, int collectionObjectId, JSONObject collectionObject){
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.specifyProperties.rootUrl() + "/api/specify/collectionobject/" + collectionObjectId + "/"))
                .header("Cookie", "collection=" + collectionId + ";csrftoken=" + csrfToken + ";sessionid=" + sessionId)
                .header("X-CSRFToken", csrfToken)
                .PUT(HttpRequest.BodyPublishers.ofString(collectionObject.toString()))
                .build();

        System.out.println(request);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("There was an error. Status: " + response.statusCode() + ". Error: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

// TODO:
// Ask about Collections. Do they already know the collection number? Should I pass it? For now, it's hardcoded.
// Should username and password for login into collection be passed as a POST body or should they be in the application.properties file?
// Preparations? I have hardcoded preparation: none for this, but how will it work later on?
// Methods for CREATING a new Collection Object from scratch exist in the code. I wouldn't delete them for now, maybe they will be useful later.