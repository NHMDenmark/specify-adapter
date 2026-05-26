package dk.northtech.dassco_specify_adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("specify-web-asset-service")
public record SpecifyWebAssetServiceConfig(String allowStaticFileAccess, String fileFriendlyPostfix, String tokenKey, Integer tokenTimeToleranceSeconds, Boolean tokenRequiredForGet, String institution) {

}