package dk.northtech.dassco_specify_adapter.services;

import dk.northtech.dassco_specify_adapter.configuration.InstitutionConfigCryptoProperties;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class InstitutionConfigPasswordService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int NONCE_LENGTH_BYTES = 12;

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public InstitutionConfigPasswordService(InstitutionConfigCryptoProperties cryptoProperties) {
        byte[] keyBytes = Base64.getDecoder().decode(requireSecretKey(cryptoProperties));
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("institution-config-crypto.secretKey must decode to 16, 24, or 32 bytes");
        }
        this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer payload = ByteBuffer.allocate(nonce.length + encrypted.length);
            payload.put(nonce);
            payload.put(encrypted);
            return Base64.getEncoder().encodeToString(payload.array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt institution config password", exception);
        }
    }

    public String decrypt(String encryptedPayload) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedPayload);
            if (payload.length <= NONCE_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted institution config password is invalid");
            }
            ByteBuffer byteBuffer = ByteBuffer.wrap(payload);
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            byteBuffer.get(nonce);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to decrypt institution config password", exception);
        }
    }

    private String requireSecretKey(InstitutionConfigCryptoProperties cryptoProperties) {
        if (cryptoProperties.secretKey() == null || cryptoProperties.secretKey().isBlank()) {
            throw new IllegalStateException("institution-config-crypto.secretKey must be configured");
        }
        return cryptoProperties.secretKey().trim();
    }
}
