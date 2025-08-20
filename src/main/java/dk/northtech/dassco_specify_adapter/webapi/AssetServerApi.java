package dk.northtech.dassco_specify_adapter.webapi;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import dk.northtech.dassco_specify_adapter.configuration.AssetServiceConfig;
import dk.northtech.dassco_specify_adapter.domain.specify.LoginInfo;
import dk.northtech.dassco_specify_adapter.services.AssetFileService;
import dk.northtech.dassco_specify_adapter.services.SpecifyEndpointService;
import dk.northtech.dassco_specify_adapter.services.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.tika.Tika;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static jakarta.ws.rs.core.MediaType.*;

@Path("/")
public class AssetServerApi {
    private final AssetServiceConfig assetServiceConfig;
    private final SpecifyEndpointService specifyEndpointService;
    private final AssetFileService assetFileService;
    private final TokenService tokenService;

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetServerApi.class);

    @Value("${asset-service.tokenRequiredForGet}")
    private boolean tokenRequiredForGet;

    private ServerProperties serverProperties;

    String hostname = "host.docker.internal";


    @Inject
    public AssetServerApi(AssetServiceConfig assetServiceConfig, SpecifyEndpointService specifyEndpointService, AssetFileService assetFileService, TokenService tokenService, ServerProperties serverProperties) {
        this.assetServiceConfig = assetServiceConfig;
        this.specifyEndpointService = specifyEndpointService;
        this.assetFileService = assetFileService;
        this.tokenService = tokenService;
        this.serverProperties = serverProperties;
    }

    @GET
    @Path("")
    public Response itWorks(){
        LOGGER.info("ItWorks");
        return Response.status(200).entity("It works!").build();
    }

    @GET
    @Path("static/{path: .+}")
    public Response getStaticFiles(@PathParam("path") String path){
        LOGGER.info("static/{path}");
        if(!Boolean.parseBoolean(this.assetServiceConfig.allowStaticFileAccess())){
            return Response.status(404).build();
        }

        String[] pathParts = path.split("/");
        if(pathParts.length < 4){
            return Response.status(404).build();
        }

        String fileFriendlyPostfix = pathParts[0];
        String coll = pathParts[1];
        String type = pathParts[2];
        String filename = pathParts[3];
        HttpResponse<InputStream> response = assetFileService.readFileFromParkedFiles(URLDecoder.decode(coll, StandardCharsets.UTF_8), type, filename, URLDecoder.decode(fileFriendlyPostfix, StandardCharsets.UTF_8), null);
        if(response.statusCode() != 200){
            return Response.status(response.statusCode()).entity(response.body()).build();
        }
        StreamingOutput streamingOutput = output -> {
            try (InputStream is = response.body()) {
                is.transferTo(output);
                output.flush();
            }
        };
        String updatedFileName = filename;
        if(type.equals("T") && updatedFileName.contains(".pdf")){
            updatedFileName = updatedFileName.replace(".pdf", ".png");
        } else if(type.equals("T") && updatedFileName.contains(".tif")){
            updatedFileName = updatedFileName.replace(".tif", ".png");
        }
        if(filename != null){
            String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            return Response.status(response.statusCode())
                    .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
//                    .header("Content-Disposition", "inline; filename=*utf-8" + encodedName)
                    .header("Content-Type", new Tika().detect(updatedFileName))
                    .entity(streamingOutput).build();
        }
        return Response.status(200)
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .header("Content-Disposition", "inline; attachment; filename=*utf-8" + updatedFileName)
                .header("Content-Type", new Tika().detect(updatedFileName))
                .entity(streamingOutput).build();
    }

    @GET
    @Produces("text/plain;charset=UTF-8")
    @Path("getfileref")
    public Response getFileRef(@QueryParam("coll") String coll, @QueryParam("type") String type, @QueryParam("filename") String filename, @QueryParam("scale") Integer scale){
        LOGGER.info("getfileref");
        String path = this.assetFileService.pathToUrlPath(type, coll, filename, this.assetServiceConfig.fileFriendlyPostfix(), scale);
        var response = this.assetFileService.readFilePathFromParkedFiles(coll, type, filename, this.assetServiceConfig.fileFriendlyPostfix(), scale);
        if(response.statusCode() == 200){
            return Response.status(200).entity(this.hostname + ":" + this.serverProperties.getPort() + "/static/" + path).build();
        }
        return Response.status(response.statusCode()).entity(response.body()).build();
    }

    @GET
    @Path("fileget")
    @Consumes(MULTIPART_FORM_DATA)
    public Response getFile(@QueryParam("token") String token, @QueryParam("coll") String coll, @QueryParam("type") String type, @QueryParam("filename") String filename, @QueryParam("scale") Integer scale, @QueryParam("downloadname") String downloadName){
        LOGGER.info("fileget");
        if(this.tokenRequiredForGet) {
            this.tokenService.validateToken(token, filename);
        }

        HttpResponse<InputStream> response = assetFileService.readFileFromParkedFiles(coll, type, filename, this.assetServiceConfig.fileFriendlyPostfix(), scale);
        if(response.statusCode() != 200){
            return Response.status(response.statusCode()).entity(response.body()).build();
        }
        StreamingOutput streamingOutput = output -> {
            try (InputStream is = response.body()) {
                is.transferTo(output);
                output.flush();
            }
        };
        String updatedFileName = filename;
        if(type.equals("T") && updatedFileName.contains(".pdf")){
            updatedFileName = updatedFileName.replace(".pdf", ".png");
        } else if(type.equals("T") && updatedFileName.contains(".tif")){
            updatedFileName = updatedFileName.replace(".tif", ".png");
        }
        if(downloadName != null){
            String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
            return Response.status(response.statusCode())
                    .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                    .header("Content-Disposition", "inline; filename=*utf-8" + encodedName)
                    .header("Content-Type", new Tika().detect(updatedFileName))
                    .entity(streamingOutput).build();
        }
        return Response.status(200)
                .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
                .header("Content-Disposition", "inline; attachment; filename=*utf-8" + updatedFileName)
                .header("Content-Type", new Tika().detect(updatedFileName))
                .entity(streamingOutput).build();
    }

    @OPTIONS
    @Path("fileupload")
    public Response testFileUploadCors(){
        LOGGER.info("fileupload::OPTIONS");
        return Response.status(Response.Status.OK).entity("").build();
    }

    @POST
    @Consumes(MULTIPART_FORM_DATA)
    @CrossOrigin(originPatterns = "*")
    @Produces("text/plain;charset=UTF-8")
    @Path("fileupload")
    public Response fileUpload(@FormDataParam("file") InputStream file, @FormDataParam("token") String token, @FormDataParam("store") String store, @FormDataParam("type") String type, @FormDataParam("coll") String coll){
        LOGGER.info("fileupload::POST");
        this.tokenService.validateToken(token, store);
        if(file == null){
            return Response.status(Response.Status.BAD_REQUEST).entity("No file received").build();
        }
        if(type.equals("T")){
            return Response.status(Response.Status.OK).entity("Ignoring thumbnail upload!").build();
        }
        LoginInfo loginInfo = this.specifyEndpointService.login();
        if(!loginInfo.collections.containsKey(coll) && !loginInfo.collections.isEmpty()){
            return Response.status(Response.Status.NOT_FOUND).entity(String.format("Unknown collection: %s", coll)).build();
        }

        int status = this.assetFileService.postFileToParkedFiles(file, "originals", coll, store, this.assetServiceConfig.fileFriendlyPostfix());

        return status == 200 ? Response.status(200).entity("Ok.").header("X-Timestamp", String.valueOf(System.currentTimeMillis())).build() : Response.status(status).header("X-Timestamp", String.valueOf(System.currentTimeMillis())).build();
    }

    @POST
    @Produces("text/plain;charset=UTF-8")
    @Path("filedelete")
    public Response deleteFile(@FormParam("coll") String coll, @FormParam("filename") String filename){
        LOGGER.info("filedelete");
        int status = assetFileService.deleteFileFromParkedFiles(coll, filename, this.assetServiceConfig.fileFriendlyPostfix());
        return Response.status(status).entity(status == 200 ? "Ok." : "").build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("getmetadata")
    public Response getMetadata(@QueryParam("token") String token, @QueryParam("filename") String filename, @QueryParam("coll") String coll, @QueryParam("dt") String dt){
        LOGGER.info("getmetadata");
        if(this.tokenRequiredForGet) {
            this.tokenService.validateToken(token, filename);
        }
        HttpResponse<InputStream> response = assetFileService.readFileFromParkedFiles(coll, "O", filename, this.assetServiceConfig.fileFriendlyPostfix(), null);
        if(response.statusCode() != 200){
            return Response.status(response.statusCode()).header("X-Timestamp", String.valueOf(System.currentTimeMillis())).entity(response.body()).build();
        }

        try (InputStream is = new BufferedInputStream(response.body())) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);
            Map<String, String> metaMap = new LinkedHashMap<>();

            for (Directory directory : metadata.getDirectories()) {
                String dirPrefix = assetFileService.normalizeMetadataDirectoryName(directory.getName());
                for (Tag tag : directory.getTags()) {
                    String tagName = assetFileService.normalizeMetadataTagName(tag.getTagName());
                    String key = dirPrefix + " " + tagName;
                    metaMap.put(key, tag.getDescription());
                }
            }

            if(Objects.equals(dt, "date")){
                String dateTimeOriginal = metaMap.get("EXIF DateTimeOriginal");
                if(dateTimeOriginal != null){
                    return Response.status(Response.Status.OK).header("X-Timestamp", String.valueOf(System.currentTimeMillis())).entity(dateTimeOriginal).build();
                }else{
                    return Response.status(Response.Status.NOT_FOUND).header("X-Timestamp", String.valueOf(System.currentTimeMillis())).entity("DateTime not found in EXIF").build();
                }
            }

            JSONArray jsonArray = new JSONArray();
            for (Map.Entry<String, String> entry : metaMap.entrySet()) {
                JSONObject obj = new JSONObject();
                obj.put("Name", entry.getKey());
                obj.put("Fields", entry.getValue());
                jsonArray.put(obj);
            }

            return Response.status(Response.Status.OK).header("X-Timestamp", String.valueOf(System.currentTimeMillis())).entity(jsonArray.toString()).build();


        } catch (IOException | ImageProcessingException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Response.Status.OK).header("X-Timestamp", String.valueOf(System.currentTimeMillis())).build();

        }
    }

    @GET
    @Produces("text/plain;charset=UTF-8")
    @Path("testkey")
    public Response testTokenWorks(@QueryParam("token") String token, @QueryParam("random") String random){
        LOGGER.info("testkey");
        //overrides -> tokenRequiredForGet
        this.tokenService.validateToken(token, random);
        return Response.status(Response.Status.OK).entity("Ok.").build();
    }

    @GET
    @Produces("text/xml;charset=UTF-8")
    @Path("web_asset_store.xml")
    public Response serveXmlDescriptionOfUrlsAvailable(){
        LOGGER.info("web_asset_store.xml");
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <urls>
                    <url type="read"><![CDATA[http://{{host}}:{{serverPort}}/fileget]]></url>
                    <url type="write"><![CDATA[http://{{host}}:{{serverPort}}/fileupload]]></url>
                    <url type="delete"><![CDATA[http://{{host}}:{{serverPort}}/filedelete]]></url>
                    <url type="getmetadata"><![CDATA[http://{{host}}:{{serverPort}}/getmetadata]]></url>
                    <url type="testkey">http://{{host}}:{{serverPort}}/testkey</url>
                </urls>
                """.replace("{{host}}", hostname).replace("{{serverPort}}", this.serverProperties.getPort().toString());
        return Response.status(Response.Status.OK).entity(xml).header("X-Timestamp", String.valueOf(System.currentTimeMillis())).build();
}
}
