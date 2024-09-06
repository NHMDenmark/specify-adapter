package dk.northtech.dassco_specify_adapter.AMQP;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ServiceManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!tests") // will cause errors on contextLoads() in tests if the queue image isn't running.
@Component
public class QueueServiceHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueServiceHandler.class);
    private final ServiceManager serviceManager;

    public QueueServiceHandler(QueueBroadcaster queueBroadcaster) {
        this.serviceManager = new ServiceManager(ImmutableList.of(queueBroadcaster));
    }

    @PostConstruct
    public void startup() {
        LOGGER.info("Services init");
        this.serviceManager.startAsync();
        this.serviceManager.awaitHealthy();
        LOGGER.info("Services running");
    }

    @PreDestroy
    public void teardown() {
        LOGGER.info("Services teardown");
        serviceManager.stopAsync();
        serviceManager.awaitStopped();
        LOGGER.info("Services shut down");
    }
}
