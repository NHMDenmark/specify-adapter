package dk.northtech.dassco_specify_adapter.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.List;

public record InstitutionConfigCredentials(Long id,
                                           String name,
                                           String specifyRootUrl,

                                           String specifyUsername,
                                           String specifyPassword,
                                           List<CollectionConfig> collectionConfigs) {

    @JdbiConstructor
    public InstitutionConfigCredentials(Long id,
                                        String name,
                                        String specifyRootUrl,

                                        String specifyUsername,
                                        String specifyPassword) {
        this(id, name, specifyRootUrl,  specifyUsername, specifyPassword, List.of());
    }

    public InstitutionConfigCredentials {
        collectionConfigs = collectionConfigs == null ? List.of() : List.copyOf(collectionConfigs);
    }
}
