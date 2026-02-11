package dk.northtech.dassco_specify_adapter.domain.sync;

import java.time.Instant;

public record SpecifyArsSyncBatch(Long specify_ars_sync_batch_id, Instant batch_timestamp, Instant specify_from_timestamp, Instant specify_to_timestamp, SpecifyArsSyncBatchStatus status, String additional_info) {
}
