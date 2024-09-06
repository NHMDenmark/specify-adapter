package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dk.northtech.dassco_specify_adapter.configuration.AssetServiceConfig;
import dk.northtech.dassco_specify_adapter.domain.Asset;
import dk.northtech.dassco_specify_adapter.domain.User;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AssetService {

    AssetServiceConfig assetServiceProperties;

    @Inject
    public AssetService(AssetServiceConfig assetServiceProperties) {
        this.assetServiceProperties = assetServiceProperties;
    }

    public Asset getAsset(String assetGuid, User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(assetServiceProperties.rootUrl() + "/api/v1/assetmetadata/" + assetGuid))
                .header("Authorization", "Bearer " + user.token)
                .GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                try {
                    return objectMapper.readValue(response.body(), Asset.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (response.statusCode() == 403) {
                throw new RuntimeException("Forbidden.");
            } else {
                throw new RuntimeException("An error ocurred. Status Code: " + response.statusCode() + ", Body: " + response.body());
            }

            return new Asset();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getInstitutionMapping(String institution, User user) {
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(assetServiceProperties.rootUrl() + "/api/v1/mappings/institutions/" + institution))
                .header("Authorization", "Bearer " + user.token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else if (response.statusCode() == 403) {
                throw new RuntimeException("Forbidden.");
            } else if (response.statusCode() == 500) {
                throw new RuntimeException(response.body());
            } else {
                throw new RuntimeException("There was an error. Status " + response.statusCode() + ". Error: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCollectionMapping(String collection, User user) {
        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(assetServiceProperties.rootUrl() + "/api/v1/mappings/collections/" + collection))
                .header("Authorization", "Bearer " + user.token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else if (response.statusCode() == 403) {
                throw new RuntimeException("Forbidden");
            } else if (response.statusCode() == 500) {
                throw new RuntimeException(response.body());
            } else {
                throw new RuntimeException("There was an error. Code: " + response.statusCode() + ". Error: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void convertAssetToAttachment() {
        System.out.println("heey");
    }
}