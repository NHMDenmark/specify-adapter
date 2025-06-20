package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.AMQP.QueueBroadcaster;
import dk.northtech.dassco_specify_adapter.domain.ARSUpdate;
import dk.northtech.dassco_specify_adapter.domain.Acknowledge;
import dk.northtech.dassco_specify_adapter.domain.AcknowledgeStatus;
import dk.northtech.dassco_specify_adapter.domain.SpecifyAdapterException;
import dk.northtech.dassco_specify_adapter.domain.specify.CollectionObjectAttachment;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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
            Acknowledge acknowledge;
            try {
                AcknowledgeStatus acknowledgeStatus = specifyEndpointService.pushImageToSpecify(attachment);
                queueBroadcaster.sendMessage(new Acknowledge(attachment.ars_assetguid, acknowledgeStatus, null, Instant.now()));
            } catch (SpecifyAdapterException spx) {
                queueBroadcaster.sendMessage(new Acknowledge(attachment.ars_assetguid, spx.status(), spx.getMessage(), Instant.now()));
            } catch (Exception ex) {
                queueBroadcaster.sendMessage(new Acknowledge(attachment.ars_assetguid, AcknowledgeStatus.UNKNOWN_ERROR, "Error syncing file to specify, please check the logs of Specify Bridge", Instant.now()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
