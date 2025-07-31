package dk.northtech.dassco_specify_adapter.configuration;

import dk.northtech.dassco_specify_adapter.webapi.AssetFiles;
import dk.northtech.dassco_specify_adapter.webapi.AssetServerApi;
import dk.northtech.dassco_specify_adapter.webapi.exceptionmappers.DasscoIllegalActionExceptionMapper;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("/")
public class JerseyApplicationConfig extends ResourceConfig {
    public JerseyApplicationConfig(){
        register(MultiPartFeature.class);
        register(RolesAllowedDynamicFeature.class);
        register(DasscoIllegalActionExceptionMapper.class);
        register(AssetFiles.class);
        register(AssetServerApi.class);
    }
}
