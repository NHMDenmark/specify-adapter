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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Disabled
@SpringBootTest
class QueueTests {

    @Inject
    private QueueBroadcaster queueBroadcaster;

    @Test
    @Disabled // only works if the queue is running in the other app. is just for manual testing purposes.
    public void adapter() {
        List<String> guids = new ArrayList<>();
        guids.add("test_guid");
        Acknowledge acknowledge = new Acknowledge(guids, AcknowledgeStatus.SUCCESS, "such good", Instant.now());
        ObjectWriter ow = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();
        try {
            String json = ow.writeValueAsString(acknowledge);
            this.queueBroadcaster.sendMessage(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}