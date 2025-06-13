package dk.northtech.dassco_specify_adapter.AMQP;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.northtech.dassco_specify_adapter.configuration.AMQPConfig;
import dk.northtech.dassco_specify_adapter.domain.ARSUpdate;
import dk.northtech.dassco_specify_adapter.domain.SpecifySyncService;
import dk.northtech.dassco_specify_adapter.services.KeycloakService;
import dk.northtech.dassco_specify_adapter.services.SpecifyEndpointService;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class AssetQueueListener extends QueueListener {

    private final SpecifySyncService specifySyncService;
    @Inject
    public AssetQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig, SpecifySyncService specifySyncService) {
        super(keycloakService, amqpConfig, amqpConfig.assetQueueName());
        this.specifySyncService = specifySyncService;
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN ASSET LISTENER:");
        System.out.println(message);
//        int statusCode = this.specifyAdapterClient.sendAssets(message);
        this.specifySyncService.sync(message);
        System.out.println("sent them off and got status code: ");
    }
}
