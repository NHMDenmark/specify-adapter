package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.domain.SecurityRoles;
import dk.northtech.dassco_specify_adapter.domain.User;
import dk.northtech.dassco_specify_adapter.services.SpecifyEndpointService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/specify")
public class SpecifyEndpoints {

    private final SpecifyEndpointService specifyEndpointService;

    @Inject
    public SpecifyEndpoints(SpecifyEndpointService specifyEndpointService){
        this.specifyEndpointService = specifyEndpointService;
    }

    @POST
    @Path("/push/{assetGuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE, SecurityRoles.USER})
    public Response pushImageToSpecify(@PathParam("assetGuid") String assetGuid, @Context SecurityContext securityContext){
        User user = UserMapper.from(securityContext);
        return this.specifyEndpointService.pushImageToSpecify(assetGuid, user);
    }
}
