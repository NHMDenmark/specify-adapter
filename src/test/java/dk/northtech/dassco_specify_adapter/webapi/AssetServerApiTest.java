package dk.northtech.dassco_specify_adapter.webapi;

import dk.northtech.dassco_specify_adapter.assets.FileProxyProperties;
import dk.northtech.dassco_specify_adapter.configuration.ServerConfig;
import dk.northtech.dassco_specify_adapter.configuration.SpecifyWebAssetServiceConfig;
import dk.northtech.dassco_specify_adapter.services.AssetFileService;
import dk.northtech.dassco_specify_adapter.services.TokenService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.ServerProperties;

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

    @Test
    void testKeyResponseIncludesSecondBasedTimestampHeader() {
        AssetServerApi assetServerApi = new AssetServerApi(
                CONFIG,
                null,
                null,
                new TokenService(CONFIG),
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        String token = new TokenService(CONFIG).generateToken(String.valueOf(java.time.Instant.now().getEpochSecond()), "random");
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
                new TokenService(CONFIG),
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        Response response = assetServerApi.deleteFile("NHMD", "file.jpg");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("X-Timestamp")).matches("\\d{10}");
        assertThat(response.getEntity()).isEqualTo("Ok.");
    }

    @Test
    void webAssetStoreResponseIncludesSecondBasedTimestampHeader() {
        AssetServerApi assetServerApi = new AssetServerApi(
                CONFIG,
                null,
                null,
                new TokenService(CONFIG),
                new ServerProperties(),
                new ServerConfig("http://localhost:8081")
        );

        Response response = assetServerApi.serveXmlDescriptionOfUrlsAvailable();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaderString("X-Timestamp")).matches("\\d{10}");
        assertThat((String) response.getEntity()).contains("http://localhost:8081/fileupload");
    }
}
