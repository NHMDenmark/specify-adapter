package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.domain.User;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Map;

public class UserMapper {
    private static final String SPRING_ROLE_PREFIX = "ROLE_";
    public static User from(SecurityContext securityContext) {
        JwtAuthenticationToken token = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        Map<String, Object> tokenAttributes = token.getTokenAttributes();
        User user = new User();
//        if(securityContext.isUserInRole(SecurityRoles.ADMIN)) {
//            user.roles.add(SecurityRoles.ADMIN);
//        }
//        if(securityContext.isUserInRole(SecurityRoles.USER)) {
//            user.roles.add(SecurityRoles.USER);
//        }
//        if(securityContext.isUserInRole(SecurityRoles.DEVELOPER)) {
//            user.roles.add(SecurityRoles.DEVELOPER);
//        }
//        if(securityContext.isUserInRole(SecurityRoles.SERVICE)) {
//            user.roles.add(SecurityRoles.SERVICE);
//        }

        JwtAuthenticationToken userPrincipal = (JwtAuthenticationToken) securityContext.getUserPrincipal();
        Collection<GrantedAuthority> authorities = userPrincipal.getAuthorities();
        authorities.stream().map(x -> {
            String authority = x.getAuthority();
            if(authority.startsWith(SPRING_ROLE_PREFIX)){
                return authority.substring(SPRING_ROLE_PREFIX.length());
            }
            return authority;
        }).forEach(role -> user.roles.add(role));

        user.keycloakId = String.valueOf(tokenAttributes.get("sub"));
        user.username = String.valueOf(tokenAttributes.get("preferred_username"));
        user.token = token.getToken().getTokenValue();
        return user;
    }
}
