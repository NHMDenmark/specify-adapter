package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.configuration.AssetServiceConfig;
import dk.northtech.dassco_specify_adapter.domain.specify.LoginInfo;
import dk.northtech.dassco_specify_adapter.services.AssetFileService;
import dk.northtech.dassco_specify_adapter.services.SpecifyEndpointService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.tika.Tika;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.InputStream;

import static jakarta.ws.rs.core.MediaType.*;

@Path("/")
public class AssetServerApi {
    private final AssetServiceConfig assetServiceConfig;
    private final SpecifyEndpointService specifyEndpointService;
    private final AssetFileService assetFileService;

    @Inject
    public AssetServerApi(AssetServiceConfig assetServiceConfig, SpecifyEndpointService specifyEndpointService, AssetFileService assetFileService) {
        this.assetServiceConfig = assetServiceConfig;
        this.specifyEndpointService = specifyEndpointService;
        this.assetFileService = assetFileService;
    }

    @GET
    @Path("")
    public Response itWorks(){
        return Response.status(200).entity("It works!").build();
    }

    @GET
    @Path("static/{path}")
    public Response getStaticFiles(@PathParam("path") String path){
        if(!Boolean.getBoolean(this.assetServiceConfig.allowStaticFileAccess())){
            return Response.status(404).build();
        }
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    @Produces("text/plain;charset=UTF-8")
    @Path("getfileref")
    public Response getFileRef(){
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    //@require_token('filename')
    //r.set_header('Content-Disposition', "inline; filename*=utf-8''%s" % download_name)
    @Path("fileget")
    @Consumes(MULTIPART_FORM_DATA)
    public Response getFile(@FormDataParam("coll") String coll, @FormDataParam("type") String type, @FormDataParam("filename") String filename, @FormDataParam("scale") Integer scale
    ){
        StreamingOutput streamingOutput = output -> {
            try (InputStream is = assetFileService.readFileFromParkedFiles(coll, type, filename, this.assetServiceConfig.fileFriendlyPostfix(), scale)) {
                is.transferTo(output);
                output.flush();
            }
        };
        String updatedFileName = type.equals("T") && filename.contains(".pdf") ? filename.replace(".pdf", ".png") : filename;
        return Response.status(200)
                .header("Content-Disposition", "inline; attachment; filename=*utf-8" + updatedFileName)
                .header("Content-Type", new Tika().detect(updatedFileName)).entity(streamingOutput).build();
    }

    @OPTIONS
    //@allow_cross_origin
    @Path("fileupload")
    public Response testFileUploadCors(){
        return Response.status(Response.Status.OK).entity("").build();
    }

    @POST
    //@allow_cross_origin-
    //@require_token('store')
    @Consumes(MULTIPART_FORM_DATA)
    @Produces("text/plain;charset=UTF-8")
    @Path("fileupload")
    public Response fileUpload(@FormDataParam("file") InputStream file, @FormDataParam("token") String token, @FormDataParam("store") String store, @FormDataParam("type") String type, @FormDataParam("coll") String coll){
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

        return status == 200 ? Response.status(200).entity("Ok.").build() : Response.status(status).build();
    }

    @POST
    @Consumes(MULTIPART_FORM_DATA)
    @Produces("text/plain;charset=UTF-8")
    @Path("filedelete")
    public Response deleteFile(){
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    //@require_token('filename')
    @Produces(APPLICATION_JSON)
    @Path("getmetadata")
    public Response getMetadata(@QueryParam("filename") String filename, @QueryParam("dt") String dt){
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    //@require_token('random', always=True)
    @Produces("text/plain;charset=UTF-8")
    @Path("testkey")
    public Response testAccessKey(){
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @GET
    //@include_timestamp
    @Produces("text/xml;charset=UTF-8")
    @Path("web_asset_store.xml")
    public Response serveXmlDescriptionOfUrlsAvailable(){
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
