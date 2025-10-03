package dk.northtech.dassco_specify_adapter.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record Specimen(
        String institution,
        String collection,
        String barcode,
        String specimen_pid,
        HashSet<String> preparation_types,
        Integer specimen_id,
        Integer collection_id,
        List<Role> role_restrictions
) {
    public Specimen(String barcode, String specimen_pid, HashSet<String> preparation_types) {
        this(null, null, barcode, specimen_pid, preparation_types, null, null, new ArrayList<>());
    }

    public Specimen(Specimen specimen, Integer specimen_id ,Integer collecion_id) {
        this(specimen.institution, specimen.collection, specimen.barcode, specimen.specimen_pid, specimen.preparation_types, specimen_id,collecion_id, specimen.role_restrictions());
    }


    public Specimen(Integer collection_id, Integer specimen_id, HashSet<String> preparation_types, String specimen_pid, String barcode, String collection, String institution) {
        this(institution, collection, barcode, specimen_pid, preparation_types, specimen_id, collection_id, List.of());
    }

    public Specimen {
    }
}
