package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.configuration.SpecifyWebAssetServiceConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Formatter;

@Service
public class TokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenService.class);
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    SpecifyWebAssetServiceConfig specifyWebAssetServiceConfig;

    @Inject
    public TokenService(SpecifyWebAssetServiceConfig specifyWebAssetServiceConfig) {
        this.specifyWebAssetServiceConfig = specifyWebAssetServiceConfig;
    }

    public String generateToken(String timestamp, String filename) {
        try {
            String data = timestamp + filename;
            Mac mac = Mac.getInstance("HmacMD5");
            SecretKeySpec secretKeySpec = new SecretKeySpec(this.specifyWebAssetServiceConfig.tokenKey().getBytes(StandardCharsets.UTF_8), "HmacMD5");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            Formatter formatter = new Formatter();
            for (byte b : hmacBytes) {
                formatter.format("%02x", b);
            }
            String hmacHex = formatter.toString();
            formatter.close();
            String result = hmacHex + ":" + timestamp;
            return result;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public void validateToken(String token, String filename) {
        if(this.specifyWebAssetServiceConfig.tokenKey() == null) return;
        if(token.isEmpty()){
            throw forbidden("Auth token is missing.");
        }
        if (!token.contains(":")){
            throw forbidden("Auth token is malformed.");
        }
        String[] tokenParts = token.split(":");
        Long tokenTimeStamp = Long.parseLong(tokenParts[1]);
        Long currentTime = currentTimestampSeconds();

        if(this.specifyWebAssetServiceConfig.tokenTimeToleranceSeconds() != null){
            if(Math.abs(currentTime - tokenTimeStamp) > this.specifyWebAssetServiceConfig.tokenTimeToleranceSeconds()){
                throw forbidden("Auth token timestamp out of range: %s vs %s".formatted(tokenTimeStamp, currentTime));
            }
        }
        if(!token.equals(this.generateToken(String.valueOf(tokenTimeStamp), filename))){
            throw forbidden("Auth token is invalid.");
        }

    }

    private WebApplicationException forbidden(String message) {
        return new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                .header(TIMESTAMP_HEADER, String.valueOf(currentTimestampSeconds()))
                .entity(message)
                .build());
    }

    private long currentTimestampSeconds() {
        return Instant.now().getEpochSecond();
    }
}
