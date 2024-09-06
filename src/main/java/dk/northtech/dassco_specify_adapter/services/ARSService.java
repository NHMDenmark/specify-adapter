package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.configuration.AssetServiceConfig;
import jakarta.inject.Inject;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class ARSService {
    private static final Logger logger = LoggerFactory.getLogger(ARSService.class);
    private final KeycloakService keycloakService;
    private final AssetServiceConfig assetServiceConfig;

    @Inject
    public ARSService(KeycloakService keycloakService, AssetServiceConfig assetServiceConfig) {
        this.keycloakService = keycloakService;
        this.assetServiceConfig = assetServiceConfig;
    }

    public String getCredentials(String institutionName) {
        var token = this.keycloakService.getUserServiceToken();
        try {
            URIBuilder urlWithParams = new URIBuilder(assetServiceConfig.rootUrl() + "/api/v1/specify-users/")
                    .addParameter("institutionName", institutionName);

            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .uri(urlWithParams.build())
                    .GET().build();
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpResponse httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() > 199 && httpResponse.statusCode() < 300) {
                logger.info("Credentials for institution {} have been received", institutionName);
                return httpResponse.body().toString();
            } else {
                logger.warn("Failed to get credentials, request failed with status code: " + httpResponse.statusCode());
            }
        } catch (Exception e) {
            logger.error("Failed to get credentials for institution {}", institutionName);
            throw new RuntimeException("Failed to get credentials for institution " + institutionName, e);
        }
        return null;
    }
}
