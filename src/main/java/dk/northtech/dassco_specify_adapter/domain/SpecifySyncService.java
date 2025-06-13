package dk.northtech.dassco_specify_adapter.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.AMQP.QueueBroadcaster;
import dk.northtech.dassco_specify_adapter.services.MappingService;
import dk.northtech.dassco_specify_adapter.services.SpecifyEndpointService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SpecifySyncService {
    private static final Logger log = LoggerFactory.getLogger(SpecifySyncService.class);
    private QueueBroadcaster queueBroadcaster;
    private SpecifyEndpointService specifyEndpointService;
    private MappingService mappingService;

    @Inject
    public SpecifySyncService(QueueBroadcaster queueBroadcaster, SpecifyEndpointService specifyEndpointService, MappingService mappingService) {
        this.queueBroadcaster = queueBroadcaster;
        this.specifyEndpointService = specifyEndpointService;
        this.mappingService = mappingService;
    }


    public void sync(String arsUpdateJson) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            ARSUpdate arsUpdate = mapper.readValue(arsUpdateJson, ARSUpdate.class);
            CollectionObjectAttachment attachment = mappingService.getAttachment(arsUpdate.asset);
//            specifyEndpointService.pushImageToSpecify(attachment);
            Acknowledge ack = new Acknowledge(attachment.ars_assetguid, AcknowledgeStatus.FILE_UPLOAD_ERROR, "Failed to sync specify: Not implemented yet", Instant.now());
            queueBroadcaster.sendMessage(ack);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
