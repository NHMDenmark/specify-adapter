package dk.northtech.dassco_specify_adapter.domain.specify;

import dk.northtech.dassco_specify_adapter.domain.Attachment;

import java.util.List;

public class AttachmentSearchResult {
    public List<Attachment> objects;
    public Meta meta;

    public class Meta {
        public int limit;
        public int offset;
        public int total_count;
    }
}
