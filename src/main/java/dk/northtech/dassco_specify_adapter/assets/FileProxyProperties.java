package dk.northtech.dassco_specify_adapter.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("file-proxy")
public record FileProxyProperties(String rootUrl) {
}
