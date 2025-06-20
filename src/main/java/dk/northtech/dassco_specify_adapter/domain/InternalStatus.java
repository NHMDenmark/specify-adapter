package dk.northtech.dassco_specify_adapter.domain;

public enum InternalStatus {
    METADATA_RECEIVED
    , ASSET_RECEIVED
    , COMPLETED
    , ERDA_FAILED
    //   , ERDA_ERROR
    , ERDA_SYNCHRONISED
    , SPECIFY_SYNC_SCHEDULED
    , SPECIFY_SYNC_FAILED
    , SPECIFY_SYNCHRONISED
}