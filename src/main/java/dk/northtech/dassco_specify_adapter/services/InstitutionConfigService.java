package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.domain.ConfigurationConflictException;
import dk.northtech.dassco_specify_adapter.domain.InstitutionConfig;
import dk.northtech.dassco_specify_adapter.domain.InstitutionConfigCredentials;
import dk.northtech.dassco_specify_adapter.domain.InstitutionConfigRequest;
import dk.northtech.dassco_specify_adapter.repository.CollectionConfigRepository;
import dk.northtech.dassco_specify_adapter.repository.InstitutionConfigRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Service
public class InstitutionConfigService {
    private static final String UNIQUE_VIOLATION = "23505";

    private final Jdbi jdbi;
    private final InstitutionConfigPasswordService passwordService;

    @Inject
    public InstitutionConfigService(Jdbi jdbi, InstitutionConfigPasswordService passwordService) {
        this.jdbi = jdbi;
        this.passwordService = passwordService;
    }

    public List<InstitutionConfig> listInstitutionConfigs() {
        return jdbi.withHandle(handle -> {
            InstitutionConfigRepository institutionRepository = handle.attach(InstitutionConfigRepository.class);
            CollectionConfigRepository collectionRepository = handle.attach(CollectionConfigRepository.class);
            return institutionRepository.listInstitutionConfigs().stream()
                    .map(institutionConfig -> withCollections(institutionConfig, collectionRepository))
                    .toList();
        });
    }

    public Optional<InstitutionConfig> getInstitutionConfig(Long id) {
        return jdbi.withHandle(handle -> {
            InstitutionConfigRepository institutionRepository = handle.attach(InstitutionConfigRepository.class);
            CollectionConfigRepository collectionRepository = handle.attach(CollectionConfigRepository.class);
            InstitutionConfig institutionConfig = institutionRepository.getInstitutionConfig(id);
            return Optional.ofNullable(institutionConfig)
                    .map(config -> withCollections(config, collectionRepository));
        });
    }

    public InstitutionConfig createInstitutionConfig(InstitutionConfigRequest institutionConfigRequest) {
        InstitutionConfigRequest normalized = normalizeForCreate(institutionConfigRequest);
        try {
            return jdbi.inTransaction(handle -> {
                InstitutionConfigRepository repository = handle.attach(InstitutionConfigRepository.class);
                Long id = repository.createInstitutionConfig(
                        normalized.name(),
                        normalized.specifyRootUrl(),
                        normalized.specifyUsername(),
                        passwordService.encrypt(normalized.specifyPassword())
                );
                return withCollections(repository.getInstitutionConfig(id), handle.attach(CollectionConfigRepository.class));
            });
        } catch (UnableToExecuteStatementException exception) {
            if (hasSqlState(exception, UNIQUE_VIOLATION)) {
                throw new ConfigurationConflictException("Institution config with name '" + normalized.name() + "' already exists");
            }
            throw exception;
        }
    }

    public Optional<InstitutionConfig> updateInstitutionConfig(Long id, InstitutionConfigRequest institutionConfigRequest) {
        InstitutionConfigRequest normalized = normalizeForUpdate(institutionConfigRequest);
        try {
            return jdbi.inTransaction(handle -> {
                InstitutionConfigRepository repository = handle.attach(InstitutionConfigRepository.class);
                InstitutionConfigCredentials existingCredentials = repository.getInstitutionConfigCredentials(id);
                if (existingCredentials == null) {
                    return Optional.empty();
                }
                String encryptedPassword = existingCredentials.specifyPassword();
                if (normalized.specifyPassword() != null) {
                    encryptedPassword = passwordService.encrypt(normalized.specifyPassword());
                }
                int updated = repository.updateInstitutionConfig(
                        id,
                        normalized.name(),
                        normalized.specifyRootUrl(),
                        normalized.specifyUsername(),
                        encryptedPassword
                );
                if (updated == 0) {
                    return Optional.empty();
                }
                return Optional.of(withCollections(repository.getInstitutionConfig(id), handle.attach(CollectionConfigRepository.class)));
            });
        } catch (UnableToExecuteStatementException exception) {
            if (hasSqlState(exception, UNIQUE_VIOLATION)) {
                throw new ConfigurationConflictException("Institution config with name '" + normalized.name() + "' already exists");
            }
            throw exception;
        }
    }

    public boolean deleteInstitutionConfig(Long id) {
        return jdbi.inTransaction(handle -> {
            InstitutionConfigRepository repository = handle.attach(InstitutionConfigRepository.class);
            if (repository.countCollections(id) > 0) {
                throw new ConfigurationConflictException("Institution config " + id + " cannot be deleted while collection configs still exist");
            }
            return repository.deleteInstitutionConfig(id) > 0;
        });
    }

    private InstitutionConfigRequest normalizeForCreate(InstitutionConfigRequest institutionConfigRequest) {
        return new InstitutionConfigRequest(
                requireText(institutionConfigRequest.name(), "name"),
                requireText(institutionConfigRequest.specifyRootUrl(), "specifyRootUrl"),
                requireText(institutionConfigRequest.specifyUsername(), "specifyUsername"),
                requirePassword(institutionConfigRequest.specifyPassword(), true)
        );
    }

    private InstitutionConfigRequest normalizeForUpdate(InstitutionConfigRequest institutionConfigRequest) {
        return new InstitutionConfigRequest(
                requireText(institutionConfigRequest.name(), "name"),
                requireText(institutionConfigRequest.specifyRootUrl(), "specifyRootUrl"),
                requireText(institutionConfigRequest.specifyUsername(), "specifyUsername"),
                requirePassword(institutionConfigRequest.specifyPassword(), false)
        );
    }

    private InstitutionConfig withCollections(InstitutionConfig institutionConfig, CollectionConfigRepository collectionRepository) {
        return new InstitutionConfig(
                institutionConfig.id(),
                institutionConfig.name(),
                institutionConfig.specifyRootUrl(),
                institutionConfig.specifyUsername(),
                collectionRepository.listCollectionConfigsByInstitutionId(institutionConfig.id())
        );
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String requirePassword(String value, boolean required) {
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("specifyPassword is required");
            }
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("specifyPassword must not be blank");
        }
        return value;
    }

    private boolean hasSqlState(Throwable throwable, String sqlState) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
