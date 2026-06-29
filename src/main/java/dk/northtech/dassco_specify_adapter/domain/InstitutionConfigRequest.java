package dk.northtech.dassco_specify_adapter.domain;

public record InstitutionConfigRequest(String name,
                                       String specifyRootUrl,
                                       String specifyUsername,
                                       String specifyPassword) {
}
