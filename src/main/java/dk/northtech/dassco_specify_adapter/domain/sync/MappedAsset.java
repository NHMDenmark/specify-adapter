package dk.northtech.dassco_specify_adapter.domain.sync;

import dk.northtech.dassco_specify_adapter.domain.Asset;
import dk.northtech.dassco_specify_adapter.domain.Attachment;

import java.time.Instant;

public class MappedAsset {
    public Asset asset;
    public Attachment attachment;
    public String error;
    public Instant SpecifyModifiedDate;
    public Long specifyCollectionObjectAttachmentId;

}
