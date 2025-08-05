package dk.northtech.dassco_specify_adapter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.UrlEscapers;
import dk.northtech.dassco_specify_adapter.assets.FileProxyProperties;
import dk.northtech.dassco_specify_adapter.domain.AcknowledgeStatus;
import dk.northtech.dassco_specify_adapter.domain.SpecifyAdapterException;
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
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class AssetFileService {

    FileProxyProperties fileProxyProperties;
    private static final Logger logger = LoggerFactory.getLogger(AssetFileService.class);

    @Inject
    public AssetFileService(FileProxyProperties fileProxyProperties) {
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
            data = objectMapper.readValue(response.body(), new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() == 200) {
            if (!data.isEmpty()) {
                return data;
            } else {
                throw new SpecifyAdapterException("Asset does not have files in ERDA", AcknowledgeStatus.FILE_DOWNLOAD_ERROR);
            }
        } else {
            throw new RuntimeException("There was an error fetching the Data From Erda");
        }

    }

    public InputStream fetchFiles(String institution, String collection, String guid, String path, String token) {
        String escapedFilePath = UrlEscapers.urlFragmentEscaper().escape(institution + "/" + collection + "/" + guid + "/" + path);
//        try {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        fileProxyProperties.rootUrl() + "/file_proxy/api/files/assets/" + escapedFilePath + "?no-cache=true"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new SpecifyAdapterException("Failed to fetch files from file proxy", AcknowledgeStatus.FILE_DOWNLOAD_ERROR);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            logger.error("There was an error fetching file from file proxy, http code: " + response.statusCode());
            throw new SpecifyAdapterException("There was an error fetching " + path + " from ERDA", AcknowledgeStatus.FILE_DOWNLOAD_ERROR);
        }

//            } catch (Exception e){
//                throw new RuntimeException("There was an error with the API call to file_proxy.", e);
//            }
    }

    public int postFileToParkedFiles(InputStream file, String type, String collection, String filename, String pathPostFix){
        Supplier<InputStream> streamSupplier = () -> file;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(
                        URI.create(
                                fileProxyProperties.rootUrl()
                                        + "/file_proxy/api/assetfiles/parkedfiles/"
                                        + URLEncoder.encode(pathPostFix, StandardCharsets.UTF_8)
                                        + "/" + URLEncoder.encode(collection, StandardCharsets.UTF_8)
                                        + "/" + URLEncoder.encode(type, StandardCharsets.UTF_8)
                                        + "/" + URLEncoder.encode(filename, StandardCharsets.UTF_8)
                        )
                )
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofInputStream(streamSupplier))
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new SpecifyAdapterException("Failed to upload Parked file to file proxy ", AcknowledgeStatus.PARKED_FILE_UPLOAD_ERROR);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response.statusCode();
    }

    public InputStream readFileFromParkedFiles(String coll, String type, String filename, String pathPostFix, Integer scale){
        String updatedFileName =  Objects.equals(type, "T") ? filename.replace(".", "_%s.".formatted(scale)) : filename;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileProxyProperties.rootUrl()
                        + "/file_proxy/api/assetfiles/parkedfiles/"
                        + URLEncoder.encode(pathPostFix, StandardCharsets.UTF_8) + "/"
                        + URLEncoder.encode(coll, StandardCharsets.UTF_8) + "/"
                        + URLEncoder.encode(Objects.equals(type, "T") ? "thumbnails" : "originals", StandardCharsets.UTF_8) + "/"
                        + URLEncoder.encode(updatedFileName, StandardCharsets.UTF_8)
                        + "?scale=" + scale
                ))
                .GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new SpecifyAdapterException("Failed to fetch Parked file from file proxy ", AcknowledgeStatus.PARKED_FILE_DOWNLOAD_ERROR);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return response.body();


    }
}
