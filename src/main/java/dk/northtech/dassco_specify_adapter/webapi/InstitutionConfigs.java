package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.domain.CollectionConfig;
import dk.northtech.dassco_specify_adapter.domain.ConfigurationConflictException;
import dk.northtech.dassco_specify_adapter.domain.InstitutionConfig;
import dk.northtech.dassco_specify_adapter.domain.InstitutionConfigRequest;
import dk.northtech.dassco_specify_adapter.domain.SecurityRoles;
import dk.northtech.dassco_specify_adapter.services.CollectionConfigService;
import dk.northtech.dassco_specify_adapter.services.InstitutionConfigService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/institutions")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class InstitutionConfigs {
    private final InstitutionConfigService institutionConfigService;
    private final CollectionConfigService collectionConfigService;

    @Inject
    public InstitutionConfigs(InstitutionConfigService institutionConfigService, CollectionConfigService collectionConfigService) {
        this.institutionConfigService = institutionConfigService;
        this.collectionConfigService = collectionConfigService;
    }

    @GET
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public List<InstitutionConfig> listInstitutionConfigs() {
        return institutionConfigService.listInstitutionConfigs();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response getInstitutionConfig(@PathParam("id") Long id) {
        Optional<InstitutionConfig> institutionConfig = institutionConfigService.getInstitutionConfig(id);
        return institutionConfig.map(Response::ok)
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @POST
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response createInstitutionConfig(InstitutionConfigRequest institutionConfigRequest) {
        try {
            InstitutionConfig created = institutionConfigService.createInstitutionConfig(institutionConfigRequest);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
        } catch (ConfigurationConflictException exception) {
            return Response.status(Response.Status.CONFLICT).entity(exception.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response updateInstitutionConfig(@PathParam("id") Long id, InstitutionConfigRequest institutionConfigRequest) {
        try {
            Optional<InstitutionConfig> updated = institutionConfigService.updateInstitutionConfig(id, institutionConfigRequest);
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
    public Response deleteInstitutionConfig(@PathParam("id") Long id) {
        try {
            boolean deleted = institutionConfigService.deleteInstitutionConfig(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.noContent().build();
        } catch (ConfigurationConflictException exception) {
            return Response.status(Response.Status.CONFLICT).entity(exception.getMessage()).build();
        }
    }

    @GET
    @Path("/{institutionId}/collections")
    @RolesAllowed({SecurityRoles.DEVELOPER, SecurityRoles.ADMIN})
    public Response listCollectionConfigs(@PathParam("institutionId") Long institutionId) {
        if (institutionConfigService.getInstitutionConfig(institutionId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(collectionConfigService.listCollectionConfigs(institutionId)).build();
    }

}
