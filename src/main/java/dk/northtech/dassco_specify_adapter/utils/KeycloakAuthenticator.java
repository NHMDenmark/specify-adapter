package dk.northtech.dassco_specify_adapter.utils;

import dk.northtech.dassco_specify_adapter.configuration.KeycloakUserConfig;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

@Service
public class KeycloakAuthenticator extends Authenticator {
    KeycloakUserConfig keycloakUserConfig;

    @Inject
    public KeycloakAuthenticator(KeycloakUserConfig keycloakUserConfig) {
        this.keycloakUserConfig = keycloakUserConfig;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(
                keycloakUserConfig.username(),
                keycloakUserConfig.password().toCharArray());
    }
}
