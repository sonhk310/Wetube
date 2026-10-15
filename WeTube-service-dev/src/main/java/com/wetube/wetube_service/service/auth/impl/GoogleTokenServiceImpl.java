package com.wetube.wetube_service.service.auth.impl;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.wetube.wetube_service.dto.GoogleUser;
import com.wetube.wetube_service.dto.response.GoogleTokenResponse;
import com.wetube.wetube_service.exception.InvalidTokenException;
import com.wetube.wetube_service.service.auth.GoogleTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class GoogleTokenServiceImpl implements GoogleTokenService {
    private final WebClient.Builder webClientBuilder;

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Override
    public GoogleTokenResponse exchangeCode(String code) {
        WebClient webClient = webClientBuilder.build();
        return webClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters
                        .fromFormData("code", code)
                        .with("client_id", clientId)
                        .with("client_secret", clientSecret)
                        .with("redirect_uri", redirectUri)
                        .with("grant_type", "authorization_code"))
                .retrieve()
                .bodyToMono(GoogleTokenResponse.class)
                .block();
    }

    public GoogleUser parseAndVerify(String idToken) {
        try {
            JWTClaimsSet claims = jwtProcessor.process(idToken, null);

            if (!claims.getAudience().contains(clientId)) {
                throw new InvalidTokenException("Invalid audience");
            }
            if (new Date().after(claims.getExpirationTime())) {
                throw new InvalidTokenException("Token expired");
            }

            return new GoogleUser(
                    claims.getSubject(),
                    (String) claims.getClaim("email"),
                    (String) claims.getClaim("name"),
                    (String) claims.getClaim("picture"));
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid ID token", e);
        }
    }

}
