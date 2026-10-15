package com.wetube.wetube_service.service.livekit;

import com.wetube.wetube_service.dto.LivekitProperties;
import livekit.LivekitIngress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import io.livekit.server.IngressServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.util.JsonFormat;
import com.wetube.wetube_service.dto.request.livekit.CreateIngressParams;
import com.wetube.wetube_service.dto.response.livekit.ConnectionDetails;
import com.wetube.wetube_service.dto.response.livekit.CreateIngressResponse;
import com.wetube.wetube_service.dto.request.livekit.RoomMetadata;
import io.livekit.server.*;
import livekit.LivekitModels;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivekitIngressService {
    private final IngressServiceClient ingressClient;
    private final RoomServiceClient roomClient;
    private final ObjectMapper objectMapper;
    private final LivekitProperties livekitProperties;

    public Map<String, Object> deleteInactiveIngress() throws IOException {
        Response<List<LivekitIngress.IngressInfo>> resp = ingressClient.listIngress().execute();

        if (!resp.isSuccessful() || resp.body() == null) {
            String errMsg = "Failed to list ingress: " +
                    (resp.errorBody() != null ? resp.errorBody().string() : "unknown");
            log.error("[LiveKit] {}", errMsg);
            throw new RuntimeException(errMsg);
        }

        List<LivekitIngress.IngressInfo> ingressList = resp.body();
        List<String> inactiveIds = new ArrayList<>();
        List<String> deletedIds = new ArrayList<>();
        List<String> failedToDelete = new ArrayList<>();

        // Log danh sách fetch được
        log.info("=== [LiveKit] All Ingress Fetched ({} total) ===", ingressList.size());
        for (LivekitIngress.IngressInfo ingress : ingressList) {
            log.info("IngressId={} | Room={} | Status={}",
                    ingress.getIngressId(),
                    ingress.getRoomName(),
                    ingress.hasState() ? ingress.getState().getStatus() : "UNKNOWN");
        }

        // Lọc INACTIVE và xoá
        for (LivekitIngress.IngressInfo ingress : ingressList) {
            if (ingress.hasState()
                    && ( ingress.getState().getStatus() == LivekitIngress.IngressState.Status.forNumber(0)
                    || ingress.getState().getStatus() == LivekitIngress.IngressState.Status.forNumber(3) )) {

                inactiveIds.add(ingress.getIngressId());

                Response<LivekitIngress.IngressInfo> delResp =
                        ingressClient.deleteIngress(ingress.getIngressId()).execute();

                if (delResp.isSuccessful()) {
                    deletedIds.add(ingress.getIngressId());
                    log.info("[LiveKit] Deleted Ingress {}", ingress.getIngressId());
                } else {
                    failedToDelete.add(ingress.getIngressId());
                    log.warn("[LiveKit] Failed To Delete Ingress {} - HTTP {}",
                            ingress.getIngressId(), delResp.code());
                }
            }
        }

        // Log summary
        log.info("Inactive Ingress IDs: {}", inactiveIds);
        log.info("Deleted Ingress IDs: {}", deletedIds);
        if (!failedToDelete.isEmpty()) {
            log.warn("Failed To Delete Ingress IDs: {}", failedToDelete);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inactive", inactiveIds);
        result.put("deleted", deletedIds);
        result.put("failedToDelete", failedToDelete);
        result.put("inactiveCount", inactiveIds.size());
        result.put("deletedCount", deletedIds.size());

        return result;
    }

    public CreateIngressResponse createIngress(CreateIngressParams params) throws IOException {
        RoomMetadata metadata = params.getMetadata();
        String roomName = (params.getRoomName() == null || params.getRoomName().isBlank())
                ? generateRoomId()
                : params.getRoomName();

        // Tạo room trước
        roomClient.createRoom(
                roomName, null, null, null,
                objectMapper.writeValueAsString(metadata),
                null, null, null, null
        ).execute();

        // Cấu hình audio/video cho RTMP
        LivekitIngress.IngressVideoOptions videoOptions = LivekitIngress.IngressVideoOptions.newBuilder()
                .setSource(LivekitModels.TrackSource.CAMERA)
                .setPreset(LivekitIngress.IngressVideoEncodingPreset.H264_1080P_30FPS_3_LAYERS)
                .build();

        LivekitIngress.IngressAudioOptions audioOptions = LivekitIngress.IngressAudioOptions.newBuilder()
                .setSource(LivekitModels.TrackSource.MICROPHONE)
                .setPreset(LivekitIngress.IngressAudioEncodingPreset.OPUS_STEREO_96KBPS)
                .build();

        // Gọi API tạo ingress RTMP
        Response<LivekitIngress.IngressInfo> resp = ingressClient.createIngress(
                roomName,
                roomName,
                metadata.getCreatorIdentity(),
                metadata.getCreatorIdentity() + " (via OBS)",
                LivekitIngress.IngressInput.RTMP_INPUT,
                audioOptions,
                videoOptions,
                null,
                true,
                null
        ).execute();

        if (!resp.isSuccessful() || resp.body() == null) {
            String error = "Failed to create ingress: " +
                    resp.code() + " - " +
                    (resp.errorBody() != null ? resp.errorBody().string() : "unknown");
            log.error("[LiveKit] {}", error);
            throw new RuntimeException(error);
        }

        LivekitIngress.IngressInfo ingress = resp.body();
        log.info("[LiveKit] Created ingress id={} for room={}", ingress.getIngressId(), roomName);

        // Tạo token cho viewer
        AccessToken at = new AccessToken(livekitProperties.getApiKey(), livekitProperties.getApiSecret());
        at.setIdentity(metadata.getCreatorIdentity());
        at.addGrants(new RoomJoin(true), new RoomName(roomName), new CanPublish(false),
                new CanSubscribe(true), new CanPublishData(true));

        String authToken = createAuthToken(roomName, metadata.getCreatorIdentity());

        ConnectionDetails conn = new ConnectionDetails();
        conn.setWsUrl(livekitProperties.getUrl());
        conn.setToken(at.toJwt());

        // Convert ingressInfo (protobuf) sang JSON node
        String ingressJson = JsonFormat.printer().print(ingress);
        ObjectNode ingressNode = objectMapper.readValue(ingressJson, ObjectNode.class);

        CreateIngressResponse response = new CreateIngressResponse();
        response.setIngress(ingressNode);
        response.setAuthToken(authToken);
        response.setConnectionDetails(conn);

        return response;
    }
    private String generateRoomId() {
        return "room-" + UUID.randomUUID();
    }
    private String createAuthToken(String roomName, String creatorIdentity) {
        return UUID.randomUUID().toString();
    }
}
