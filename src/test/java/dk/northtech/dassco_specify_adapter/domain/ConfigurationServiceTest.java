package dk.northtech.dassco_specify_adapter.domain;

import dk.northtech.dassco_specify_adapter.services.CollectionConfigService;
import dk.northtech.dassco_specify_adapter.services.InstitutionConfigCredentialsService;
import dk.northtech.dassco_specify_adapter.services.InstitutionConfigService;
import dk.northtech.dassco_specify_adapter.services.SpecifyTargetResolverService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("tests")
@Testcontainers
@DirtiesContext
class ConfigurationServiceTest {
    @Inject
    InstitutionConfigService institutionConfigService;
    @Inject
    CollectionConfigService collectionConfigService;
    @Inject
    InstitutionConfigCredentialsService institutionConfigCredentialsService;
    @Inject
    SpecifyTargetResolverService specifyTargetResolverService;
    @Inject
    Jdbi jdbi;

    private static final Network network = Network.newNetwork();

    @Container
    static GenericContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("dassco_bridge")
            .withPassword("dassco_bridge")
            .withUsername("dassco_bridge")
            .withExposedPorts(5432)
            .withNetwork(network)
            .withNetworkAliases("database");

    static {
        postgreSQL.start();
        String url = "jdbc:postgresql://localhost:" + postgreSQL.getFirstMappedPort() + "/dassco_bridge?user=dassco_bridge&password=dassco_bridge";
        try (Connection conn = DriverManager.getConnection(url)) {
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
            Liquibase liquibase = new Liquibase("/liquibase/changelog-master.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts("development", "default"));
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
            handle.execute("DELETE FROM collection_config");
            handle.execute("DELETE FROM institution_config");
        });
    }

    @Test
    void institutionConfigCrudFlowWorks() {
        InstitutionConfig created = institutionConfigService.createInstitutionConfig(new InstitutionConfigRequest(
                "NHMD",
                "https://specify.example",
                "https://assets.example",
                "specify-user",
                "specify-password"
        ));

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("NHMD");

        List<InstitutionConfig> all = institutionConfigService.listInstitutionConfigs();
        assertThat(all).hasSize(1);

        String encryptedPassword = jdbi.withHandle(handle -> handle.createQuery("SELECT specify_password_encrypted FROM institution_config WHERE id = :id")
                .bind("id", created.id())
                .mapTo(String.class)
                .one());
        assertThat(encryptedPassword).isNotEqualTo("specify-password");

        Optional<InstitutionConfig> updated = institutionConfigService.updateInstitutionConfig(created.id(), new InstitutionConfigRequest(
                "NHMD Updated",
                "https://specify-2.example",
                "https://assets-2.example",
                "specify-user-2",
                null
        ));

        assertThat(updated.isPresent()).isTrue();
        assertThat(updated.orElseThrow().name()).isEqualTo("NHMD Updated");
        assertThat(updated.orElseThrow().specifyRootUrl()).isEqualTo("https://specify-2.example");
        assertThat(updated.orElseThrow().collectionConfigs()).isEmpty();
        InstitutionConfigCredentials credentials = institutionConfigCredentialsService.getInstitutionConfigCredentials(created.id()).orElseThrow();
        assertThat(credentials.specifyPassword()).isEqualTo("specify-password");

        boolean deleted = institutionConfigService.deleteInstitutionConfig(created.id());
        assertThat(deleted).isTrue();
        assertThat(institutionConfigService.getInstitutionConfig(created.id()).isEmpty()).isTrue();
    }

    @Test
    void collectionConfigCrudFlowWorks() {
        InstitutionConfig institution = institutionConfigService.createInstitutionConfig(new InstitutionConfigRequest(
                "SNM",
                "https://specify.example",
                "https://assets.example",
                "specify-user",
                "specify-password"
        ));

        Optional<CollectionConfig> created = collectionConfigService.createCollectionConfig(institution.id(), new CollectionConfig(
                null,
                null,
                "Entomology",
                "Insect collection",
                true,
                false
        ));

        assertThat(created.isPresent()).isTrue();
        assertThat(created.orElseThrow().institutionId()).isEqualTo(institution.id());

        InstitutionConfig hydratedInstitution = institutionConfigService.getInstitutionConfig(institution.id()).orElseThrow();
        assertThat(hydratedInstitution.collectionConfigs()).hasSize(1);
        assertThat(hydratedInstitution.collectionConfigs().getFirst().name()).isEqualTo("Entomology");

        List<CollectionConfig> all = collectionConfigService.listCollectionConfigs(institution.id());
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().name()).isEqualTo("Entomology");

