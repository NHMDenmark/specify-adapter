package dk.northtech.dassco_specify_adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amqp-config")
public record AMQPConfig(String host
        , String acknowledgeQueueName
        , String secure
) {

}