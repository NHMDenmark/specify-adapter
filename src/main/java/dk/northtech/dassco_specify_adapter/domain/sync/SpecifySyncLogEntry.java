package dk.northtech.dassco_specify_adapter.domain.sync;

import java.time.Instant;

public record SpecifySyncLogEntry(Long specify_sync_log_id
        , Instant specify_modified_date
        , SpecifySyncStatus status
        , Integer specify_collection_object_attachment_id
        , String additional_inf
        , Instant sync_attempt_update_timestamp
        , Integer specify_ars_sync_batch_id
        , String ars_asset_guid
        , SyncDirection sync_direction
) {

}
