package dk.northtech.dassco_specify_adapter.repository;

import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyArsSyncBatch;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyArsSyncBatchStatus;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifySyncLogEntry;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.List;

public interface SpecifyArsSyncRepository extends SqlObject {
    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO specify_ars_sync_batch (batch_timestamp, specify_from_timestamp, specify_to_timestamp, status, additional_info) VALUES (:batch_timestamp,:specify_from_timestamp,:specify_to_timestamp,:status,:additional_info)")
    Integer createNewBatch(@BindMethods SpecifyArsSyncBatch batch);

    @SqlQuery("SELECT * FROM specify_ars_sync_batch WHERE status IN ('FAILED_ENTRIES', 'SUCCEEDED', 'STARTED') ORDER BY specify_to_timestamp DESC LIMIT 1")
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
    Long insertSyncLog(@BindMethods SpecifySyncLogEntry batch);

    @SqlQuery("""
    SELECT * FROM specify_sync_log ssl
    WHERE ssl.specify_sync_log_id = :specicy_sync_log_id
    	OR ssl.specify_ars_sync_batch_id IS NOT NULL
    	AND ssl.specify_ars_sync_batch_id =
    	(SELECT ssl2.specify_ars_sync_batch_id
    		FROM specify_sync_log ssl2
    		WHERE ssl2.specify_sync_log_id = :specicy_sync_log_id)
""")
    List<SpecifySyncLogEntry> getSyncLogEntriesBySyncLogId(@Bind Long specicy_sync_log_id);

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
            WHERE specify_sync_log_id = :specify_sync_log_id                        
            
            """)
    void updateSyncLog(@BindMethods SpecifySyncLogEntry entry);

    @SqlUpdate("""
        UPDATE specify_ars_sync_batch SET status = :status, additional_info = :additional_info
        WHERE specify_ars_sync_batch_id = :specify_ars_sync_batch_id
""")
    void updateSpecifyArsSyncBatch(@Bind Integer  specify_ars_sync_batch_id, SpecifyArsSyncBatchStatus status, String  additional_info);

    @SqlQuery("""
            SELECT EXISTS(
                SELECT 1
                FROM specify_sync_log ssl
                WHERE ssl.specify_collection_object_attachment_id = :specify_collection_object_attachment_id
                  AND ssl.sync_direction = 'ARS_TO_SPECIFY'
                  AND ssl.status = 'SUCCEEDED'
                  AND ssl.specify_modified_date BETWEEN :from_timestamp AND :to_timestamp
            )
            """)
    boolean hasArsToSpecifySyncNearTimestamp(@Bind Long specify_collection_object_attachment_id,
                                             @Bind("from_timestamp") Instant fromTimestamp,
                                             @Bind("to_timestamp") Instant toTimestamp);
}
