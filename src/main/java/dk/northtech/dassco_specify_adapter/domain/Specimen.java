package dk.northtech.dassco_specify_adapter.domain;

import java.util.List;

public record Specimen(
        String institution,
        String collection,
        String barcode,
        String specimen_pid,
        String preparation_type,
        String asset_preparation_type,
        List<String> preparation_types) {
    public Specimen(String barcode, String specimen_pid, String preparation_type, List<String> preparation_types, String asset_preparation_type) {
        this(null, null, barcode, specimen_pid, preparation_type, asset_preparation_type, preparation_types);
    }
}