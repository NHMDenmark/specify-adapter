package dk.northtech.dassco_specify_adapter.domain.sync;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;

public record SpecifySyncLogEntry(Long specify_sync_log_id
        , Instant specify_modified_date
        , SpecifySyncStatus status
        , Integer specify_collection_object_attachment_id
        , String additional_info
        , Instant sync_attempt_update_timestamp
        , Integer specify_ars_sync_batch_id
        , String ars_asset_guid
        , SyncDirection sync_direction
) {

    @JdbiConstructor
    public SpecifySyncLogEntry(Long specify_sync_log_id, Instant specify_modified_date, SpecifySyncStatus status, Integer specify_collection_object_attachment_id, String additional_info, Instant sync_attempt_update_timestamp, Integer specify_ars_sync_batch_id, String ars_asset_guid, SyncDirection sync_direction) {
        this.specify_sync_log_id = specify_sync_log_id;
        this.specify_modified_date = specify_modified_date;
        this.status = status;
        this.specify_collection_object_attachment_id = specify_collection_object_attachment_id;
        this.additional_info = additional_info;
        this.sync_attempt_update_timestamp = sync_attempt_update_timestamp;
        this.specify_ars_sync_batch_id = specify_ars_sync_batch_id;
        this.ars_asset_guid = ars_asset_guid;
        this.sync_direction = sync_direction;
    }

    public SpecifySyncLogEntry(SpecifySyncLogEntry entry, Integer specify_ars_sync_batch_id, Integer specify_sync_log_id) {
        this(entry.specify_sync_log_id(), entry.specify_modified_date, entry.status, entry.specify_collection_object_attachment_id, entry.additional_info,entry.sync_attempt_update_timestamp, specify_ars_sync_batch_id, entry.ars_asset_guid, entry.sync_direction);
    }
}
