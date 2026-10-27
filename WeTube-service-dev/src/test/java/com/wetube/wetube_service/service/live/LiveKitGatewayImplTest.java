package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.wetube.wetube_service.dto.LivekitProperties;

import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

class LiveKitGatewayImplTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private static LivekitProperties props() {
        LivekitProperties p = new LivekitProperties();
        p.setUrl("wss://demo.livekit.cloud");
        p.setApiKey("key");
        p.setApiSecret(SECRET);
        return p;
    }

    private final LiveKitGatewayImpl gateway = new LiveKitGatewayImpl(null, null, props());

    private static JWTClaimsSet verified(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        assertThat(jwt.verify(new MACVerifier(SECRET))).isTrue();
        return jwt.getJWTClaimsSet();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> video(JWTClaimsSet claims) {
        return (Map<String, Object>) claims.getClaim("video");
    }

    @Test
    void viewerTokenCannotPublish() throws Exception {
        JWTClaimsSet claims = verified(gateway.viewerToken("live-x", "v:abc"));

        assertThat(claims.getSubject()).isEqualTo("v:abc");
        assertThat(video(claims)).containsEntry("room", "live-x")
                .containsEntry("roomJoin", true)
                .containsEntry("canSubscribe", true);
        assertThat(video(claims).get("canPublish")).isNotEqualTo(true);
        assertThat(video(claims).get("canPublishData")).isNotEqualTo(true);
    }

    @Test
    void publisherTokenCanPublish() throws Exception {
        JWTClaimsSet claims = verified(gateway.publisherToken("live-x", "u:owner", "Chủ kênh"));

        assertThat(claims.getSubject()).isEqualTo("u:owner");
        assertThat(video(claims)).containsEntry("room", "live-x").containsEntry("canPublish", true)
                .containsEntry("canSubscribe", true);
    }

    @Test
    void tokenExpiresInAboutSixHours() throws Exception {
        JWTClaimsSet claims = verified(gateway.viewerToken("live-x", "v:abc"));

        long secondsLeft = claims.getExpirationTime().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        assertThat(secondsLeft).isBetween(21_500L, 21_700L);
    }

    @Test
    void wsUrlComesFromConfiguration() {
        assertThat(gateway.wsUrl()).isEqualTo("wss://demo.livekit.cloud");
    }

    @SuppressWarnings("unchecked")
    private LiveKitGatewayImpl gatewayListing(Call<List<LivekitModels.ParticipantInfo>> call) {
        RoomServiceClient rooms = mock(RoomServiceClient.class);
        when(rooms.listParticipants("live-x")).thenReturn(call);
        return new LiveKitGatewayImpl(rooms, null, props());
    }

    @SuppressWarnings("unchecked")
    @Test
    void roomIsCreatedWithAHardParticipantCapAndAnHourOfEmptyTimeout() throws Exception {
        RoomServiceClient rooms = mock(RoomServiceClient.class);
        Call<LivekitModels.Room> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(null));
        when(rooms.createRoom(eq("live-x"), eq(3600), eq(45), any(), eq("{}"), any(), any(), any(), any()))
                .thenReturn(call);

        new LiveKitGatewayImpl(rooms, null, props()).createRoom("live-x", "{}", 45);

        verify(rooms).createRoom(eq("live-x"), eq(3600), eq(45), any(), eq("{}"), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void quotaStatusFromLivekitIsFlaggedAsQuota() throws Exception {
        Call<List<LivekitModels.ParticipantInfo>> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.error(429, ResponseBody.create(null, "limit exceeded")));

        assertThatThrownBy(() -> gatewayListing(call).participants("live-x"))
                .isInstanceOfSatisfying(LiveKitUnavailableException.class, e -> assertThat(e.isQuota()).isTrue());
    }

    @SuppressWarnings("unchecked")
    @Test
    void networkErrorIsUnavailableButNotQuota() throws Exception {
        Call<List<LivekitModels.ParticipantInfo>> call = mock(Call.class);
        when(call.execute()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> gatewayListing(call).participants("live-x"))
                .isInstanceOfSatisfying(LiveKitUnavailableException.class, e -> assertThat(e.isQuota()).isFalse());
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingRoomGivesEmptyParticipants() throws Exception {
        Call<List<LivekitModels.ParticipantInfo>> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.error(404, ResponseBody.create(null, "room does not exist")));

        assertThat(gatewayListing(call).participants("live-x")).isEmpty();
    }
}
