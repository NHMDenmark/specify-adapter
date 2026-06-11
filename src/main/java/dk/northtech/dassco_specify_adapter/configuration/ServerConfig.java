package dk.northtech.dassco_specify_adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "specify-bridge")
public record ServerConfig(
        String rootUrl
) {

}
