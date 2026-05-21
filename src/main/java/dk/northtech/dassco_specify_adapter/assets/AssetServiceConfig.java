package dk.northtech.dassco_specify_adapter.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("asset-service")
public record AssetServiceConfig(String rootUrl) {
}
