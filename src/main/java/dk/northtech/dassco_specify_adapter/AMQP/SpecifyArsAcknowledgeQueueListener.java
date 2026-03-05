package dk.northtech.dassco_specify_adapter.AMQP;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.configuration.AMQPConfig;
import dk.northtech.dassco_specify_adapter.domain.sync.SyncAcknowledge;
import dk.northtech.dassco_specify_adapter.services.KeycloakService;
import dk.northtech.dassco_specify_adapter.services.SpecifySyncService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SpecifyArsAcknowledgeQueueListener extends QueueListener {
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final SpecifySyncService specifySyncService;
    private static final Logger log = LoggerFactory.getLogger(SpecifyArsAcknowledgeQueueListener.class);
    @Inject
    public SpecifyArsAcknowledgeQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig, SpecifySyncService specifySyncService) {
        super(keycloakService, amqpConfig, amqpConfig.specifyArsSyncAcknowledgeQueueName());
        this.specifySyncService = specifySyncService;
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN SPECIFY ACK LISTENER:");
        System.out.println(message);
//        int statusCode = this.specifyAdapterClient.sendAssets(message);
        try {
            SyncAcknowledge acknowledge = mapper.readValue(message, SyncAcknowledge.class);
            this.specifySyncService.handleAcknowledge(acknowledge);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse the following specify ars sync acnknowledge message: {}",message);

            throw new RuntimeException("Failed to parse specify sync acknowledge", e);
        }

        System.out.println("sent them off and got status code: ");
    }
}
