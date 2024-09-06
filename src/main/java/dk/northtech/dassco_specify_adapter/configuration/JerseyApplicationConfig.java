package dk.northtech.dassco_specify_adapter.configuration;

import dk.northtech.dassco_specify_adapter.webapi.AssetFiles;
import dk.northtech.dassco_specify_adapter.webapi.Assets;
import dk.northtech.dassco_specify_adapter.webapi.SpecifyEndpoints;
import dk.northtech.dassco_specify_adapter.webapi.exceptionmappers.DasscoIllegalActionExceptionMapper;
import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("/")
public class JerseyApplicationConfig extends ResourceConfig {
    public JerseyApplicationConfig(){
        register(RolesAllowedDynamicFeature.class);
        register(DasscoIllegalActionExceptionMapper.class);
        register(SpecifyEndpoints.class);
        register(Assets.class);
        register(AssetFiles.class);
    }
}
