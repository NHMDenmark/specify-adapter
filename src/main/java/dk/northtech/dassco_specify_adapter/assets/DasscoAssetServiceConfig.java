package dk.northtech.dassco_specify_adapter.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dassco-asset-service")
public record DasscoAssetServiceConfig(String rootUrl) {
}
