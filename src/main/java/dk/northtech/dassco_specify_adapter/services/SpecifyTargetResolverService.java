package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.domain.*;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SpecifyTargetResolverService {
    private final InstitutionConfigCredentialsService institutionConfigCredentialsService;
    private final CollectionConfigService collectionConfigService;

    @Inject
    public SpecifyTargetResolverService(InstitutionConfigCredentialsService institutionConfigCredentialsService,
                                        CollectionConfigService collectionConfigService) {
        this.institutionConfigCredentialsService = institutionConfigCredentialsService;
        this.collectionConfigService = collectionConfigService;
    }

    public ResolvedSpecifyTarget resolveForAsset(Asset asset) {
        if (asset.institution == null || asset.institution.isBlank()) {
            throw new SpecifyAdapterException("Asset institution is required to resolve Specify configuration", AcknowledgeStatus.MAPPING_ERROR);
        }
        if (asset.collection == null || asset.collection.isBlank()) {
            throw new SpecifyAdapterException("Asset collection is required to resolve Specify configuration", AcknowledgeStatus.MAPPING_ERROR);
        }

        InstitutionConfigCredentials institutionConfig = institutionConfigCredentialsService.listInstitutionConfigCredentials().stream()
                .filter(config -> config.name().equalsIgnoreCase(asset.institution.trim()))
                .findFirst()
                .orElseThrow(() -> new SpecifyAdapterException("No institution config found for asset institution '" + asset.institution + "'", AcknowledgeStatus.MAPPING_ERROR));
        if (institutionConfig.specifyPassword() == null || institutionConfig.specifyPassword().isBlank()) {
            throw new SpecifyAdapterException("Institution config '" + institutionConfig.name() + "' does not have a Specify password", AcknowledgeStatus.MAPPING_ERROR);
        }

        CollectionConfig collectionConfig = institutionConfig.collectionConfigs().stream()
                .filter(config -> config.name().equalsIgnoreCase(asset.collection.trim()))
                .findFirst()
                .orElseThrow(() -> new SpecifyAdapterException("No collection config found for asset collection '" + asset.collection + "' in institution '" + asset.institution + "'", AcknowledgeStatus.MAPPING_ERROR));
        if (!Boolean.TRUE.equals(collectionConfig.syncToSpecifyEnabled())) {
            throw new SpecifyAdapterException("Collection config '" + collectionConfig.name() + "' is not enabled for ARS to Specify sync", AcknowledgeStatus.MAPPING_ERROR);
        }

        return new ResolvedSpecifyTarget(institutionConfig, collectionConfig);
    }

    public List<ResolvedSpecifyTarget> listSpecifyToArsTargets() {
        return institutionConfigCredentialsService.listInstitutionConfigCredentials().stream()
                .flatMap(institutionConfig -> institutionConfig.collectionConfigs().stream()
                        .filter(collectionConfig -> Boolean.TRUE.equals(collectionConfig.syncFromSpecifyEnabled()))
                        .map(collectionConfig -> new ResolvedSpecifyTarget(institutionConfig, collectionConfig)))
                .toList();
    }

    public Optional<ResolvedSpecifyTarget> getTargetByCollectionConfigId(Long collectionConfigId) {
        Optional<CollectionConfig> collectionConfig = collectionConfigService.getCollectionConfig(collectionConfigId);
        if (collectionConfig.isEmpty()) {
            return Optional.empty();
        }
        return institutionConfigCredentialsService.getInstitutionConfigCredentials(collectionConfig.get().institutionId())
                .map(institutionConfig -> new ResolvedSpecifyTarget(institutionConfig, collectionConfig.get()));
    }
}
