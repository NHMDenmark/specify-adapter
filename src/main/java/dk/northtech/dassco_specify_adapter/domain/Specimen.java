package dk.northtech.dassco_specify_adapter.domain;

public record Specimen(
    String institution,
    String collection,
    String barcode,
    String specimen_pid,
    String preparation_type) {
    public Specimen(String barcode, String specimen_pid, String preparation_type) {
        this(null, null, barcode, specimen_pid, preparation_type);
    }
}