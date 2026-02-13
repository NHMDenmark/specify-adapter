package dk.northtech.dassco_specify_adapter.domain.sync;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;
import java.util.List;

public record SpecifyArsSyncBatch(Integer specify_ars_sync_batch_id, Instant batch_timestamp,
                                  Instant specify_from_timestamp, Instant specify_to_timestamp,
                                  SpecifyArsSyncBatchStatus status, String additional_info,
                                  List<SpecifySyncLogEntry> entries) {

    @JdbiConstructor
    public SpecifyArsSyncBatch(Integer specify_ars_sync_batch_id
            , Instant batch_timestamp
            , Instant specify_from_timestamp
            , Instant specify_to_timestamp
            , SpecifyArsSyncBatchStatus status
            , String additional_info) {
        this(specify_ars_sync_batch_id, batch_timestamp, specify_from_timestamp, specify_to_timestamp, status, additional_info, List.of());
    }
}
