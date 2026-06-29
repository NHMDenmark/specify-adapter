package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.domain.*;
import dk.northtech.dassco_specify_adapter.repository.CollectionConfigRepository;
import dk.northtech.dassco_specify_adapter.repository.InstitutionConfigRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

@Service
public class SpecifyTargetResolverService {
    private final Jdbi jdbi;
    private final InstitutionConfigPasswordService passwordService;

    @Inject
    public SpecifyTargetResolverService(Jdbi jdbi, InstitutionConfigPasswordService passwordService) {
        this.jdbi = jdbi;
        this.passwordService = passwordService;
    }

    public ResolvedSpecifyTarget resolveForAsset(Asset asset) {
        if (asset.institution == null || asset.institution.isBlank()) {
            throw new SpecifyAdapterException("Asset institution is required to resolve Specify configuration", AcknowledgeStatus.MAPPING_ERROR);
        }
        if (asset.collection == null || asset.collection.isBlank()) {
            throw new SpecifyAdapterException("Asset collection is required to resolve Specify configuration", AcknowledgeStatus.MAPPING_ERROR);
        }

        return jdbi.withHandle(handle -> {
            InstitutionConfigRepository institutionRepository = handle.attach(InstitutionConfigRepository.class);
            CollectionConfigRepository collectionRepository = handle.attach(CollectionConfigRepository.class);

            InstitutionConfigCredentials institutionConfig = institutionRepository.getInstitutionConfigCredentialsByName(asset.institution.trim());
            if (institutionConfig == null) {
                throw new SpecifyAdapterException("No institution config found for asset institution '" + asset.institution + "'", AcknowledgeStatus.MAPPING_ERROR);
            }
            if (institutionConfig.specifyPassword() == null || institutionConfig.specifyPassword().isBlank()) {
                throw new SpecifyAdapterException("Institution config '" + institutionConfig.name() + "' does not have a Specify password", AcknowledgeStatus.MAPPING_ERROR);
            }

            CollectionConfig collectionConfig = collectionRepository.getCollectionConfigByInstitutionIdAndName(institutionConfig.id(), asset.collection.trim());
            if (collectionConfig == null) {
                throw new SpecifyAdapterException("No collection config found for asset collection '" + asset.collection + "' in institution '" + asset.institution + "'", AcknowledgeStatus.MAPPING_ERROR);
            }
            if (!Boolean.TRUE.equals(collectionConfig.syncToSpecifyEnabled())) {
                throw new SpecifyAdapterException("Collection config '" + collectionConfig.name() + "' is not enabled for ARS to Specify sync", AcknowledgeStatus.MAPPING_ERROR);
            }

            return new ResolvedSpecifyTarget(
                    new InstitutionConfigCredentials(
                            institutionConfig.id(),
                            institutionConfig.name(),
                            institutionConfig.specifyRootUrl(),
                            institutionConfig.specifyAssetServerUrl(),
                            institutionConfig.specifyUsername(),
                            passwordService.decrypt(institutionConfig.specifyPassword()),
                            collectionRepository.listCollectionConfigsByInstitutionId(institutionConfig.id())
                    ),
                    collectionConfig
            );
        });
    }
}
