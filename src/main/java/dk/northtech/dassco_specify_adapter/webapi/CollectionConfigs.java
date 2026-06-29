package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.domain.CollectionConfig;
import dk.northtech.dassco_specify_adapter.domain.ConfigurationConflictException;
import dk.northtech.dassco_specify_adapter.domain.SecurityRoles;
import dk.northtech.dassco_specify_adapter.services.CollectionConfigService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/collections")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class CollectionConfigs {
    private final CollectionConfigService collectionConfigService;

    @Inject
    public CollectionConfigs(CollectionConfigService collectionConfigService) {
        this.collectionConfigService = collectionConfigService;
    }

    @POST
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response createCollectionConfig(CollectionConfig collectionConfig) {
        try {
            if (collectionConfig.institutionId() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("institutionId is required").build();
            }
            Optional<CollectionConfig> created = collectionConfigService.createCollectionConfig(collectionConfig.institutionId(), collectionConfig);
            if (created.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.status(Response.Status.CREATED).entity(created.orElseThrow()).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
        } catch (ConfigurationConflictException exception) {
            return Response.status(Response.Status.CONFLICT).entity(exception.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response getCollectionConfig(@PathParam("id") Long id) {
        Optional<CollectionConfig> collectionConfig = collectionConfigService.getCollectionConfig(id);
        return collectionConfig.map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response updateCollectionConfig(@PathParam("id") Long id, CollectionConfig collectionConfig) {
        try {
            Optional<CollectionConfig> updated = collectionConfigService.updateCollectionConfig(id, collectionConfig);
            return updated.map(Response::ok)
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                    .build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
        } catch (ConfigurationConflictException exception) {
            return Response.status(Response.Status.CONFLICT).entity(exception.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response deleteCollectionConfig(@PathParam("id") Long id) {
        boolean deleted = collectionConfigService.deleteCollectionConfig(id);
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }
}
