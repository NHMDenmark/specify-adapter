package dk.northtech.dassco_specify_adapter.domain;

public enum InternalRole {
    USER(SecurityRoles.USER)
    , ADMIN(SecurityRoles.ADMIN)
    , SERVICE_USER(SecurityRoles.SERVICE)
    , DEVELOPER(SecurityRoles.DEVELOPER);

    public final String roleName;
    InternalRole(String role) {
        this.roleName = role;
    }
}
