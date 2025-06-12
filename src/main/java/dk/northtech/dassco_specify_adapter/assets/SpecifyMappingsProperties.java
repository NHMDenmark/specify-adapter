package dk.northtech.dassco_specify_adapter.assets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("specify-mappings")
public record SpecifyMappingsProperties(String location) {
}
