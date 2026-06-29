package dk.northtech.dassco_specify_adapter.domain;

public record InstitutionConfigRequest(String name,
                                       String specifyRootUrl,
                                       String specifyAssetServerUrl,
                                       String specifyUsername,
                                       String specifyPassword) {
}
