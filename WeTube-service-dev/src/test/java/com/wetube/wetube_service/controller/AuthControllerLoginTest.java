package com.wetube.wetube_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.wetube.wetube_service.service.UserService;
import com.wetube.wetube_service.service.auth.GoogleTokenService;
import com.wetube.wetube_service.service.auth.JwtService;
import com.wetube.wetube_service.service.auth.RefreshTokenService;

/** Đăng nhập Google: "Chuyển tài khoản" ép màn hình chọn tài khoản, đăng nhập thường giữ nguyên như cũ. */
class AuthControllerLoginTest {

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(mock(GoogleTokenService.class), mock(UserService.class),
                mock(JwtService.class), mock(RefreshTokenService.class));
        ReflectionTestUtils.setField(controller, "clientId", "client-123");
        ReflectionTestUtils.setField(controller, "redirectUri", "https://api.example.test/api/auth/login/google/callback");
        ReflectionTestUtils.setField(controller, "clientUrl", "https://wetube.example.test");
    }

    private String promptOf(boolean switchAccount) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.redirectToGoogle(switchAccount, response);
        String location = response.getRedirectedUrl();
        assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        String raw = UriComponentsBuilder.fromUri(URI.create(location.replace(" ", "%20"))).build().getQueryParams().getFirst("prompt");
        return URLDecoder.decode(raw, StandardCharsets.UTF_8);
    }

    @Test
    void normalLoginKeepsPromptConsent() throws Exception {
        assertThat(promptOf(false)).isEqualTo("consent");
    }

    @Test
    void switchAccountForcesTheGoogleAccountChooser() throws Exception {
        assertThat(promptOf(true)).isEqualTo("select_account consent");
    }

    @Test
    void bothKeepTheSameClientAndRedirectUri() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.redirectToGoogle(true, response);

        assertThat(response.getRedirectedUrl()).contains("client_id=client-123").contains("response_type=code")
                .contains("redirect_uri=https://api.example.test/api/auth/login/google/callback");
    }
}
