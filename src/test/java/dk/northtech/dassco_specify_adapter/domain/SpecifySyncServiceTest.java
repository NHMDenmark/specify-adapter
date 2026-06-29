package dk.northtech.dassco_specify_adapter.domain;

import dk.northtech.dassco_specify_adapter.domain.sync.*;
import dk.northtech.dassco_specify_adapter.repository.SpecifyArsSyncRepository;
import dk.northtech.dassco_specify_adapter.services.LogService;

import dk.northtech.dassco_specify_adapter.services.SpecifySyncService;
import jakarta.inject.Inject;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
@SpringBootTest
@ActiveProfiles("tests")
@Testcontainers
@DirtiesContext
class SpecifySyncServiceTest {
    @Inject
    LogService logService;
    @Inject
    SpecifySyncService specifySyncService;
    @Inject
    Jdbi jdbi;
    private static final Logger logger = LoggerFactory.getLogger(SpecifySyncServiceTest.class);
    private static Network network = Network.newNetwork();
    @Container
    static GenericContainer postgreSQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName( "dassco_bridge")
            .withPassword("dassco_bridge")
            .withUsername("dassco_bridge")
            .withExposedPorts(5432)
//            .waitingFor(Wait.forLogMessage("ready to accept connections",1))
            .withNetwork(network).withNetworkAliases("database");

    //
    static {
        postgreSQL.start();
        String url =  "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_bridge?user=dassco_bridge&password=dassco_bridge";
        try (Connection conn = DriverManager.getConnection(url);) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
            Liquibase liquibase  = new Liquibase("/liquibase/changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts("development", "default") );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_bridge");
    }

    @BeforeEach
    void resetTables() {
        jdbi.useHandle(handle -> {
            handle.execute("DELETE FROM specify_sync_log");
            handle.execute("DELETE FROM specify_ars_sync_batch");
        });
    }

