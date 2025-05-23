package dk.northtech.dassco_specify_adapter.domain;

import java.util.Objects;

public record Publication(
        Long publication_id,

        String asset_guid,

        String description,

        String name
) {


    public Publication(String asset_guid, String description, String name) {
        this(null, asset_guid, description, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Publication that = (Publication) o;
        return Objects.equals(asset_guid, that.asset_guid) && Objects.equals(description, that.description) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asset_guid, description, name);
    }

}
