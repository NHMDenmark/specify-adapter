package dk.northtech.dassco_specify_adapter.domain;

import java.util.List;

public class CollectionObjectSearchResult {
    public List<CollectionObject> objects;
    public Meta meta;
    public class Meta {
        public int limit;
        public int offset;
        public int total_count;
    }
}
