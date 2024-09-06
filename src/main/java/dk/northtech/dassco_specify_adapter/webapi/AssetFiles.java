package dk.northtech.dassco_specify_adapter.webapi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("assetfiles")
public class AssetFiles {
    @GET
    @Path("/{assetGuid}")
    public Response getFiles(@PathParam("assetGuid") String assetGuid){
        return Response.status(200).entity(assetGuid).build();
    }
}
