package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.domain.InstitutionConfigCredentials;
import dk.northtech.dassco_specify_adapter.repository.CollectionConfigRepository;
import dk.northtech.dassco_specify_adapter.repository.InstitutionConfigRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class InstitutionConfigCredentialsService {
    private final Jdbi jdbi;
    private final InstitutionConfigPasswordService passwordService;

    @Inject
    public InstitutionConfigCredentialsService(Jdbi jdbi, InstitutionConfigPasswordService passwordService) {
        this.jdbi = jdbi;
        this.passwordService = passwordService;
    }

    public Optional<InstitutionConfigCredentials> getInstitutionConfigCredentials(Long id) {
        return jdbi.withHandle(handle -> {
            InstitutionConfigRepository institutionRepository = handle.attach(InstitutionConfigRepository.class);
            CollectionConfigRepository collectionRepository = handle.attach(CollectionConfigRepository.class);
            InstitutionConfigCredentials credentials = institutionRepository.getInstitutionConfigCredentials(id);
            if (credentials == null || credentials.specifyPassword() == null || credentials.specifyPassword().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new InstitutionConfigCredentials(
                    credentials.id(),
                    credentials.name(),
                    credentials.specifyRootUrl(),
                    credentials.specifyUsername(),
                    passwordService.decrypt(credentials.specifyPassword()),
                    collectionRepository.listCollectionConfigsByInstitutionId(credentials.id())
            ));
        });
    }

    public List<InstitutionConfigCredentials> listInstitutionConfigCredentials() {
        return jdbi.withHandle(handle -> {
            InstitutionConfigRepository institutionRepository = handle.attach(InstitutionConfigRepository.class);
            CollectionConfigRepository collectionRepository = handle.attach(CollectionConfigRepository.class);
            return institutionRepository.listInstitutionConfigs().stream()
                    .map(institutionConfig -> {
                        InstitutionConfigCredentials credentials = institutionRepository.getInstitutionConfigCredentials(institutionConfig.id());
                        return new InstitutionConfigCredentials(
                                credentials.id(),
                                credentials.name(),
                                credentials.specifyRootUrl(),
                                credentials.specifyUsername(),
                                credentials.specifyPassword() == null ? null : passwordService.decrypt(credentials.specifyPassword()),
                                collectionRepository.listCollectionConfigsByInstitutionId(credentials.id())
                        );
                    })
                    .toList();
        });
    }
}
