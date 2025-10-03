package dk.northtech.dassco_specify_adapter.domain;

import javax.annotation.Nullable;

public class AssetSpecimen {
    public Integer specimen_id;
    public String asset_guid;
    public String specimen_pid;
    public Long asset_specimen_id;
    public String asset_preparation_type;
    public Long specify_collection_object_attachment_id;
    public boolean asset_detached;
    @Nullable
    public Specimen specimen;

    public AssetSpecimen(String asset_guid, String specimen_pid, String asset_preparation_type, boolean asset_detached) {
        this.asset_guid = asset_guid;
        this.specimen_pid = specimen_pid;
        this.asset_preparation_type = asset_preparation_type;
        this.asset_detached = asset_detached;
    }

    public AssetSpecimen(boolean asset_detached, Long specify_collection_object_attachment_id, String asset_preparation_type, Long asset_specimen_id, String specimen_pid, String asset_guid, Integer specimen_id) {
        this.asset_detached = asset_detached;
        this.specify_collection_object_attachment_id = specify_collection_object_attachment_id;
        this.asset_preparation_type = asset_preparation_type;
        this.asset_specimen_id = asset_specimen_id;
        this.specimen_pid = specimen_pid;
        this.asset_guid = asset_guid;
        this.specimen_id = specimen_id;
    }

    public AssetSpecimen(boolean asset_detached, Long specify_collection_object_attachment_id, String asset_preparation_type, String specimen_pid, String asset_guid) {
        this.asset_detached = asset_detached;
        this.specify_collection_object_attachment_id = specify_collection_object_attachment_id;
        this.asset_preparation_type = asset_preparation_type;
        this.specimen_pid = specimen_pid;
        this.asset_guid = asset_guid;
    }

    public AssetSpecimen() {
    }
}
