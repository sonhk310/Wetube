package com.wetube.wetube_service.service.live;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.wetube.wetube_service.dto.LivekitProperties;
import com.wetube.wetube_service.enumeration.LiveProtocol;

import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanSubscribe;
import io.livekit.server.IngressServiceClient;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
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

    private final RoomServiceClient roomClient;
    private final IngressServiceClient ingressClient;
    private final LivekitProperties props;

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
