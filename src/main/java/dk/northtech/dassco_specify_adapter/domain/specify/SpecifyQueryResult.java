package dk.northtech.dassco_specify_adapter.domain.specify;

import java.util.List;

public class SpecifyQueryResult {
// Specify delivers search result as an array of object arrays,
// when querying for collection objects the first entry in the object array will be the collection object id.
// The next entries will be matched values, in our case the modified by date
    public List<List<Object>> results;


}