        Optional<CollectionConfig> updated = collectionConfigService.updateCollectionConfig(created.orElseThrow().id(), new CollectionConfig(
                null,
                institution.id(),
                "Entomology Updated",
                "Updated description",
                false,
                true
        ));

        assertThat(updated.isPresent()).isTrue();
        assertThat(updated.orElseThrow().name()).isEqualTo("Entomology Updated");
        assertThat(updated.orElseThrow().syncFromSpecifyEnabled()).isTrue();

        boolean deleted = collectionConfigService.deleteCollectionConfig(created.orElseThrow().id());
        assertThat(deleted).isTrue();
        assertThat(collectionConfigService.getCollectionConfig(created.orElseThrow().id()).isEmpty()).isTrue();
    }

    @Test
    void duplicateNamesAndInstitutionDeleteConflictAreRejected() {
        InstitutionConfig institution = institutionConfigService.createInstitutionConfig(new InstitutionConfigRequest(
                "NHMA",
                "https://specify.example",
                "https://assets.example",
                "specify-user",
                "specify-password"
        ));

        assertThrows(ConfigurationConflictException.class, () -> institutionConfigService.createInstitutionConfig(new InstitutionConfigRequest(
                "NHMA",
                "https://other-specify.example",
                "https://other-assets.example",
                "other-user",
                "other-password"
        )));

        collectionConfigService.createCollectionConfig(institution.id(), new CollectionConfig(
                null,
                null,
                "Mammals",
                null,
                null,
                null
        ));

        assertThrows(ConfigurationConflictException.class, () -> collectionConfigService.createCollectionConfig(institution.id(), new CollectionConfig(
                null,
                null,
                "Mammals",
                null,
                null,
                null
        )));

        assertThrows(ConfigurationConflictException.class, () -> institutionConfigService.deleteInstitutionConfig(institution.id()));
    }

    @Test
    void blankPasswordIsRejectedAndOmittedPasswordPreservesStoredValue() {
        InstitutionConfig institution = institutionConfigService.createInstitutionConfig(new InstitutionConfigRequest(
                "ZMUC",
                "https://specify.example",
                "https://assets.example",
                "specify-user",
                "initial-password"
        ));

        assertThrows(IllegalArgumentException.class, () -> institutionConfigService.updateInstitutionConfig(institution.id(), new InstitutionConfigRequest(
                "ZMUC",
                "https://specify.example",
                "https://assets.example",
                "specify-user",
                "   "
        )));

        institutionConfigService.updateInstitutionConfig(institution.id(), new InstitutionConfigRequest(
                "ZMUC Updated",
                "https://specify.example",
                "https://assets.example",
                "specify-user",
                null
        ));

        InstitutionConfigCredentials credentials = institutionConfigCredentialsService.getInstitutionConfigCredentials(institution.id()).orElseThrow();
        assertThat(credentials.specifyPassword()).isEqualTo("initial-password");
    }

    @Test
    void arsToSpecifyTargetIsResolvedFromDatabaseConfigs() {
        InstitutionConfig institution = institutionConfigService.createInstitutionConfig(new InstitutionConfigRequest(
                "NHMD",
                "https://specify.example",
                "https://assets.example",
                "specify-user",
                "specify-password"
        ));

        collectionConfigService.createCollectionConfig(institution.id(), new CollectionConfig(
                null,
                null,
                "Botany",
                null,
                true,
                false
        ));

        Asset asset = new Asset();
        asset.institution = "NHMD";
        asset.collection = "Botany";

        ResolvedSpecifyTarget target = specifyTargetResolverService.resolveForAsset(asset);
        assertThat(target.institutionConfig().id()).isEqualTo(institution.id());
        assertThat(target.institutionConfig().specifyPassword()).isEqualTo("specify-password");
        assertThat(target.collectionConfig().name()).isEqualTo("Botany");
    }
}
