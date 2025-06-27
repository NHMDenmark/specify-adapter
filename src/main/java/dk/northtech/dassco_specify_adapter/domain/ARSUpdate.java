package dk.northtech.dassco_specify_adapter.domain;

import java.util.List;

public class ARSUpdate {
    public Asset asset;
    public boolean deleteAttachment = false;

    public ARSUpdate(Asset asset) {
        this.asset = asset;
    }

    public ARSUpdate() {
    }

    public ARSUpdate(Asset asset, boolean deleteAttachment) {
        this.asset = asset;
        this.deleteAttachment = deleteAttachment;
    }
}
