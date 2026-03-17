package dk.northtech.dassco_specify_adapter.domain.sync;

import dk.northtech.dassco_specify_adapter.domain.Attachment;
import dk.northtech.dassco_specify_adapter.domain.specify.Agent;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObject;
import dk.northtech.dassco_specify_adapter.domain.specify.PrepType;

public class SpecifyAttachmentContext {
    public Attachment attachment;
    public Agent modifiedByAgent;
    public CollectionObject collectionObject;
    public PrepType prepType;
    public Long collectionObjectAttachmentId;

    public SpecifyAttachmentContext(Attachment attachment, Agent modifiedByAgent, CollectionObject collectionObject, PrepType prepType, Long collectionObjectAttachmentId) {
        this.attachment = attachment;
        this.modifiedByAgent = modifiedByAgent;
        this.collectionObject = collectionObject;
        this.prepType = prepType;
        this.collectionObjectAttachmentId = collectionObjectAttachmentId;
    }

    @Override
    public String toString() {
        return "SpecifyAttachmentContext{" +
                "attachment=" + attachment +
                ", modifiedByAgent=" + modifiedByAgent +
                ", collectionObject=" + collectionObject +
                ", prepType=" + prepType +
                ", collectionObjectAttachmentId=" + collectionObjectAttachmentId +
                '}';
    }
}
