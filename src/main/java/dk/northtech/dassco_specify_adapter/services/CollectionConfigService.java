package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.domain.CollectionConfig;
import dk.northtech.dassco_specify_adapter.domain.ConfigurationConflictException;
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
public class CollectionConfigService {
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";

    private final Jdbi jdbi;

    @Inject
    public CollectionConfigService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<CollectionConfig> listCollectionConfigs(Long institutionId) {
        return jdbi.withHandle(handle -> handle.attach(CollectionConfigRepository.class).listCollectionConfigsByInstitutionId(institutionId));
    }

    public Optional<CollectionConfig> getCollectionConfig(Long id) {
        return jdbi.withHandle(handle -> Optional.ofNullable(handle.attach(CollectionConfigRepository.class).getCollectionConfig(id)));
    }

    public Optional<CollectionConfig> createCollectionConfig(Long institutionId, CollectionConfig collectionConfig) {
        CollectionConfig normalized = normalizeForCreate(institutionId, collectionConfig);
        try {
            return jdbi.inTransaction(handle -> {
                InstitutionConfigRepository institutionRepository = handle.attach(InstitutionConfigRepository.class);
                if (institutionRepository.getInstitutionConfig(institutionId) == null) {
                    return Optional.empty();
                }
                CollectionConfigRepository collectionRepository = handle.attach(CollectionConfigRepository.class);
                Long id = collectionRepository.createCollectionConfig(normalized);
                return Optional.ofNullable(collectionRepository.getCollectionConfig(id));
            });
        } catch (UnableToExecuteStatementException exception) {
            throw translateWriteException(normalized, exception);
        }
    }

    public Optional<CollectionConfig> updateCollectionConfig(Long id, CollectionConfig collectionConfig) {
        CollectionConfig normalized = normalizeForUpdate(id, collectionConfig);
        try {
            return jdbi.inTransaction(handle -> {
                CollectionConfigRepository collectionRepository = handle.attach(CollectionConfigRepository.class);
                if (collectionRepository.getCollectionConfig(id) == null) {
                    return Optional.empty();
                }
                InstitutionConfigRepository institutionRepository = handle.attach(InstitutionConfigRepository.class);
                if (institutionRepository.getInstitutionConfig(normalized.institutionId()) == null) {
                    throw new IllegalArgumentException("institutionId does not reference an existing institution config");
                }
                collectionRepository.updateCollectionConfig(normalized);
                return Optional.ofNullable(collectionRepository.getCollectionConfig(id));
            });
        } catch (UnableToExecuteStatementException exception) {
            throw translateWriteException(normalized, exception);
        }
    }

    public boolean deleteCollectionConfig(Long id) {
        return jdbi.withHandle(handle -> handle.attach(CollectionConfigRepository.class).deleteCollectionConfig(id) > 0);
    }

    private CollectionConfig normalizeForCreate(Long institutionId, CollectionConfig collectionConfig) {
        return new CollectionConfig(null,
                institutionId,
                requireText(collectionConfig.name(), "name"),
                normalizeDescription(collectionConfig.description()),
                booleanValue(collectionConfig.syncToSpecifyEnabled()),
                booleanValue(collectionConfig.syncFromSpecifyEnabled()));
    }

    private CollectionConfig normalizeForUpdate(Long id, CollectionConfig collectionConfig) {
        if (collectionConfig.institutionId() == null) {
            throw new IllegalArgumentException("institutionId is required");
        }
        return new CollectionConfig(id,
                collectionConfig.institutionId(),
                requireText(collectionConfig.name(), "name"),
                normalizeDescription(collectionConfig.description()),
                booleanValue(collectionConfig.syncToSpecifyEnabled()),
                booleanValue(collectionConfig.syncFromSpecifyEnabled()));
    }

    private RuntimeException translateWriteException(CollectionConfig collectionConfig, UnableToExecuteStatementException exception) {
        if (hasSqlState(exception, UNIQUE_VIOLATION)) {
            throw new ConfigurationConflictException("Collection config with name '" + collectionConfig.name() + "' already exists for institution " + collectionConfig.institutionId());
        }
        if (hasSqlState(exception, FOREIGN_KEY_VIOLATION)) {
            throw new IllegalArgumentException("institutionId does not reference an existing institution config");
        }
        return exception;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private boolean booleanValue(Boolean value) {
        return Boolean.TRUE.equals(value);
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
