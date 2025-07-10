package dk.northtech.dassco_specify_adapter.domain;

import java.util.HashSet;

public record Specimen(
        String institution,
        String collection,
        String barcode,
        String specimen_pid,
        HashSet<String> preparation_types,
        String asset_preparation_type,
        Integer specimen_id,
        Integer collection_id,
        Long specify_collection_object_attachment_id,
        boolean asset_detached
) {
    public Specimen(String barcode, String specimen_pid, HashSet<String> preparation_types, String asset_preparation_type) {
        this(null, null, barcode, specimen_pid, preparation_types, asset_preparation_type, null, null, null, false);
    }

    public Specimen {
    }
    //    new Specimen(createAsset.institution, "i1_c1", "creatAsset-sp-2", "spid2", new HashSet<>(Set.of("pinning")),"pinning",
    public Specimen(String institution, String collection, String barcode, String specimen_pid, HashSet<String> preparation_types, String asset_preparation_type) {
        this(institution, collection, barcode, specimen_pid, preparation_types, asset_preparation_type,null, null, null, false);
    }
}
