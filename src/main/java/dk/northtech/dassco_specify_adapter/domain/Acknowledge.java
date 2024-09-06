package dk.northtech.dassco_specify_adapter.domain;

import java.time.Instant;
import java.util.List;

public record Acknowledge(
    List<String> assetGuids,
    AcknowledgeStatus status,
    String body,
    Instant date) {

    public Acknowledge {
    }
}
