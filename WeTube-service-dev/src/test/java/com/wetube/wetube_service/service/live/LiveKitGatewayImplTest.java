package com.wetube.wetube_service.service.live;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.wetube.wetube_service.dto.LiveRecordingProperties;
import com.wetube.wetube_service.dto.LivekitProperties;

import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
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

    private final LiveKitGatewayImpl gateway =
            new LiveKitGatewayImpl(null, null, null, props(), new LiveRecordingProperties());

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
        return new LiveKitGatewayImpl(rooms, null, null, props(), new LiveRecordingProperties());
    }

    private static LiveRecordingProperties recordingProps() {
        LiveRecordingProperties p = new LiveRecordingProperties();
        p.setEnabled(true);
        p.setS3Endpoint("https://acc.r2.cloudflarestorage.com");
        p.setS3Bucket("wetube-live");
        p.setS3AccessKey("access");
        p.setS3Secret("secret");
        p.setPublicUrl("https://pub.example");
        return p;
    }

    @SuppressWarnings("unchecked")
    private static <T> Call<T> answering(T body) throws Exception {
        Call<T> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(body));
        return call;
    }

    @Test
    void recordingGoesToTheR2BucketAsAnMp4OfTheSpeakerLayout() throws Exception {
        EgressServiceClient egress = mock(EgressServiceClient.class);
        Call<LivekitEgress.EgressInfo> started = answering(LivekitEgress.EgressInfo.newBuilder().setEgressId("EG_1").build());
        when(egress.startRoomCompositeEgress(eq("live-x"), any(LivekitEgress.EncodedFileOutput.class), eq("speaker"),
                isNull(), any(LivekitEgress.EncodingOptions.class), eq(false), eq(false), isNull(), isNull()))
                .thenReturn(started);

        String id = new LiveKitGatewayImpl(null, null, egress, props(), recordingProps())
                .startRecording("live-x", "live/abc.mp4");

        assertThat(id).isEqualTo("EG_1");
        ArgumentCaptor<LivekitEgress.EncodedFileOutput> output = ArgumentCaptor.forClass(LivekitEgress.EncodedFileOutput.class);
        ArgumentCaptor<LivekitEgress.EncodingOptions> encoding = ArgumentCaptor.forClass(LivekitEgress.EncodingOptions.class);
        verify(egress).startRoomCompositeEgress(eq("live-x"), output.capture(), eq("speaker"), isNull(),
                encoding.capture(), eq(false), eq(false), isNull(), isNull());
        LivekitEgress.EncodedFileOutput file = output.getValue();
        assertThat(file.getFileType()).isEqualTo(LivekitEgress.EncodedFileType.MP4);
        assertThat(file.getFilepath()).isEqualTo("live/abc.mp4");
        LivekitEgress.S3Upload s3 = file.getS3();
        assertThat(s3.getBucket()).isEqualTo("wetube-live");
        assertThat(s3.getEndpoint()).isEqualTo("https://acc.r2.cloudflarestorage.com");
        assertThat(s3.getAccessKey()).isEqualTo("access");
        assertThat(s3.getSecret()).isEqualTo("secret");
        assertThat(s3.getRegion()).isEqualTo("auto");
        assertThat(s3.getForcePathStyle()).isTrue();
        assertThat(encoding.getValue().getWidth()).isEqualTo(1280);
        assertThat(encoding.getValue().getHeight()).isEqualTo(720);
        assertThat(encoding.getValue().getFramerate()).isEqualTo(30);
        assertThat(encoding.getValue().getVideoBitrate()).isEqualTo(1500);
        assertThat(encoding.getValue().getAudioBitrate()).isEqualTo(96);
    }

    @Test
    void theStorageRegionIsConfigurableForProvidersThatSignWithTheRealRegion() throws Exception {
        LiveRecordingProperties b2 = recordingProps();
        b2.setS3Region(" us-west-004 ");
        EgressServiceClient egress = mock(EgressServiceClient.class);
        Call<LivekitEgress.EgressInfo> started = answering(LivekitEgress.EgressInfo.newBuilder().setEgressId("EG_2").build());
        when(egress.startRoomCompositeEgress(any(), any(LivekitEgress.EncodedFileOutput.class), any(), any(), any(),
                eq(false), eq(false), any(), any())).thenReturn(started);

        new LiveKitGatewayImpl(null, null, egress, props(), b2).startRecording("live-x", "live/abc.mp4");

        ArgumentCaptor<LivekitEgress.EncodedFileOutput> output = ArgumentCaptor.forClass(LivekitEgress.EncodedFileOutput.class);
        verify(egress).startRoomCompositeEgress(any(), output.capture(), any(), any(), any(), eq(false), eq(false),
                any(), any());
        assertThat(output.getValue().getS3().getRegion()).isEqualTo("us-west-004");
    }

    @Test
    void stoppingARecordingThatAlreadyEndedIsNotAnError() throws Exception {
        EgressServiceClient egress = mock(EgressServiceClient.class);
        Call<LivekitEgress.EgressInfo> gone = mock(Call.class);
        when(gone.execute()).thenReturn(Response.error(404, ResponseBody.create(null, "not found")));
        when(egress.stopEgress("EG_1")).thenReturn(gone);

        new LiveKitGatewayImpl(null, null, egress, props(), recordingProps()).stopRecording("EG_1");

        verify(egress).stopEgress("EG_1");
    }

    private LiveKitGateway.RecordingInfo recordingOf(LivekitEgress.EgressInfo info) throws Exception {
        EgressServiceClient egress = mock(EgressServiceClient.class);
        Call<List<LivekitEgress.EgressInfo>> found = answering(List.of(info));
        when(egress.listEgress(isNull(), eq("EG_1"))).thenReturn(found);
        return new LiveKitGatewayImpl(null, null, egress, props(), recordingProps()).recording("EG_1");
    }

    private static LivekitEgress.EgressInfo egressInfo(LivekitEgress.EgressStatus status, long durationNanos,
                                                        String error) {
        LivekitEgress.EgressInfo.Builder b = LivekitEgress.EgressInfo.newBuilder().setEgressId("EG_1")
                .setStatus(status).setError(error);
        if (durationNanos > 0) {
            b.addFileResults(LivekitEgress.FileInfo.newBuilder().setFilename("live/abc.mp4")
                    .setDuration(durationNanos).setSize(1000));
        }
        return b.build();
    }

    @Test
    void aCompletedRecordingReportsItsDurationInSeconds() throws Exception {
        LiveKitGateway.RecordingInfo info = recordingOf(
                egressInfo(LivekitEgress.EgressStatus.EGRESS_COMPLETE, 612_500_000_000L, ""));

        assertThat(info).isEqualTo(new LiveKitGateway.RecordingInfo(true, true, 612, ""));
    }

    @Test
    void aRecordingThatHitTheThreeHourLimitStillDeliversItsFile() throws Exception {
        LiveKitGateway.RecordingInfo info = recordingOf(
                egressInfo(LivekitEgress.EgressStatus.EGRESS_LIMIT_REACHED, 10_800_000_000_000L, ""));

        assertThat(info.finished()).isTrue();
        assertThat(info.hasFile()).isTrue();
        assertThat(info.seconds()).isEqualTo(10_800);
    }

    @Test
    void aFailedRecordingHasNoFileAndCarriesTheError() throws Exception {
        LiveKitGateway.RecordingInfo info = recordingOf(
                egressInfo(LivekitEgress.EgressStatus.EGRESS_FAILED, 0, "storage quota"));

        assertThat(info).isEqualTo(new LiveKitGateway.RecordingInfo(true, false, 0, "storage quota"));
    }

    @Test
    void aRecordingStillRunningOrEndingIsNotFinished() throws Exception {
        assertThat(recordingOf(egressInfo(LivekitEgress.EgressStatus.EGRESS_ACTIVE, 0, "")).finished()).isFalse();
        assertThat(recordingOf(egressInfo(LivekitEgress.EgressStatus.EGRESS_ENDING, 0, "")).finished()).isFalse();
        assertThat(recordingOf(egressInfo(LivekitEgress.EgressStatus.EGRESS_STARTING, 0, "")).finished()).isFalse();
    }

    @Test
    void anUnknownEgressIdGivesNull() throws Exception {
        EgressServiceClient egress = mock(EgressServiceClient.class);
        Call<List<LivekitEgress.EgressInfo>> none = answering(List.<LivekitEgress.EgressInfo>of());
        when(egress.listEgress(isNull(), eq("EG_X"))).thenReturn(none);

        assertThat(new LiveKitGatewayImpl(null, null, egress, props(), recordingProps()).recording("EG_X")).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void participantPublishesWhenItHasAnyTrackIncludingAudioOnly() throws Exception {
        Call<List<LivekitModels.ParticipantInfo>> call = mock(Call.class);
        LivekitModels.ParticipantInfo audioOnly = LivekitModels.ParticipantInfo.newBuilder().setIdentity("u:a")
                .addTracks(LivekitModels.TrackInfo.newBuilder().setType(LivekitModels.TrackType.AUDIO)).build();
        LivekitModels.ParticipantInfo viewer = LivekitModels.ParticipantInfo.newBuilder().setIdentity("v:b").build();
        when(call.execute()).thenReturn(Response.success(List.of(audioOnly, viewer)));

        List<LiveKitGateway.Participant> participants = gatewayListing(call).participants("live-x");

        assertThat(participants).containsExactly(new LiveKitGateway.Participant("u:a", true),
                new LiveKitGateway.Participant("v:b", false));
    }

    @SuppressWarnings("unchecked")
    @Test
    void roomIsCreatedWithAHardParticipantCapAndAnHourOfEmptyTimeout() throws Exception {
        RoomServiceClient rooms = mock(RoomServiceClient.class);
        Call<LivekitModels.Room> call = mock(Call.class);
        when(call.execute()).thenReturn(Response.success(null));
        when(rooms.createRoom(eq("live-x"), eq(3600), eq(45), any(), eq("{}"), any(), any(), any(), any()))
                .thenReturn(call);

        new LiveKitGatewayImpl(rooms, null, null, props(), new LiveRecordingProperties()).createRoom("live-x", "{}", 45);

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
