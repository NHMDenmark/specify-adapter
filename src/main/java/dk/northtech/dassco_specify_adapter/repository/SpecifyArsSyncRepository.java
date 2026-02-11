package dk.northtech.dassco_specify_adapter.repository;

import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyArsSyncBatch;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface SpecifyArsSyncRepository extends SqlObject {
    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO specify_ars_sync_batch (batch_timestamp, specify_from_timestamp, specify_to_timestamp, status, additional_info) VALUES (:batch_timestamp,:specify_from_timestamp,:specify_to_timestamp,:status,:additional_info)")
    public Integer createNewBatch(@BindMethods SpecifyArsSyncBatch batch);

    @SqlQuery("SELECT * FROM specify_ars_sync_batch WHERE status IN ('PARTIAL_SUCCESS', 'SUCCESSFUL', 'STARTED') ORDER BY specify_to_timestamp LIMIT 1")
    public SpecifyArsSyncBatch getLatestNonFailed();
}
