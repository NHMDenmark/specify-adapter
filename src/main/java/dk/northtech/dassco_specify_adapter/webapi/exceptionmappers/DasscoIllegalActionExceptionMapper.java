package dk.northtech.dassco_specify_adapter.webapi.exceptionmappers;

import dk.northtech.dassco_specify_adapter.domain.DasscoIllegalActionException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class DasscoIllegalActionExceptionMapper implements ExceptionMapper<DasscoIllegalActionException> {
    @Override
    public Response toResponse(DasscoIllegalActionException e) {
        return Response.status(403).entity(new DaSSCoError("1.0", DaSSCoErrorCode.FORBIDDEN, e.getMessage(), e.body())).build();
    }
}
