package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.AMQP.QueueBroadcaster;
import dk.northtech.dassco_specify_adapter.domain.Acknowledge;
import dk.northtech.dassco_specify_adapter.domain.AcknowledgeStatus;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Disabled
@SpringBootTest
@ActiveProfiles("tests")
class QueueTests {

    @Inject
    private QueueBroadcaster queueBroadcaster;

    @Test
    @Disabled // only works if the queue is running in the other app. is just for manual testing purposes.
    public void adapter() {
        String guid = "test_guid";
//        guids.add("test_guid");
        Acknowledge acknowledge = new Acknowledge(guid, AcknowledgeStatus.SUCCESS, "such good", Instant.now(),null);

        this.queueBroadcaster.sendMessage(acknowledge);
    }
}
