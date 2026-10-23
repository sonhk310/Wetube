package com.wetube.wetube_service.utility;

import com.wetube.wetube_service.exception.KeyLoadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads the JWT RSA key pair. The key content comes from JWT_PRIVATE_KEY / JWT_PUBLIC_KEY
 * (PEM, with or without the BEGIN/END lines) when set; otherwise from the PEM files, which
 * are kept out of git and only used for local development.
 */
@Component
public class KeyLoader {
    @Value("${app.jwt.private-key:}")
    private String privateKeyInline;
    @Value("${app.jwt.public-key:}")
    private String publicKeyInline;
    @Value("${app.jwt.private-key-pem}")
    private Resource privateKeyPem;
    @Value("${app.jwt.public-key-pem}")
    private Resource publicKeyPem;

    public RSAPrivateKey loadPrivateKey() {
        try {
            String key = readKey(privateKeyInline, privateKeyPem)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (IOException e) {
            throw new KeyLoadException("Failed to read private key file", e);
        } catch (Exception e) {
            throw new KeyLoadException("Failed to load private key", e);
        }
    }

    public RSAPublicKey loadPublicKey() {
        try {
            String key = readKey(publicKeyInline, publicKeyPem)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read public key file", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    private static String readKey(String inline, Resource file) throws IOException {
        if (inline != null && !inline.isBlank()) {
            return inline;
        }
        try (InputStream is = file.getInputStream()) {
            return new String(is.readAllBytes());
        }
    }
}
