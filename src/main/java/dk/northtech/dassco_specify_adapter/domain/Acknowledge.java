package dk.northtech.dassco_specify_adapter.domain;

import java.time.Instant;
import java.util.List;

public record Acknowledge(
        String asset_guid,
        AcknowledgeStatus status,
        String message,
        Instant date,
        List<AssetSpecimen> specimensWithSpecifyIds) {



    @Override
    public String toString() {
        return "Acknowledge{" +
                "asset_guid='" + asset_guid + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", date=" + date +
                ", specimensWithSpecifyIds=" + specimensWithSpecifyIds +
                '}';
    }
}
