package com.wetube.wetube_service.configuration;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.wetube.wetube_service.exception.MissingConfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.net.MalformedURLException;


@Configuration
public class GoogleJwtConfiguration {
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Bean
    public ConfigurableJWTProcessor<SecurityContext> googleJwtProcessor() throws MalformedURLException {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new MissingConfigurationException(
                    "Missing 'spring.security.oauth2.client.registration.google.client-id' in application.yml");
        }

        var resourceRetriever = new DefaultResourceRetriever(
                (int) Duration.ofSeconds(3).toMillis(),
                (int) Duration.ofSeconds(3).toMillis(),
                1024 * 1024
        );

        JWKSource<SecurityContext> jwkSource = JWKSourceBuilder.create(
                        URI.create(GOOGLE_JWKS).toURL(),
                        resourceRetriever
                )
                // .cache(ttlMillis, refreshAheadMillis) // optional: configure caching
                 .retrying(true)
                // .outageTolerant(ttlMillis) // optional: tolerate outages by using cached keys
                .build();

        var jwtProcessor = new DefaultJWTProcessor<>();

        var keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        jwtProcessor.setJWSKeySelector(keySelector);

        JWTClaimsSet expectedClaims = new JWTClaimsSet.Builder()
                .issuer(GOOGLE_ISSUER)
                .audience(googleClientId)
                .build();

        Set<String> requiredClaims = new HashSet<>();
        requiredClaims.add("sub");
        requiredClaims.add("exp");
        requiredClaims.add("iat");

        var claimsVerifier = new DefaultJWTClaimsVerifier<>(expectedClaims, requiredClaims);
        jwtProcessor.setJWTClaimsSetVerifier(claimsVerifier);

        return jwtProcessor;
    }
}

