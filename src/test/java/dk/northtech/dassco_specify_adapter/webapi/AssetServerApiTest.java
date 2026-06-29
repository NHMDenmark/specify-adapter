package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.assets.FileProxyProperties;
import dk.northtech.dassco_specify_adapter.configuration.ServerConfig;
import dk.northtech.dassco_specify_adapter.configuration.SpecifyWebAssetServiceConfig;
import dk.northtech.dassco_specify_adapter.services.AssetFileService;
import dk.northtech.dassco_specify_adapter.services.TokenService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.time.Instant;

import static com.google.common.truth.Truth.assertThat;

class AssetServerApiTest {
    private static final SpecifyWebAssetServiceConfig CONFIG = new SpecifyWebAssetServiceConfig(
            "true",
            "specify-001",
            "test_attachment_key",
            150,
            false,
            "NHMD"
    );

    private final TokenService tokenService = new TokenService(CONFIG);

    @Test
    void testKeyResponseIncludesSecondBasedTimestampHeader() {
        AssetServerApi assetServerApi = new AssetServerApi(
                CONFIG,
                null,
                null,
                tokenService,
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        String token = tokenService.generateToken(String.valueOf(Instant.now().getEpochSecond()), "random");
        Response response = assetServerApi.testTokenWorks(token, "random");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("X-Timestamp")).matches("\\d{10}");
    }

    @Test
    void fileDeleteResponseIncludesSecondBasedTimestampHeader() {
        AssetFileService assetFileService = new AssetFileService(new FileProxyProperties("http://localhost:8080"), CONFIG, null) {
            @Override
            public int deleteFileFromParkedFiles(String coll, String filename, String pathPostFix) {
                return 200;
            }
        };

        AssetServerApi assetServerApi = new AssetServerApi(
                CONFIG,
                null,
                assetFileService,
                tokenService,
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        String token = tokenService.generateToken(String.valueOf(Instant.now().getEpochSecond()), "file.jpg");
        Response response = assetServerApi.deleteFile("NHMD", "file.jpg", token);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("X-Timestamp")).matches("\\d{10}");
        assertThat(response.getEntity()).isEqualTo("Ok.");
    }

    @Test
    void fileDeleteReturnsBadRequestWhenFilenameMissing() {
        AssetServerApi assetServerApi = new AssetServerApi(
                CONFIG,
                null,
                null,
                tokenService,
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        Response response = assetServerApi.deleteFile("NHMD", "", "ignored");

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getHeaderString("X-Timestamp")).matches("\\d{10}");
        assertThat(response.getEntity()).isEqualTo("Missing required form field: filename");
    }

    @Test
    void fileDeleteReturnsMessageForUnexpectedStatus() {
        AssetFileService assetFileService = new AssetFileService(new FileProxyProperties("http://localhost:8080"), CONFIG, null) {
            @Override
            public int deleteFileFromParkedFiles(String coll, String filename, String pathPostFix) {
                return 500;
            }
        };

        AssetServerApi assetServerApi = new AssetServerApi(
                CONFIG,
                null,
                assetFileService,
                tokenService,
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        String token = tokenService.generateToken(String.valueOf(Instant.now().getEpochSecond()), "file.jpg");
        Response response = assetServerApi.deleteFile("NHMD", "file.jpg", token);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getEntity()).isEqualTo("Deletion failed with status: 500");
    }

    @Test
    void webAssetStoreResponseIncludesSecondBasedTimestampHeader() {
        AssetServerApi assetServerApi = new AssetServerApi(
                CONFIG,
                null,
                null,
                tokenService,
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        Response response = assetServerApi.serveXmlDescriptionOfUrlsAvailable();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("X-Timestamp")).matches("\\d{10}");
        assertThat((String) response.getEntity()).contains("http://localhost:8081/fileupload");
    }
}
