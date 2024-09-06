package dk.northtech.dassco_specify_adapter.domain;

import java.time.Instant;

public record KeycloakToken(
        String accessToken,
        long expiresIn,
        Instant accessExpirationTimeStamp,
        long refreshExpiresIn,
        Instant refreshExpirationTimeStamp,
        String tokenType,
        String refreshToken,
        String scope) {

}
