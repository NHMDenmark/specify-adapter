package dk.northtech.dassco_specify_adapter.AMQP;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.util.concurrent.AbstractIdleService;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import dk.northtech.dassco_specify_adapter.configuration.AMQPConfig;
import dk.northtech.dassco_specify_adapter.domain.Acknowledge;
import dk.northtech.dassco_specify_adapter.services.KeycloakService;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class QueueBroadcaster extends AbstractIdleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueBroadcaster.class);
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private int qbrMax;
    private KeycloakService keycloakService;
    private AMQPConfig amqpConfig;
    QueueSender sender;
    QueueSession session;
    QueueConnection queueConnection;
    private Instant lastRestart;
    ObjectWriter writer = new ObjectMapper().registerModule(new JavaTimeModule()).writer().withDefaultPrettyPrinter();
    @Inject
    public QueueBroadcaster(KeycloakService keycloakService, AMQPConfig amqpConfig) {
        this.keycloakService = keycloakService;
        this.amqpConfig = amqpConfig;
        this.qbrMax = 0;
    }

    private String queueName() {
        return this.amqpConfig.acknowledgeQueueName();
    }

    private String token() {
        return this.keycloakService.getUserServiceToken();
    }

    private String hostname() {
        return this.amqpConfig.host();
    }

    private boolean isSecure() {
        return Boolean.parseBoolean(this.amqpConfig.secure());
    }

    @Override
    protected void startUp() {
        LOGGER.info("Initializing {}", this.getClass().getSimpleName());
        init();

    }

    protected void init() {
        try {
            this.queueConnection = getQueueConnectionFactory().createQueueConnection("", token());
            this.queueConnection.start();
            this.session = this.queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            Queue queue = this.session.createQueue(queueName());
            this.sender = this.session.createSender(queue);
            this.sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            this.lastRestart = Instant.now();
        } catch (JMSException e) {
            throw new RuntimeException("QueueBroadcaster failed to connect to the queue", e);
        }
    }

    private QueueConnectionFactory getQueueConnectionFactory(){
        return (QueueConnectionFactory)getConnectionFactory();
    }

    private ConnectionFactory getConnectionFactory() {
        RMQConnectionFactory rmqCF = new RMQConnectionFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public Connection createConnection(String userName, String password) throws JMSException {
                if (!isSecure()) {
                    this.setPort(RABBIT_PORT);
                } else {
                    this.setPort(RABBIT_TLS_PORT);
                }
                return super.createConnection(userName, password);
            }
        };
        if (isSecure()) {
            try {
                rmqCF.useSslProtocol();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("An error occurred when trying to use SSL protocol on the RMQConnectionFactory.", e);
            }
        }
        rmqCF.setHost(hostname());
        rmqCF.setQueueBrowserReadMax(qbrMax);
        return rmqCF;
    }

    public void sendMessage(Acknowledge acknowledge) {
        synchronized (this) {
            if (lastRestart.plus(58, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                LOGGER.info("Refreshing sesh");
                this.closeSession();
                this.init();
            }
        }
        try {
            LOGGER.info("Sending object {}", acknowledge);

            sender.send(textMessage(this.session, writer.writeValueAsString(acknowledge)));
        } catch (Exception e) {
            LOGGER.error("Could not send object {}", acknowledge);
            throw new RuntimeException("An error occurred when trying to send the object to the queue", e);
        }
    }

    private Message textMessage(QueueSession session, String message) {
        try {
            return session.createTextMessage(message);
        } catch (JMSException e) {
            throw new RuntimeException("An error occurred when trying to turn the message into a Message object.", e);
        }
    }

    protected void closeSession() {
        try {
            if (this.sender != null) {
                this.sender.close();
            }
            if (this.queueConnection != null) {
                this.queueConnection.stop();
                this.session.close();
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void shutDown() {
        LOGGER.info("Shutting down {}", this.getClass().getSimpleName());
        closeSession();
        LOGGER.info("{} is shut down", this.getClass().getSimpleName());
    }
}
