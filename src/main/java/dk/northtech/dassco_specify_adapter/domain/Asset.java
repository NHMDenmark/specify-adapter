package dk.northtech.dassco_specify_adapter.domain;


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
               ", digitiser='" + digitiser + '\'' +
               ", complete_digitiser_list=" + complete_digitiser_list +
               '}';
    }

}
