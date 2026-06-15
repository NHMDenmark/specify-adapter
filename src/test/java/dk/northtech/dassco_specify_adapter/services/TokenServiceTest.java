package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.configuration.SpecifyWebAssetServiceConfig;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {
    private static final String TOKEN_KEY = "test_attachment_key";

    private final TokenService tokenService = new TokenService(new SpecifyWebAssetServiceConfig(
            "true",
            "specify-001",
            TOKEN_KEY,
            150,
            false,
            "NHMD"
    ));

    @Test
    void validatesTokenGeneratedWithEpochSeconds() {
        long timestamp = Instant.now().getEpochSecond();
        String token = tokenService.generateToken(String.valueOf(timestamp), "file.jpg");

        tokenService.validateToken(token, "file.jpg");
    }

    @Test
    void rejectsMillisecondTimestampToken() {
        long timestamp = System.currentTimeMillis();
        String token = tokenService.generateToken(String.valueOf(timestamp), "file.jpg");

        WebApplicationException exception = assertThrows(WebApplicationException.class,
                () -> tokenService.validateToken(token, "file.jpg"));

        assertThat(exception.getResponse().getStatus()).isEqualTo(403);
        assertThat(exception.getResponse().getEntity()).isEqualTo(
                "Auth token timestamp out of range: %s vs %s".formatted(timestamp,
                        Long.parseLong(exception.getResponse().getHeaderString("X-Timestamp"))));
        assertThat(exception.getResponse().getHeaderString("X-Timestamp")).matches("\\d{10}");
    }
}