    @Test
    void test() {
        jdbi.withHandle(x -> {
            SpecifyArsSyncBatch failed = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(24, ChronoUnit.HOURS), Instant.now(), SpecifyArsSyncBatchStatus.FAILED, "test");
            SpecifyArsSyncBatch succeeded = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(48, ChronoUnit.HOURS), Instant.now().minus(24, ChronoUnit.HOURS), SpecifyArsSyncBatchStatus.SUCCEEDED, "test2");
            SpecifyArsSyncRepository attach = x.attach(SpecifyArsSyncRepository.class);
            attach.createNewBatch(failed);
            attach.createNewBatch(succeeded);
            SpecifyArsSyncBatch latestNonFailed = attach.getLatestNonFailed();
            assertThat(latestNonFailed.additional_info()).isEqualTo(succeeded.additional_info());
            assertThat(latestNonFailed.status()).isEqualTo(SpecifyArsSyncBatchStatus.SUCCEEDED);
            return x;
        });
    }

    @Test
    void emptyBatchIsTreatedAsLatestNonFailed() {
        jdbi.withHandle(x -> {
            SpecifyArsSyncBatch failed = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(24, ChronoUnit.HOURS), Instant.now(), SpecifyArsSyncBatchStatus.FAILED, "failed");
            SpecifyArsSyncBatch empty = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(12, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS), SpecifyArsSyncBatchStatus.EMPTY, "empty");
            SpecifyArsSyncRepository attach = x.attach(SpecifyArsSyncRepository.class);
            attach.createNewBatch(failed);
            attach.createNewBatch(empty);
            SpecifyArsSyncBatch latestNonFailed = attach.getLatestNonFailed();
            assertThat(latestNonFailed.additional_info()).isEqualTo(empty.additional_info());
            assertThat(latestNonFailed.status()).isEqualTo(SpecifyArsSyncBatchStatus.EMPTY);
            return x;
        });
    }

    @Test
    void test2() {
        jdbi.withHandle(x -> {
            SpecifyArsSyncRepository repository = x.attach(SpecifyArsSyncRepository.class);
            SpecifyArsSyncBatch succeeded = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(48, ChronoUnit.HOURS), Instant.now().minus(24, ChronoUnit.HOURS), SpecifyArsSyncBatchStatus.SUCCEEDED, "test2");
            Integer id = repository.createNewBatch(succeeded);
            SpecifySyncLogEntry test1 = new SpecifySyncLogEntry(null, null, SpecifySyncStatus.FAILED, 1234L, "test1", null, id, null, SyncDirection.SPECIFY_TO_ARS);
            SpecifySyncLogEntry test2 = new SpecifySyncLogEntry(null, null, SpecifySyncStatus.STARTED, 1235L, "test2", null, id, null, SyncDirection.SPECIFY_TO_ARS);
            repository.insertSyncLog(test1);
            Long syncLogId = repository.insertSyncLog(test2);
            List<SpecifySyncLogEntry> syncLog = repository.getSyncLog(id);
            assertThat(syncLog.size()).isEqualTo(2);
            specifySyncService.handleAcknowledge(new SyncAcknowledge(SpecifySyncStatus.SUCCEEDED, syncLogId, "Great success", null));
            SpecifyArsSyncBatch latestNonFailed = repository.getLatestNonFailed();
            assertThat(latestNonFailed.status()).isEqualTo(SpecifyArsSyncBatchStatus.FAILED_ENTRIES);
            return x;
        });
    }

    @Test
    void testSuccess() {
        jdbi.withHandle(x -> {
            SpecifyArsSyncRepository repository = x.attach(SpecifyArsSyncRepository.class);
            SpecifyArsSyncBatch succeeded = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(48, ChronoUnit.HOURS), Instant.now().minus(24, ChronoUnit.HOURS), SpecifyArsSyncBatchStatus.SUCCEEDED, "test2");
            Integer id = repository.createNewBatch(succeeded);
            SpecifySyncLogEntry test1 = new SpecifySyncLogEntry(null, null, SpecifySyncStatus.SUCCEEDED, 1234L, "test1", null, id, null, SyncDirection.SPECIFY_TO_ARS);
            SpecifySyncLogEntry test2 = new SpecifySyncLogEntry(null, null, SpecifySyncStatus.STARTED, 1235L, "test2", null, id, null, SyncDirection.SPECIFY_TO_ARS);
            repository.insertSyncLog(test1);
            Long syncLogId = repository.insertSyncLog(test2);
            List<SpecifySyncLogEntry> syncLog = repository.getSyncLog(id);
            assertThat(syncLog.size()).isEqualTo(2);
            specifySyncService.handleAcknowledge(new SyncAcknowledge(SpecifySyncStatus.SUCCEEDED, syncLogId, "Great success", null));
            SpecifyArsSyncBatch latestNonFailed = repository.getLatestNonFailed();
            assertThat(latestNonFailed.status()).isEqualTo(SpecifyArsSyncBatchStatus.SUCCEEDED);
            return x;
        });
    }

    @Test
    void testUpdate() {
        Long syncLogId = null;
        jdbi.withHandle(x -> {
            SpecifyArsSyncRepository repository = x.attach(SpecifyArsSyncRepository.class);
            SpecifyArsSyncBatch succeeded = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(48, ChronoUnit.HOURS), Instant.now().minus(24, ChronoUnit.HOURS), SpecifyArsSyncBatchStatus.SUCCEEDED, "test2");
            Integer id = repository.createNewBatch(succeeded);
            SpecifySyncLogEntry test1 = new SpecifySyncLogEntry(null, null, SpecifySyncStatus.STARTED, 1234L, "test1", null, id, null, SyncDirection.SPECIFY_TO_ARS);
            SpecifySyncLogEntry test2 = new SpecifySyncLogEntry(null, null, SpecifySyncStatus.STARTED, 1235L, "testupdate", null, id, null, SyncDirection.SPECIFY_TO_ARS);
            repository.insertSyncLog(test1);
            Long i = repository.insertSyncLog(test2);
            List<SpecifySyncLogEntry> syncLog = repository.getSyncLog(id);
            assertThat(syncLog.size()).isEqualTo(2);
            return x;
        });
    }

}
