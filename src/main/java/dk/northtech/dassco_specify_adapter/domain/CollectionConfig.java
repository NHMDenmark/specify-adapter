package dk.northtech.dassco_specify_adapter.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

public record CollectionConfig(Long id,
                               Long institutionId,
                               String name,
                               String description,
                               Boolean syncToSpecifyEnabled,
                               Boolean syncFromSpecifyEnabled) {

    @JdbiConstructor
    public CollectionConfig {
    }
}
