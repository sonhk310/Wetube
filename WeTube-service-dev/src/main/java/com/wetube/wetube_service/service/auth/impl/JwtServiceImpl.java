package com.wetube.wetube_service.service.auth.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.wetube.wetube_service.exception.TokenGenerationException;
import com.wetube.wetube_service.service.auth.JwtService;
import com.wetube.wetube_service.utility.KeyLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {
    private final RSAPrivateKey privateKey;
    private final String issuer;
    private final long accessTtlHours;

    public JwtServiceImpl(KeyLoader loader,
                          @Value("${app.jwt.issuer}") String issuer,
                          @Value("${app.jwt.access-token-ttl-hour}") long accessTtl) {
        this.privateKey = loader.loadPrivateKey();
        this.issuer = issuer;
        this.accessTtlHours = accessTtl;
    }

    public String createAccessToken(UUID userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTtlHours, ChronoUnit.HOURS)))
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build();
        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new TokenGenerationException("Failed to sign the JWT", e);
        }
    }
}
