package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.UrlEscapers;
import dk.northtech.dassco_specify_adapter.assets.FileProxyProperties;
import dk.northtech.dassco_specify_adapter.domain.User;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AssetFileService {

    FileProxyProperties fileProxyProperties;
    private static final Logger logger = LoggerFactory.getLogger(AssetFileService.class);
    @Inject
    public AssetFileService(FileProxyProperties fileProxyProperties){
        this.fileProxyProperties = fileProxyProperties;
    }

    public List<String> getAssetFiles(String assetGuid, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileProxyProperties.rootUrl() + "/file_proxy/api/assetfiles/listfiles/" + assetGuid))
                .header("Authorization", "Bearer " + token)
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

    public InputStream fetchFiles(String institution, String collection, String guid, String path, String token) {
        String escapedFilePath = UrlEscapers.urlFragmentEscaper().escape(institution + "/" + collection + "/" + guid + "/" + path);
        try {
        HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            fileProxyProperties.rootUrl() + "/file_proxy/api/files/assets/" +escapedFilePath + "?no-cache=true"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpClient httpClient = HttpClient.newHttpClient();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200){
                    return response.body();
                } else {
                    logger.error("There was an error fetching file from file proxy, http code: " + response.statusCode());
                    throw new RuntimeException("There was an error fetching " + path + " from ERDA");
                }

            } catch (Exception e){
                throw new RuntimeException("There was an error with the API call to file_proxy.", e);
            }
    }
}
