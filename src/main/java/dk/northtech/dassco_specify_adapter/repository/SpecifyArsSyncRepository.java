package dk.northtech.dassco_specify_adapter.repository;

import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyArsSyncBatch;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifySyncLogEntry;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface SpecifyArsSyncRepository extends SqlObject {
    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO specify_ars_sync_batch (batch_timestamp, specify_from_timestamp, specify_to_timestamp, status, additional_info) VALUES (:batch_timestamp,:specify_from_timestamp,:specify_to_timestamp,:status,:additional_info)")
    Integer createNewBatch(@BindMethods SpecifyArsSyncBatch batch);

    @SqlQuery("SELECT * FROM specify_ars_sync_batch WHERE status IN ('PARTIAL_SUCCESS', 'SUCCESSFUL', 'STARTED') ORDER BY specify_to_timestamp DESC LIMIT 1")
    SpecifyArsSyncBatch getLatestNonFailed();

    @GetGeneratedKeys
    @SqlUpdate("""
            INSERT INTO public.specify_sync_log(specify_modified_date
                        , status
                        , specify_collection_object_attachment_id
                        , additional_info
                        , sync_attempt_update_timestamp
                        , specify_ars_sync_batch_id
                        , ars_asset_guid
                        , sync_direction)
                        VALUES (:specify_modified_date
                                    , :status
                                    , :specify_collection_object_attachment_id
                                    , :additional_info
                                    , :sync_attempt_update_timestamp
                                    , :specify_ars_sync_batch_id
                                    , :ars_asset_guid
                                    , :sync_direction)
            
            """)
    Integer insertSyncLog(@BindMethods SpecifySyncLogEntry batch);

    @SqlQuery("SELECT * FROM specify_sync_log s WHERE s.specify_ars_sync_batch_id = :specify_ars_sync_batch_id")
    List<SpecifySyncLogEntry> getSyncLog(@Bind Integer specify_ars_sync_batch_id);


    @SqlUpdate("""
            UPDATE specify_sync_log SET specify_modified_date = :specify_modified_date
                                    , status = :status
                                    , specify_collection_object_attachment_id = :specify_collection_object_attachment_id
                                    , additional_info = :additional_info
                                    , sync_attempt_update_timestamp = :sync_attempt_update_timestamp
                                    , specify_ars_sync_batch_id = :specify_ars_sync_batch_id
                                    , ars_asset_guid = :ars_asset_guid
            
            """)
    void updateSyncLog(@BindMethods SpecifySyncLogEntry entry);
}
