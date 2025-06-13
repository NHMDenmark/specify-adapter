package dk.northtech.dassco_specify_adapter.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CollectionObjectAttachment {
    public Integer ordinal;
    public final String _tableName = "CollectionObjectAttachment";
    public Attachment attachment;
    public Long collectionmemberid;
    @JsonIgnore
    public String ars_collection;
    @JsonIgnore
    public String ars_institution;
    @JsonIgnore
    public String ars_barcode;
    @JsonIgnore
    public String ars_assetguid;

    @Override
    public String toString() {
        return "CollectionObjectAttachment{" +
               "ordinal=" + ordinal +
               ", _tableName='" + _tableName + '\'' +
               ", attachment=" + attachment +
               ", collectionmemberid=" + collectionmemberid +
               ", ars_collection='" + ars_collection + '\'' +
               ", ars_institution='" + ars_institution + '\'' +
               ", ars_barcode='" + ars_barcode + '\'' +
               ", ars_assetguid='" + ars_assetguid + '\'' +
               '}';
    }
}
