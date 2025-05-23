package dk.northtech.dassco_specify_adapter.domain;

import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.*;

public class Asset {
    public String asset_pid;
    public String asset_guid;
    public String status;
    public boolean multi_specimen;
    public List<Specimen> specimens = new ArrayList<>();
    public List<String> funding = new ArrayList<>();
    public String asset_subject;
    public String payload_type;
    public List<String> file_formats = new ArrayList<>();
    public boolean asset_locked;
    public List<InternalRole> restricted_access = new ArrayList<>();

    public Map<String, String> tags = new HashMap<>();
    public boolean audited;

    public Instant created_date;
    public Instant date_metadata_updated;
    public Instant date_asset_taken;
    public Instant date_asset_deleted;
    public Instant date_audited;
    public String audited_by;
    public Instant date_asset_finalised;
    //References
    public String institution;

    public Set<String> parent_guids = new HashSet<>();
    public String collection;
    public InternalStatus internal_status;
    public String updateUser;
    public List<Event> events;

    public String workstation;
    public String pipeline;
    public String error_message;
    public Instant error_timestamp;
    public DasscoEvent event_name;
    public boolean writeAccess;


    // new fields
    public String camera_setting_control;
    public Instant date_metadata_ingested;

    public String metadata_version;

    public String metadata_source;

    public String mos_id;

    public boolean make_public;

    public boolean push_to_specify;
//    public List<Issue> issues;
    public String digitiser;
    public List<String> complete_digitiser_list = new ArrayList<>();

    public String specify_attachment_remarks;
    public String specify_attachment_title;

    public String metadata_updated_by;
    public List<Publication> external_publishers;
    public Legality legality;
    public String metadata_created_by;
//    public List<String> file_formats;
    // Internal ids for database operations
    public transient Integer workstation_id;
    public transient Integer digitiser_id;
    public transient Integer collection_id;
    public transient Integer updating_pipeline_id;
    public String updating_pipeline;

    public String getAsset_guid() {
        return asset_guid;
    }


    @Override
    public String toString() {
        return "Asset{" +
               "asset_pid='" + asset_pid + '\'' +
               ", asset_guid='" + asset_guid + '\'' +
               ", status='" + status + '\'' +
               ", multi_specimen=" + multi_specimen +
               ", specimens=" + specimens +
               ", funding=" + funding +
               ", subject='" + asset_subject + '\'' +
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
               ", institution='" + institution + '\'' +
               ", parent_guid='" + parent_guids + '\'' +
               ", collection='" + collection + '\'' +
               ", httpInfo=" + httpInfo +
               ", internal_status=" + internal_status +
               ", updateUser='" + updateUser + '\'' +
               ", events=" + events +
               ", workstation='" + workstation + '\'' +
               ", pipeline='" + pipeline + '\'' +
               ", error_message='" + error_message + '\'' +
               ", error_timestamp=" + error_timestamp +
               ", event_name=" + event_name +
               ", writeAccess=" + writeAccess +
               ", camera_setting_control='" + camera_setting_control + '\'' +
               ", date_metadata_ingested=" + date_metadata_ingested +
               ", metadata_version='" + metadata_version + '\'' +
               ", metadata_source='" + metadata_source + '\'' +
               ", mos_id='" + mos_id + '\'' +
               ", make_public=" + make_public +
               ", push_to_specify=" + push_to_specify +
               ", issues=" + issues +
               ", digitiser='" + digitiser + '\'' +
               ", complete_digitiser_list=" + complete_digitiser_list +
               '}';
    }

    //TODO maybe we need to handle the new lists here
    @Override
    public boolean equals(Object o) { // does NOT compare the creation_date, workstation, pipeline and digitiser (to be able to compare the objects regardless of the Event linked to it)
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asset asset = (Asset) o;
        return multi_specimen == asset.multi_specimen && asset_locked == asset.asset_locked && audited == asset.audited && Objects.equals(asset_pid, asset.asset_pid) && Objects.equals(asset_guid, asset.asset_guid) && status == asset.status && Objects.equals(specimens, asset.specimens) && Objects.equals(funding, asset.funding) && Objects.equals(asset_subject, asset.asset_subject) && Objects.equals(payload_type, asset.payload_type) && Objects.equals(file_formats, asset.file_formats) && Objects.equals(restricted_access, asset.restricted_access) && Objects.equals(tags, asset.tags) && Objects.equals(date_metadata_updated, asset.date_metadata_updated) && Objects.equals(date_asset_taken, asset.date_asset_taken) && Objects.equals(date_asset_deleted, asset.date_asset_deleted) && Objects.equals(date_asset_finalised, asset.date_asset_finalised) && Objects.equals(institution, asset.institution) && Objects.equals(parent_guids, asset.parent_guids) && Objects.equals(collection, asset.collection) && Objects.equals(httpInfo, asset.httpInfo) && internal_status == asset.internal_status && Objects.equals(updateUser, asset.updateUser) && Objects.equals(events, asset.events) && Objects.equals(error_message, asset.error_message) && Objects.equals(error_timestamp, asset.error_timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asset_pid, asset_guid, status, multi_specimen, specimens, funding, asset_subject, payload_type, file_formats, asset_locked, restricted_access, tags, audited, date_metadata_updated, date_asset_taken, date_asset_deleted, date_asset_finalised, institution, parent_guids, collection, httpInfo, internal_status, updateUser, events, error_message, error_timestamp);
    }
}
