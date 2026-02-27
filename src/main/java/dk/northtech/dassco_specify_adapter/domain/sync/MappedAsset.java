package dk.northtech.dassco_specify_adapter.domain.sync;

import dk.northtech.dassco_specify_adapter.domain.Asset;
import dk.northtech.dassco_specify_adapter.domain.Attachment;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class MappedAsset {
    public Asset asset;
    public Attachment attachment;
    public String error;
    public Instant SpecifyModifiedDate;
    public Long specifyCollectionObjectAttachmentId;
    public Set<String> updatedFields = new HashSet<>();

}
