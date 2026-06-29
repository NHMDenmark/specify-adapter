package dk.northtech.dassco_specify_adapter.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.List;

public record InstitutionConfig(Long id,
                                String name,
                                String specifyRootUrl,
                                String specifyUsername,
                                List<CollectionConfig> collectionConfigs) {

    @JdbiConstructor
    public InstitutionConfig(Long id,
                             String name,
                             String specifyRootUrl,
                             String specifyUsername) {
        this(id, name, specifyRootUrl, specifyUsername, List.of());
    }

    public InstitutionConfig {
        collectionConfigs = collectionConfigs == null ? List.of() : List.copyOf(collectionConfigs);
    }
}
