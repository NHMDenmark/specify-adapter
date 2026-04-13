package dk.northtech.dassco_specify_adapter.services;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SpecifySyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(SpecifySyncScheduler.class);
    private final SpecifySyncService specifySyncService;

    @Inject
    public SpecifySyncScheduler(SpecifySyncService specifySyncService) {
        this.specifySyncService = specifySyncService;
    }

    @Scheduled(cron = "${sync.specify-to-ars.cron:0 0 3 * * *}", zone = "Europe/Copenhagen")
    public void runDailySpecifyToArsSync() {
        log.info("Starting scheduled Specify to ARS sync");
        try {
            specifySyncService.specifyToArsSync();
            log.info("Finished scheduled Specify to ARS sync");
        } catch (Exception e) {
            log.error("Scheduled Specify to ARS sync failed", e);
        }
    }
}
