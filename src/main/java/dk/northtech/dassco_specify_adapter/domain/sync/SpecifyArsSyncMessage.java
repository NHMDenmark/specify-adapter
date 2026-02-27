package dk.northtech.dassco_specify_adapter.domain.sync;

import dk.northtech.dassco_specify_adapter.domain.Asset;

import java.util.List;
import java.util.Set;

public class SpecifyArsSyncMessage {
    public Asset asset;
    //Those are the fields that have specifymappings
    public Set<String> updatedFields;
    public SpecifyArsSyncMessage() {
    }

    public SpecifyArsSyncMessage(Asset asset, Set<String> updatedFields) {
        this.asset = asset;
        this.updatedFields = updatedFields;
    }
}
