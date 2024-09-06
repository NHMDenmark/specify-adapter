package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.northtech.dassco_specify_adapter.assets.FileProxyProperties;
import dk.northtech.dassco_specify_adapter.domain.User;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class AssetFileService {

    FileProxyProperties fileProxyProperties;

    @Inject
    public AssetFileService(FileProxyProperties fileProxyProperties){
        this.fileProxyProperties = fileProxyProperties;
    }

    public List<String> getAssetFiles(String assetGuid, User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileProxyProperties.rootUrl() + "/file_proxy/api/assetfiles/listfiles/" + assetGuid))
                .header("Authorization", "Bearer " + user.token)
                .GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> data = null;

        try {
            data = objectMapper.readValue(response.body(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() == 200){
            if (!data.isEmpty()){
                return data;
            } else {
                throw new RuntimeException("Asset does not have files in ERDA");
            }
        } else {
            throw new RuntimeException("There was an error fetching the Data From Erda");
        }

    }

    public InputStream fetchFiles(String institution, String collection, String guid, String path, User user) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fileProxyProperties.rootUrl() + "/file_proxy/api/files/assets/" + institution + "/" + collection + "/" + guid + "/" + path + "?no-cache=true"))
                    .header("Authorization", "Bearer " + user.token)
                    .GET()
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();
            System.out.println(request);

            try {
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                System.out.println(response.body());
                System.out.println(response.statusCode());

                if (response.statusCode() == 200){
                    return response.body();
                } else {
                    throw new RuntimeException("There was an error fetching " + path + " from ERDA");
                }

            } catch (Exception e){
                throw new RuntimeException("There was an error with the API call to file_proxy.");
            }
    }
}
