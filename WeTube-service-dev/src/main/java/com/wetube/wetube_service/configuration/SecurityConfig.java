package com.wetube.wetube_service.configuration;

import java.util.ArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import com.wetube.wetube_service.security.CookieBearerTokenResolver;
import com.wetube.wetube_service.security.ForbiddenEntryPoint;
import com.wetube.wetube_service.security.UnauthorizedEntryPoint;
import com.wetube.wetube_service.utility.KeyLoader;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final UnauthorizedEntryPoint unauthorizedEntryPoint;
    private final KeyLoader keyLoader;
    private final WebCsrfConfiguration webCsrfConfiguration;
    private final ForbiddenEntryPoint forbiddenEntryPoint;
    private final CookieBearerTokenResolver cookieBearerTokenResolver;

    @Bean
    Converter<Jwt, JwtAuthenticationToken> authenticationConverter() {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles"); // token nội bộ của bạn chứa "roles"
        roles.setAuthorityPrefix(""); // đã có tiền tố ROLE_ trong claim

        return jwt -> {
            var authorities = new ArrayList<>(roles.convert(jwt));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable()
                        // .csrfTokenRepository(webCsrfConfiguration.csrfTokenRepository())
//                         .ignoringRequestMatchers(
//                                 webCsrfConfiguration.csrfIgnoringRequestMatcher()
// //                                authApiMatcher() // ignore CSRF for /api/auth/**
                        // )
//                .csrf(csrf -> csrf
//                                .csrfTokenRepository(webCsrfConfiguration.csrfTokenRepository())
//                                .ignoringRequestMatchers(webCsrfConfiguration.csrfIgnoringRequestMatcher())
                        )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint) // 401
                        .accessDeniedHandler(forbiddenEntryPoint) // 403
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(cookieBearerTokenResolver)
                        .jwt(jwt -> jwt.decoder(NimbusJwtDecoder
                                .withPublicKey(keyLoader.loadPublicKey()).build())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/playlists/**").permitAll()
                        .requestMatchers("/api/posts/**").permitAll()
                        .requestMatchers("/api/livekit/**").permitAll()
                        .requestMatchers("/api/payment/**").permitAll()
                        .requestMatchers("/api/subpacks/**").permitAll()
                        .requestMatchers("/api/channels/**").permitAll()
                        .requestMatchers("/api/tiers", "/api/tiers/**").permitAll()
                        .requestMatchers("/api/subscriptions/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/api/videos","/api/videos/**").permitAll()
                        .requestMatchers("/api/channel/**").permitAll()
                        .requestMatchers("/api/recommend/**").permitAll()
                        .requestMatchers("/api/transactions/**").permitAll()
                        .requestMatchers("/api/interactions/**").permitAll()
                        .requestMatchers("/es/health/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                        .requestMatchers("/api/customers/**").authenticated()
                        .requestMatchers("/api/uploads/**").permitAll()
                        .requestMatchers("/api/likes/**").permitAll()
                        .requestMatchers("/api/comments/**").permitAll()
                        .requestMatchers("/api/ai/**").permitAll())

                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
