package dk.northtech.dassco_specify_adapter.webapi;


import dk.northtech.dassco_specify_adapter.domain.SecurityRoles;
import dk.northtech.dassco_specify_adapter.domain.User;
import dk.northtech.dassco_specify_adapter.services.FileService;
import dk.northtech.dassco_specify_adapter.services.LogService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;

import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/logs")
public class Logs {
    private final LogService logService;

    @Inject
    public Logs(LogService logService) {
        this.logService = logService;
    }


    @GET
    @Path("/")
//    @Operation(summary = "List logfiles", description = "List all logfiles in the log directory")
    @Produces(APPLICATION_JSON)
//    @ApiResponse(responseCode = "200", description = "Returns the file.")
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public List<String> listLogs(
            @Context SecurityContext securityContext
    ) {
        return logService.listLogs();
    }

    @GET
    @Path("/{fileName}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response getFile(
            @Context SecurityContext securityContext
            , @PathParam("fileName") String name
    ) {
        User user = UserMapper.from(securityContext);

        Optional<FileService.FileResult> getFileResult = logService.getFile(name);
        if (getFileResult.isPresent()) {
            FileService.FileResult fileResult = getFileResult.get();
            StreamingOutput streamingOutput = output -> {
                fileResult.is().transferTo(output);
                output.flush();
            };
            return Response.status(200)
                    .header("Content-Disposition", "attachment; filename=" + fileResult.filename())
                    .header("Content-Type", "text/plain").entity(streamingOutput).build();
        }
        return Response.status(404).build();
    }

}
