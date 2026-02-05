package dk.northtech.dassco_specify_adapter.domain.specify;

import java.time.LocalDate;
import java.util.List;

public class SpecifyQuery {
    public String name;
    public String contextname;
    public int contexttableid;
    public boolean selectdistinct;
    public boolean smushed;
    public boolean countonly;
    public boolean formatauditrecids;
    public String specifyuser;
    public boolean isfavorite;
    public int ordinal;

    public List<Field> fields;

    public String _tablename;
    public String remarks;
    public String searchsynonymy;
    public String sqlstr;

    public LocalDate timestampcreated;
    public LocalDate timestampmodified;

    public String createdbyagent;
    public String modifiedbyagent;

    public int version;
    public int offset;
    public int limit;

    // ---------- Inner class for fields ----------

    public static class Field {

        public String tablelist;
        public String stringid;
        public String fieldname;
        public boolean isrelfld;
        public int sorttype;
        public int position;
        public boolean isdisplay;
        public int operstart;
        public String startvalue;
        public boolean isnot;
        public boolean isstrict;
    }
}