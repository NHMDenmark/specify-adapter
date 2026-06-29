package dk.northtech.dassco_specify_adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("specify-sync")
public record SpecifySyncProperties(int maxUpdatedAttachments,
                                    int attachmentPageSize,
                                    String specifyTimezone) {
}
