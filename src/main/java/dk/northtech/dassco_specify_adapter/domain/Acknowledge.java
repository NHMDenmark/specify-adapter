package dk.northtech.dassco_specify_adapter.domain;

import java.time.Instant;

public record Acknowledge(
    String asset_guid,
    AcknowledgeStatus status,
    String message,
    Instant date) {

    @Override
    public String toString() {
        return "Acknowledge{" +
               "asset_guid='" + asset_guid + '\'' +
               ", status=" + status +
               ", message='" + message + '\'' +
               ", date=" + date +
               '}';
    }

    public Acknowledge {
    }
}
