package dk.northtech.dassco_specify_adapter.AMQP;


import dk.northtech.dassco_specify_adapter.configuration.AMQPConfig;
import dk.northtech.dassco_specify_adapter.services.KeycloakService;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class AssetQueueListener extends QueueListener {


    @Inject
    public AssetQueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig) {
        super(keycloakService, amqpConfig, amqpConfig.assetQueueName());
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("MESSAGE IN ASSET LISTENER:");
        System.out.println(message);
//        int statusCode = this.specifyAdapterClient.sendAssets(message);
        System.out.println("sent them off and got status code: ");
    }
}
