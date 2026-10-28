package com.wetube.wetube_service.service.live;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.LiveRecordingProperties;
import com.wetube.wetube_service.dto.LivekitProperties;
import com.wetube.wetube_service.enumeration.LiveProtocol;

import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanSubscribe;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.IngressServiceClient;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import livekit.LivekitIngress;
import livekit.LivekitModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveKitGatewayImpl implements LiveKitGateway {

    /** AccessToken.setTtl của SDK nhận mili-giây (đưa 21600 vào thì token chỉ sống 21 giây). */
    private static final long TOKEN_TTL_MILLIS = Duration.ofHours(6).toMillis();

    /** Phòng chưa có ai (chờ OBS nối) được giữ chừng này giây; buổi CREATED bị huỷ sau 1 giờ nên không cần lâu hơn. */
    private static final int EMPTY_TIMEOUT_SECONDS = (int) Duration.ofHours(1).toSeconds();

    /** Bản ghi: 720p 30 khung / giây, khoảng 1,5 Mb/giây hình + 96 kb/giây tiếng (≈ 0,7 GB một giờ). */
    private static final int RECORD_WIDTH = 1280;
    private static final int RECORD_HEIGHT = 720;
    private static final int RECORD_FPS = 30;
    private static final int RECORD_VIDEO_KBPS = 1500;
    private static final int RECORD_AUDIO_KBPS = 96;
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final RoomServiceClient roomClient;
    private final IngressServiceClient ingressClient;
    private final EgressServiceClient egressClient;
    private final LivekitProperties props;
    private final LiveRecordingProperties recording;

    @Override
    public void createRoom(String room, String metadataJson, int maxParticipants) {
        ok(send(roomClient.createRoom(room, EMPTY_TIMEOUT_SECONDS, maxParticipants, null, metadataJson, null, null,
                null, null)));
    }

    @Override
    public void deleteRoom(String room) {
        Response<Void> resp = send(roomClient.deleteRoom(room));
        if (resp.code() != 404) {
            ok(resp);
        }
    }

    @Override
    public List<Participant> participants(String room) {
        Response<List<LivekitModels.ParticipantInfo>> resp = send(roomClient.listParticipants(room));
        if (resp.code() == 404) {
            return List.of();
        }
        List<LivekitModels.ParticipantInfo> body = ok(resp);
        if (body == null) {
            return List.of();
        }
        return body.stream()
                .map(p -> new Participant(p.getIdentity(), p.getTracksCount() > 0))
                .toList();
    }

    @Override
    public String publisherToken(String room, String identity, String displayName) {
        AccessToken at = token(identity, displayName);
        at.addGrants(new RoomJoin(true), new RoomName(room), new CanPublish(true), new CanSubscribe(true));
        return at.toJwt();
    }

    @Override
    public String viewerToken(String room, String identity) {
        AccessToken at = token(identity, null);
        // LiveKit coi quyền vắng mặt là true, nên phải ghi rõ false
        at.addGrants(new RoomJoin(true), new RoomName(room), new CanSubscribe(true), new CanPublish(false),
                new CanPublishData(false));
        return at.toJwt();
    }

    @Override
    public IngressDetails createIngress(String room, String identity, String displayName, LiveProtocol protocol) {
        boolean whip = protocol == LiveProtocol.WHIP;
        LivekitIngress.IngressAudioOptions audio = null;
        LivekitIngress.IngressVideoOptions video = null;
        if (!whip) {
            video = LivekitIngress.IngressVideoOptions.newBuilder()
                    .setSource(LivekitModels.TrackSource.CAMERA)
                    .setPreset(LivekitIngress.IngressVideoEncodingPreset.H264_1080P_30FPS_3_LAYERS)
                    .build();
            audio = LivekitIngress.IngressAudioOptions.newBuilder()
                    .setSource(LivekitModels.TrackSource.MICROPHONE)
                    .setPreset(LivekitIngress.IngressAudioEncodingPreset.OPUS_STEREO_96KBPS)
                    .build();
        }
        // WHIP: không transcode (không tốn phút transcode của gói free); RTMP luôn transcode
        LivekitIngress.IngressInfo info = ok(send(ingressClient.createIngress(room, room, identity, displayName,
                whip ? LivekitIngress.IngressInput.WHIP_INPUT : LivekitIngress.IngressInput.RTMP_INPUT,
                audio, video, null, !whip)));
        return new IngressDetails(info.getIngressId(), info.getUrl(), info.getStreamKey());
    }

    @Override
    public void deleteIngress(String ingressId) {
        Response<LivekitIngress.IngressInfo> resp = send(ingressClient.deleteIngress(ingressId));
        if (resp.code() != 404) {
            ok(resp);
        }
    }

    @Override
    public int deleteInactiveIngress(Collection<String> keepIngressIds) {
        List<LivekitIngress.IngressInfo> all = ok(send(ingressClient.listIngress()));
        int deleted = 0;
        for (LivekitIngress.IngressInfo info : all == null ? List.<LivekitIngress.IngressInfo>of() : all) {
            if (keepIngressIds.contains(info.getIngressId()) || !info.hasState()) {
                continue;
            }
            // 0 = ENDPOINT_INACTIVE, 3 = ENDPOINT_ERROR (như job dọn cũ)
            int status = info.getState().getStatusValue();
            if (status != 0 && status != 3) {
                continue;
            }
            try {
                deleteIngress(info.getIngressId());
                deleted++;
            } catch (LiveKitUnavailableException e) {
                log.warn("[LiveKit] Không xoá được Ingress {}: {}", info.getIngressId(), e.getMessage());
            }
        }
        return deleted;
    }

    @Override
    public String startRecording(String room, String filepath) {
        LivekitEgress.S3Upload s3 = LivekitEgress.S3Upload.newBuilder()
                .setAccessKey(recording.getS3AccessKey())
                .setSecret(recording.getS3Secret())
                .setBucket(recording.getS3Bucket())
                .setEndpoint(recording.getS3Endpoint())
                .setRegion("auto")
                .setForcePathStyle(true)
                .build();
        LivekitEgress.EncodedFileOutput output = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath(filepath)
                .setS3(s3)
                .build();
        LivekitEgress.EncodingOptions encoding = LivekitEgress.EncodingOptions.newBuilder()
                .setWidth(RECORD_WIDTH)
                .setHeight(RECORD_HEIGHT)
                .setFramerate(RECORD_FPS)
                .setVideoCodec(LivekitModels.VideoCodec.H264_MAIN)
                .setVideoBitrate(RECORD_VIDEO_KBPS)
                .setAudioCodec(LivekitModels.AudioCodec.AAC)
                .setAudioBitrate(RECORD_AUDIO_KBPS)
                .build();
        LivekitEgress.EgressInfo info = ok(send(egressClient.startRoomCompositeEgress(room, output, "speaker", null,
                encoding, false, false, null, null)));
        return info.getEgressId();
    }

    @Override
    public void stopRecording(String egressId) {
        Response<LivekitEgress.EgressInfo> resp = send(egressClient.stopEgress(egressId));
        // 404: phiên ghi đã tự kết thúc (vd. phòng bị xoá) nên không còn gì để dừng
        if (resp.code() != 404) {
            ok(resp);
        }
    }

    @Override
    public RecordingInfo recording(String egressId) {
        List<LivekitEgress.EgressInfo> found = ok(send(egressClient.listEgress(null, egressId)));
        if (found == null || found.isEmpty()) {
            return null;
        }
        LivekitEgress.EgressInfo info = found.get(0);
        LivekitEgress.EgressStatus status = info.getStatus();
        boolean finished = status == LivekitEgress.EgressStatus.EGRESS_COMPLETE
                || status == LivekitEgress.EgressStatus.EGRESS_FAILED
                || status == LivekitEgress.EgressStatus.EGRESS_ABORTED
                || status == LivekitEgress.EgressStatus.EGRESS_LIMIT_REACHED;
        // LIMIT_REACHED (file MP4 tối đa 3 giờ) vẫn giao phần đã ghi
        boolean hasFile = info.getFileResultsCount() > 0 && !info.getFileResults(0).getFilename().isEmpty()
                && (status == LivekitEgress.EgressStatus.EGRESS_COMPLETE
                || status == LivekitEgress.EgressStatus.EGRESS_LIMIT_REACHED);
        long seconds = hasFile ? info.getFileResults(0).getDuration() / NANOS_PER_SECOND : 0;
        return new RecordingInfo(finished, hasFile, seconds, info.getError());
    }

    @Override
    public String wsUrl() {
        return props.getUrl();
    }

    private AccessToken token(String identity, String displayName) {
        AccessToken at = new AccessToken(props.getApiKey(), props.getApiSecret());
        at.setIdentity(identity);
        if (displayName != null) {
            at.setName(displayName);
        }
        at.setTtl(TOKEN_TTL_MILLIS);
        return at;
    }

    private static <T> Response<T> send(Call<T> call) {
        try {
            return call.execute();
        } catch (IOException e) {
            throw new LiveKitUnavailableException("Không kết nối được LiveKit: " + e.getMessage(), false, e);
        }
    }

    private static <T> T ok(Response<T> resp) {
        if (resp.isSuccessful()) {
            return resp.body();
        }
        String detail = "";
        try {
            detail = resp.errorBody() != null ? resp.errorBody().string() : "";
        } catch (IOException ignored) {
            // chỉ để làm rõ thông báo lỗi
        }
        boolean quota = resp.code() == 403 || resp.code() == 429
                || detail.toLowerCase(Locale.ROOT).contains("quota")
                || detail.toLowerCase(Locale.ROOT).contains("limit");
        throw new LiveKitUnavailableException("LiveKit trả lỗi " + resp.code() + ": " + detail, quota);
    }
}
