package dk.northtech.dassco_specify_adapter.domain.specify;

import java.util.List;

public class PreparationSearchResult {
    public List<Preparation> objects;
    public Meta meta;

    public class Meta {
        public int limit;
        public int offset;
        public int total_count;
    }
}
