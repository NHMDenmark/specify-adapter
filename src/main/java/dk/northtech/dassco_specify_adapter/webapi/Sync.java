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
import dk.northtech.dassco_specify_adapter.services.SpecifyQueryService;
import dk.northtech.dassco_specify_adapter.services.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.EnumUtils;
import org.apache.tika.Tika;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

@Path("/sync")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Asset Files", description = "Endpoints related to assets' files.")
public class Sync {
    private final SpecifyQueryService specifyQueryService;
    private static final Logger LOGGER = LoggerFactory.getLogger(Sync.class);

    @Value("${asset-service.tokenRequiredForGet}")
    private boolean tokenRequiredForGet;

    private ServerProperties serverProperties;

    String hostname = "host.docker.internal";


    @Inject
    public Sync(SpecifyQueryService specifyQueryService) {
        this.specifyQueryService = specifyQueryService;
    }

    @GET
    @Path("/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getInternalStatusAmt(@PathParam("timeframe") String timeframe) {
        specifyQueryService.findCollectionObjectsToSync();
        return Response.ok().build();
    }

}
