package dk.northtech.dassco_specify_adapter.domain;

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
