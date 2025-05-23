package dk.northtech.dassco_specify_adapter.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public record Legality(@JsonIgnore Long legality_id, String copyright, String license, String credit) {



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Legality legality = (Legality) o;
        return Objects.equals(legality_id, legality.legality_id) && Objects.equals(copyright, legality.copyright) && Objects.equals(license, legality.license) && Objects.equals(credit, legality.credit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(legality_id, copyright, license, credit);
    }
}
