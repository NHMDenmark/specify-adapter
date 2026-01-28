package dk.northtech.dassco_specify_adapter.domain.specify;

import java.time.Instant;

public record CollectionObjectIdAndTimeStamp(int collectionObjectId, Instant modifiedTimestamp) {
}
