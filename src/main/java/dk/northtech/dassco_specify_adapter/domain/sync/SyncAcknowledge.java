package dk.northtech.dassco_specify_adapter.domain.sync;

public record SyncAcknowledge(SpecifySyncStatus specifySyncStatus, Long specifySyncLogId, String additionalInfo, String assetGuid)  {

}
