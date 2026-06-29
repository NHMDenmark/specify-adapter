package dk.northtech.dassco_specify_adapter.repository;

import dk.northtech.dassco_specify_adapter.domain.InstitutionConfig;
import dk.northtech.dassco_specify_adapter.domain.InstitutionConfigCredentials;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface InstitutionConfigRepository extends SqlObject {
    @SqlQuery("""
            SELECT id,
                   name,
                   specify_root_url AS specifyRootUrl,
                   specify_asset_server_url AS specifyAssetServerUrl,
                   specify_username AS specifyUsername
            FROM institution_config
            ORDER BY name
            """)
    List<InstitutionConfig> listInstitutionConfigs();

    @SqlQuery("""
            SELECT id,
                   name,
                   specify_root_url AS specifyRootUrl,
                   specify_asset_server_url AS specifyAssetServerUrl,
                   specify_username AS specifyUsername
            FROM institution_config
            WHERE id = :id
            """)
    InstitutionConfig getInstitutionConfig(@Bind Long id);

    @SqlQuery("""
            SELECT id,
                   name,
                   specify_root_url AS specifyRootUrl,
                   specify_asset_server_url AS specifyAssetServerUrl,
                   specify_username AS specifyUsername
            FROM institution_config
            WHERE lower(name) = lower(:name)
            """)
    InstitutionConfig getInstitutionConfigByName(@Bind("name") String name);

    @SqlQuery("""
            SELECT id,
                   name,
                   specify_root_url AS specifyRootUrl,
                   specify_asset_server_url AS specifyAssetServerUrl,
                   specify_username AS specifyUsername,
                   specify_password_encrypted AS specifyPassword
            FROM institution_config
            WHERE id = :id
            """)
    InstitutionConfigCredentials getInstitutionConfigCredentials(@Bind Long id);

    @SqlQuery("""
            SELECT id,
                   name,
                   specify_root_url AS specifyRootUrl,
                   specify_asset_server_url AS specifyAssetServerUrl,
                   specify_username AS specifyUsername,
                   specify_password_encrypted AS specifyPassword
            FROM institution_config
            WHERE lower(name) = lower(:name)
            """)
    InstitutionConfigCredentials getInstitutionConfigCredentialsByName(@Bind("name") String name);

    @SqlQuery("SELECT COUNT(*) FROM collection_config WHERE institution_id = :institutionId")
    int countCollections(@Bind Long institutionId);

    @GetGeneratedKeys
    @SqlUpdate("""
            INSERT INTO institution_config(name, specify_root_url, specify_asset_server_url, specify_username, specify_password_encrypted)
            VALUES (:name, :specifyRootUrl, :specifyAssetServerUrl, :specifyUsername, :specifyPasswordEncrypted)
            """)
    Long createInstitutionConfig(@Bind("name") String name,
                                 @Bind("specifyRootUrl") String specifyRootUrl,
                                 @Bind("specifyAssetServerUrl") String specifyAssetServerUrl,
                                 @Bind("specifyUsername") String specifyUsername,
                                 @Bind("specifyPasswordEncrypted") String specifyPasswordEncrypted);

    @SqlUpdate("""
            UPDATE institution_config
            SET name = :name,
                specify_root_url = :specifyRootUrl,
                specify_asset_server_url = :specifyAssetServerUrl,
                specify_username = :specifyUsername,
                specify_password_encrypted = :specifyPasswordEncrypted
            WHERE id = :id
            """)
    int updateInstitutionConfig(@Bind("id") Long id,
                                @Bind("name") String name,
                                @Bind("specifyRootUrl") String specifyRootUrl,
                                @Bind("specifyAssetServerUrl") String specifyAssetServerUrl,
                                @Bind("specifyUsername") String specifyUsername,
                                @Bind("specifyPasswordEncrypted") String specifyPasswordEncrypted);

    @SqlUpdate("DELETE FROM institution_config WHERE id = :id")
    int deleteInstitutionConfig(@Bind Long id);
}
