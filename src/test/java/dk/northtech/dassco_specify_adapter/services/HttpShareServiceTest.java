package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyArsSyncBatch;
import dk.northtech.dassco_specify_adapter.domain.sync.SpecifyArsSyncBatchStatus;
import dk.northtech.dassco_specify_adapter.repository.SpecifyArsSyncRepository;
import jakarta.inject.Inject;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
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

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest
@Testcontainers
@DirtiesContext
class HttpShareServiceTest {

    @Inject
    LogService logService;

    @Inject Jdbi jdbi;
    private static final Logger logger = LoggerFactory.getLogger(HttpShareServiceTest.class);
    private static Network network = Network.newNetwork();
    @Container
    static GenericContainer postgreSQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName( "dassco_file_proxy")
            .withPassword("dassco_file_proxy")
            .withUsername("dassco_file_proxy")
            .withExposedPorts(5432)
//            .waitingFor(Wait.forLogMessage("ready to accept connections",1))
            .withNetwork(network).withNetworkAliases("database");

//
    static {
            postgreSQL.start();
//        String url =  "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_file_proxy";
//        Properties props = new Properties();
//        props.setProperty("user", "fred");
//        props.setProperty("password", "secret");
//        props.setProperty("ssl", "true");
//        Connection conn = DriverManager.getConnection(url, props);

        String url =  "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_file_proxy?user=dassco_file_proxy&password=dassco_file_proxy";
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
        registry.add("datasource.jdbcUrl", () -> "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_file_proxy");
    }

    @Test
    void test() {
        jdbi.withHandle(x -> {
            SpecifyArsSyncBatch failed = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(24, ChronoUnit.HOURS), Instant.now(), SpecifyArsSyncBatchStatus.FAILED, "test");
            SpecifyArsSyncBatch succeeded = new SpecifyArsSyncBatch(null, Instant.now(), Instant.now().minus(48, ChronoUnit.HOURS), Instant.now().minus(24, ChronoUnit.HOURS), SpecifyArsSyncBatchStatus.SUCCESSFUL, "test2");
            SpecifyArsSyncRepository attach = x.attach(SpecifyArsSyncRepository.class);
            attach.createNewBatch(failed);
            attach.createNewBatch(succeeded);
            SpecifyArsSyncBatch latestNonFailed = attach.getLatestNonFailed();
            assertThat(latestNonFailed.additional_info()).isEqualTo(succeeded.additional_info());

            return x;
        });
    }
    @Test
    void contextLoads() {
    }
}
