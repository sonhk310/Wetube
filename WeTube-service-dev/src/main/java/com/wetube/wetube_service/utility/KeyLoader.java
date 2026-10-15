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

@Component
public class KeyLoader {
    @Value("${app.jwt.private-key-pem}")
    private Resource privateKeyPem;
    @Value("${app.jwt.public-key-pem}")
    private Resource publicKeyPem;

    public RSAPrivateKey loadPrivateKey() {
        try (InputStream is = privateKeyPem.getInputStream()) {
            String key = new String(is.readAllBytes());
            key = key.replace("-----BEGIN PRIVATE KEY-----", "")
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
        try (InputStream is = publicKeyPem.getInputStream()) {
            String key = new String(is.readAllBytes());
            key = key.replace("-----BEGIN PUBLIC KEY-----", "")
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
}

