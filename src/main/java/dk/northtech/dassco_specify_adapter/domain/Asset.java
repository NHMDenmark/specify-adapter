package dk.northtech.dassco_specify_adapter.domain;

import dk.northtech.dassco_specify_adapter.webapi.domain.HttpInfo;

import java.time.Instant;
import java.util.*;

public class Asset {
    public String asset_pid;
    public String asset_guid;
    public AssetStatus status;
    public boolean multi_specimen;
    //@Schema(description = "The barcodes of associated specimens", example = "'[\"ti-sp-00012\"']")
    //public List<String> specimen_barcodes = new ArrayList<>();
    public List<Specimen> specimens = new ArrayList<>();
    public String funding;
    public String subject;
    public String payload_type;
    public List<FileFormat> file_formats = new ArrayList<>();
    public boolean asset_locked;
    public List<InternalRole> restricted_access = new ArrayList<>();

    public Map<String, String> tags = new HashMap<>();
    public boolean audited;

    public Instant created_date;
    public Instant date_metadata_updated;
    public Instant date_asset_taken;
    public Instant date_asset_deleted;
    public Instant date_asset_finalised;
    public Instant date_metadata_taken;

    //References
    public String institution;
    public String parent_guid;
    public String collection;
    public HttpInfo httpInfo;
    public InternalStatus internal_status;
    public String updateUser;
    public List<Event> events;
    public String digitiser;
    public String workstation;
    public String pipeline;
    public String error_message;
    public Instant error_timestamp;
    public DasscoEvent event_name;
    public boolean writeAccess;
    public boolean synced;

    public String getAsset_guid() {
        return asset_guid;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "asset_pid='" + asset_pid + '\'' +
                ", asset_guid='" + asset_guid + '\'' +
                ", status=" + status +
                ", multi_specimen=" + multi_specimen +
                ", specimens=" + specimens +
                ", funding='" + funding + '\'' +
                ", subject='" + subject + '\'' +
                ", payload_type='" + payload_type + '\'' +
                ", file_formats=" + file_formats +
                ", asset_locked=" + asset_locked +
                ", restricted_access=" + restricted_access +
                ", tags=" + tags +
                ", audited=" + audited +
                ", created_date=" + created_date +
                ", date_metadata_updated=" + date_metadata_updated +
                ", date_asset_taken=" + date_asset_taken +
                ", date_asset_deleted=" + date_asset_deleted +
                ", date_asset_finalised=" + date_asset_finalised +
                ", date_metadata_taken=" + date_metadata_taken +
                ", institution='" + institution + '\'' +
                ", parent_guid='" + parent_guid + '\'' +
                ", collection='" + collection + '\'' +
                ", httpInfo=" + httpInfo +
                ", internal_status=" + internal_status +
                ", updateUser='" + updateUser + '\'' +
                ", events=" + events +
                ", digitiser='" + digitiser + '\'' +
                ", workstation='" + workstation + '\'' +
                ", pipeline='" + pipeline + '\'' +
                ", error_message='" + error_message + '\'' +
                ", error_timestamp=" + error_timestamp +
                ", synced=" + synced +
                '}';
    }

    @Override
    public boolean equals(Object o) { // does NOT compare the creation_date, workstation, pipeline and digitiser (to be able to compare the objects regardless of the Event linked to it)
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return multi_specimen == asset.multi_specimen && asset_locked == asset.asset_locked && audited == asset.audited && Objects.equals(asset_pid, asset.asset_pid) && Objects.equals(asset_guid, asset.asset_guid) && status == asset.status && Objects.equals(specimens, asset.specimens) && Objects.equals(funding, asset.funding) && Objects.equals(subject, asset.subject) && Objects.equals(payload_type, asset.payload_type) && Objects.equals(file_formats, asset.file_formats) && Objects.equals(restricted_access, asset.restricted_access) && Objects.equals(tags, asset.tags) && Objects.equals(date_metadata_updated, asset.date_metadata_updated) && Objects.equals(date_asset_taken, asset.date_asset_taken) && Objects.equals(date_asset_deleted, asset.date_asset_deleted) && Objects.equals(date_asset_finalised, asset.date_asset_finalised) && Objects.equals(date_metadata_taken, asset.date_metadata_taken) && Objects.equals(institution, asset.institution) && Objects.equals(parent_guid, asset.parent_guid) && Objects.equals(collection, asset.collection) && Objects.equals(httpInfo, asset.httpInfo) && internal_status == asset.internal_status && Objects.equals(updateUser, asset.updateUser) && Objects.equals(events, asset.events) && Objects.equals(error_message, asset.error_message) && Objects.equals(error_timestamp, asset.error_timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asset_pid, asset_guid, status, multi_specimen, specimens, funding, subject, payload_type, file_formats, asset_locked, restricted_access, tags, audited, date_metadata_updated, date_asset_taken, date_asset_deleted, date_asset_finalised, date_metadata_taken, institution, parent_guid, collection, httpInfo, internal_status, updateUser, events, error_message, error_timestamp);
    }
}
