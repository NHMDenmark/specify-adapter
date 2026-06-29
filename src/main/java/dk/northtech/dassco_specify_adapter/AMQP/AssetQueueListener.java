package dk.northtech.dassco_specify_adapter.AMQP;


import dk.northtech.dassco_specify_adapter.configuration.AMQPConfig;
import dk.northtech.dassco_specify_adapter.services.SpecifySyncService;
import dk.northtech.dassco_specify_adapter.services.KeycloakService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AssetQueueListener extends QueueListener {

    private static final Logger log = LoggerFactory.getLogger(AssetQueueListener.class);
    private final SpecifySyncService specifySyncService;
    @Inject
    public AssetQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig, SpecifySyncService specifySyncService) {
        super(keycloakService, amqpConfig, amqpConfig.assetQueueName());
        this.specifySyncService = specifySyncService;
    }

    @Override
    public void handleMessage(String message) {
        log.info("MESSAGE IN ASSET LISTENER:");
        log.info(message);
//        int statusCode = this.specifyAdapterClient.sendAssets(message);
        this.specifySyncService.arsToSpecifySync(message);
    }
}
