package com.wetube.wetube_service.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class WebCsrfConfiguration {

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        var repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repo.setCookieName("XSRF-TOKEN");
        repo.setHeaderName("X-XSRF-TOKEN");
        repo.setCookiePath("/");
        return repo;
    }

    @Bean
    public RequestMatcher csrfIgnoringRequestMatcher() {
        // Ignore CSRF for endpoints auth/health if needed
        return new OrRequestMatcher(
                new RegexRequestMatcher("^/auth(/.*)?$", null),
                new RegexRequestMatcher("^/api/auth(/.*)?$", null),
                new RegexRequestMatcher("^/api/playlists(/.*)?$", null),
                new RegexRequestMatcher("^/api/posts(/.*)?$", null),
                new RegexRequestMatcher("^/actuator(/.*)?$", null),
                new RegexRequestMatcher("^/api/payment(/.*)?$", null),
                new RegexRequestMatcher("^/api/subscriptions(/.*)?$", null),
                new RegexRequestMatcher("^/api/subpacks(/.*)?$", null),
                new RegexRequestMatcher("^/api/me(/.*)?$", null),
                new RegexRequestMatcher("^/api/videos(/.*)?$", null),
                new RegexRequestMatcher("^/api/channel(/.*)?$", null),
                new RegexRequestMatcher("^/api/livekit(/.*)?$", null),
                new RegexRequestMatcher("^/api/tiers(/.*)?$", null),
                new RegexRequestMatcher("^/ws(/.*)?$", null),
                new RegexRequestMatcher("^/api/uploads(/.*)?$", null),
                new RegexRequestMatcher("^/api/likes(/.*)?$", null),
                new RegexRequestMatcher("^/api/comments(/.*)?$", null),
                new RegexRequestMatcher("^/api/users(/.*)?$", null),
                new RegexRequestMatcher("^/api/ai(/.*)?$", null)

        );
    }
}
