package dk.northtech.dassco_specify_adapter.webapi.exceptionmappers;

import dk.northtech.dassco_specify_adapter.domain.ErrorResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.time.Instant;

public class CustomExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception){
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(Instant.now().toString());
        errorResponse.setStatus(Response.Status.FORBIDDEN.getStatusCode());
        errorResponse.setError("Forbidden");
        errorResponse.setMessage(exception.getMessage());

        return Response.status(Response.Status.FORBIDDEN)
                .entity(errorResponse)
                .build();
    }
}
