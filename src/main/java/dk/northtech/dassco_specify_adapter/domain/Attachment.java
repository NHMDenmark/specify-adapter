package dk.northtech.dassco_specify_adapter.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment {
    public String attachmentlocation;
    public String mimetype;
    public String origfilename;
    public String title;
    public boolean ispublic;
    public String remarks;
    public String filecreateddate;
    // Date Media Deleted. This intentional, it appears that an existing field has been repurposed.
    public String copyrightdate;

    // Legalities
    public String copyrightholder;
    public String license;
    public String credit;

    // specify update - do these match across environments?
    public final Integer tableid = 111;
    public final String _tableName = "Attachment";

    public Integer id;
    public String attachmentstorageconfig;
    public String capturedevice;
    public String dateimaged;
    public String guid;
    public String licenselogourl;
    public String metadatatext;
    public Integer scopeid;
    public Integer scopetype;
    public String subjectorientation;
    public String subtype;
    public String timestampcreated;
    public String timestampmodified;
    public String type;
    public Integer version;
    public Integer visibility;

    public String accessionattachments;
    public String agentattachments;
    public String attachmentimageattribute;
    public String borrowattachments;
    public String collectingeventattachments;
    public String collectingtripattachments;
    public String collectionobjectattachments;
    public String conservdescriptionattachments;
    public String conserveventattachments;
    public String createdbyagent;
    public String creator;
    public String deaccessionattachments;
    public String disposalattachments;
    public String dnasequenceattachments;
    public String dnasequencingrunattachments;
    public String exchangeinattachments;
    public String exchangeoutattachments;
    public String fieldnotebookattachments;
    public String fieldnotebookpageattachments;
    public String fieldnotebookpagesetattachments;
    public String giftattachments;
    public String loanattachments;
    public String localityattachments;
    public String metadata;
    public String modifiedbyagent;
    public String permitattachments;
    public String preparationattachments;
    public String referenceworkattachments;
    public String repositoryagreementattachments;
    public String storageattachments;
    public String tags;
    public String taxonattachments;
    public String treatmenteventattachments;
    public String visibilitysetby;



    @Override
    public String toString() {
        return "Attachment{" +
               "attachmentlocation='" + attachmentlocation + '\'' +
               ", mimetype='" + mimetype + '\'' +
               ", origfilename='" + origfilename + '\'' +
               ", title='" + title + '\'' +
               ", ispublic=" + ispublic +
               ", remarks='" + remarks + '\'' +
               ", filecreateddate='" + filecreateddate + '\'' +
               ", copyrightdate='" + copyrightdate + '\'' +
               ", copyrightholder='" + copyrightholder + '\'' +
               ", license='" + license + '\'' +
               ", credit='" + credit + '\'' +
               ", tableid=" + tableid +
               ", _tableName='" + _tableName + '\'' +
               '}';
    }
}
