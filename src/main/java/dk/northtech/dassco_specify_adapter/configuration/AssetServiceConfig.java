package dk.northtech.dassco_specify_adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asset-service")
public record AssetServiceConfig(String rootUrl) {

}