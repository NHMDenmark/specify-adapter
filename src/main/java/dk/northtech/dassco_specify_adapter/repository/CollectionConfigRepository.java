package dk.northtech.dassco_specify_adapter.repository;

import dk.northtech.dassco_specify_adapter.domain.CollectionConfig;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface CollectionConfigRepository extends SqlObject {
    @SqlQuery("""
            SELECT id,
                   institution_id AS institutionId,
                   name,
                   description,
                   sync_to_specify_enabled AS syncToSpecifyEnabled,
                   sync_from_specify_enabled AS syncFromSpecifyEnabled
            FROM collection_config
            WHERE institution_id = :institutionId
            ORDER BY name
            """)
    List<CollectionConfig> listCollectionConfigsByInstitutionId(@Bind Long institutionId);

    @SqlQuery("""
            SELECT id,
                   institution_id AS institutionId,
                   name,
                   description,
                   sync_to_specify_enabled AS syncToSpecifyEnabled,
                   sync_from_specify_enabled AS syncFromSpecifyEnabled
            FROM collection_config
            WHERE id = :id
            """)
    CollectionConfig getCollectionConfig(@Bind Long id);

    @GetGeneratedKeys
    @SqlUpdate("""
            INSERT INTO collection_config(institution_id, name, description, sync_to_specify_enabled, sync_from_specify_enabled)
            VALUES (:institutionId, :name, :description, :syncToSpecifyEnabled, :syncFromSpecifyEnabled)
            """)
    Long createCollectionConfig(@BindMethods CollectionConfig collectionConfig);

    @SqlUpdate("""
            UPDATE collection_config
            SET institution_id = :institutionId,
                name = :name,
                description = :description,
                sync_to_specify_enabled = :syncToSpecifyEnabled,
                sync_from_specify_enabled = :syncFromSpecifyEnabled
            WHERE id = :id
            """)
    int updateCollectionConfig(@BindMethods CollectionConfig collectionConfig);

    @SqlUpdate("DELETE FROM collection_config WHERE id = :id")
    int deleteCollectionConfig(@Bind Long id);
}
