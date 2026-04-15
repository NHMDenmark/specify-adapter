package dk.northtech.dassco_specify_adapter.domain.sync;

import dk.northtech.dassco_specify_adapter.domain.Attachment;
import dk.northtech.dassco_specify_adapter.domain.specify.Agent;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObject;
import dk.northtech.dassco_specify_adapter.domain.specify.PrepType;

import java.util.List;

public class SpecifyAttachmentContext {
    public Attachment attachment;
    public Agent modifiedByAgent;
    public CollectionObject collectionObject;
    public List<PrepType> prepTypes;
    public Long collectionObjectAttachmentId;

    public SpecifyAttachmentContext(Attachment attachment, Agent createdByAgent, CollectionObject collectionObject, List<PrepType> prepTypes, Long collectionObjectAttachmentId) {
        this.attachment = attachment;
        this.modifiedByAgent = createdByAgent;
        this.collectionObject = collectionObject;
        this.prepTypes = prepTypes;
        this.collectionObjectAttachmentId = collectionObjectAttachmentId;
    }

    @Override
    public String toString() {
        return "SpecifyAttachmentContext{" +
                "attachment=" + attachment +
                ", modifiedByAgent=" + modifiedByAgent +
                ", collectionObject=" + collectionObject +
                ", prepTypes=" + prepTypes +
                ", collectionObjectAttachmentId=" + collectionObjectAttachmentId +
                '}';
    }
}
