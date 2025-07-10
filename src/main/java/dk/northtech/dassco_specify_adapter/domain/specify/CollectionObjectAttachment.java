package dk.northtech.dassco_specify_adapter.domain.specify;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dk.northtech.dassco_specify_adapter.domain.Attachment;

public class CollectionObjectAttachment {
    public Integer ordinal;
    public final String _tableName = "CollectionObjectAttachment";
    public Attachment attachment;
    public Integer collectionmemberid;
    public String collectionobject;
    public Long id;
    // We can create objects in specify without version, but any attempts at updating them afterward will result in errors.
    public Integer version;
//    @JsonIgnore
//    public String ars_collection;
//    @JsonIgnore
//    public String ars_institution;
//    @JsonIgnore
//    public String ars_barcode;
//    @JsonIgnore
//    public String ars_assetguid;


    @Override
    public String toString() {
        return "CollectionObjectAttachment{" +
               "ordinal=" + ordinal +
               ", _tableName='" + _tableName + '\'' +
               ", collectionmemberid=" + collectionmemberid +
               ", collectionobject='" + collectionobject + '\'' +
               ", id='" + id + '\'' +
               ", version=" + version +
               '}';
    }
}
