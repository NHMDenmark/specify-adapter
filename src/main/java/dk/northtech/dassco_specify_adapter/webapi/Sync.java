package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.domain.SecurityRoles;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyArsSyncBatch;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifySyncLogEntry;
import dk.northtech.dassco_specify_adapter.services.SpecifySyncService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Path("/sync")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Asset Files", description = "Endpoints related to assets' files.")
public class Sync {
    private final SpecifySyncService specifySyncService;
    private static final Logger LOGGER = LoggerFactory.getLogger(Sync.class);

    @Inject
    public Sync(SpecifySyncService specifySyncService) {
        this.specifySyncService = specifySyncService;
    }

    @POST
    @Path("/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getInternalStatusAmt(@PathParam("timeframe") String timeframe) {
//        specifyQueryService.findCollectionObjectsToSync();
        specifySyncService.specifyToArsSync();
        return Response.ok().build();
    }

    @GET
    @Path("/batches")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public List<SpecifyArsSyncBatch> listSyncBatches(@QueryParam("offset") Integer offset,
                                                     @QueryParam("limit") Integer limit) {
        return specifySyncService.listSyncBatches(offset, limit);
    }

    @GET
    @Path("/batches/{batchId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public List<SpecifySyncLogEntry> listBatchEntries(@PathParam("batchId") Integer batchId,
                                                      @QueryParam("offset") Integer offset,
                                                      @QueryParam("limit") Integer limit) {
        return specifySyncService.listSyncBatchEntries(batchId, offset, limit);
    }

}
