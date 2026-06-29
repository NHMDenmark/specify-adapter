package dk.northtech.dassco_specify_adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("institution-config-crypto")
public record InstitutionConfigCryptoProperties(String secretKey) {
}
