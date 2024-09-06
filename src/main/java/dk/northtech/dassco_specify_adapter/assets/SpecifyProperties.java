package dk.northtech.dassco_specify_adapter.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("specify")
public record SpecifyProperties (String rootUrl, String username, String password, String assetServer) {
}
