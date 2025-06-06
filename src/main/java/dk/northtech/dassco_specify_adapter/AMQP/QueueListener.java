package dk.northtech.dassco_specify_adapter.AMQP;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import dk.northtech.dassco_specify_adapter.configuration.AMQPConfig;
import dk.northtech.dassco_specify_adapter.services.KeycloakService;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;

public abstract class QueueListener extends AbstractExecutionThreadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueListener.class);
    private static final int RABBIT_PORT = 5672;
    private static final int RABBIT_TLS_PORT = 5671;
    private int qbrMax;
    private KeycloakService keycloakService;
    private AMQPConfig amqpConfig;
    String queueName;
    QueueConnection connection;
    QueueSession session;

    public QueueListener(KeycloakService keycloakService, AMQPConfig amqpConfig, String queueName) {
        this.keycloakService = keycloakService;
        this.amqpConfig = amqpConfig;
        this.queueName = queueName;
        this.qbrMax = 0;
    }

    private String queueName() {
        return this.queueName;
    }

    String token() {
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
        try {
            QueueConnection queueConnection = getQueueConnectionFactory().createQueueConnection("", token());
            queueConnection.setExceptionListener(new MyExceptionListener());
            queueConnection.start();
            connection = queueConnection;
            System.out.println(token());
            session = queueConnection.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        } catch (JMSException e) {
            throw new RuntimeException("QueueListener failed to setup the connection", e);
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

    @Override
    protected void run() throws JMSException {
        Queue queue = session.createQueue(queueName());
        MessageConsumer messageConsumer = session.createConsumer(queue);

        while (isRunning()) {
            try {
                Message message = messageConsumer.receive(2000L);
                if (message != null) {
                    LOGGER.info("Received a message on {}", this.getClass().getSimpleName());
                }
                if (message instanceof TextMessage) {
                    handleMessage(((TextMessage) message).getText());
                }
            } catch (Exception e) {
                LOGGER.error("Error while receiving messages", e);
            }
        }
    }

    public abstract void handleMessage(String message);

    @Override
    protected void shutDown() {
        LOGGER.info("Shutting down {}", this.getClass().getSimpleName());
        try {
            if (session != null) {
                session.close();
                session = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (JMSException e) {
            throw new RuntimeException("An error occurred when trying to shut down " + this.getClass().getSimpleName(), e);
        }
    }

    private static class MyExceptionListener implements ExceptionListener {
        private Logger logger = LoggerFactory.getLogger(MyExceptionListener.class);
        @Override
        public void onException(JMSException exception) {
            logger.info("Connection ExceptionListener fired, exiting");
            logger.warn(exception.getMessage());
        }
    }
}
