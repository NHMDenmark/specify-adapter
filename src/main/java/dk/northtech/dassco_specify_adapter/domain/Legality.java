package dk.northtech.dassco_specify_adapter.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public final class Legality {
    @JsonIgnore
    private Long legality_id;
    public String copyright;
    public String license;
    public String credit;

    public Legality(Long legality_id, String copyright, String license, String credit) {
        this.legality_id = legality_id;
        this.copyright = copyright;
        this.license = license;
        this.credit = credit;
    }

    public Legality() {
    }

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

    @JsonIgnore
    public Long legality_id() {
        return legality_id;
    }

    public String copyright() {
        return copyright;
    }

    public String license() {
        return license;
    }

    public String credit() {
        return credit;
    }

    @Override
    public String toString() {
        return "Legality[" +
                "legality_id=" + legality_id + ", " +
                "copyright=" + copyright + ", " +
                "license=" + license + ", " +
                "credit=" + credit + ']';
    }

}
