package com.wetube.wetube_service.service.livekit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.LivekitProperties;
import com.wetube.wetube_service.dto.request.livekit.*;
import com.wetube.wetube_service.dto.response.livekit.*;
import com.wetube.wetube_service.mapper.ParticipantMapper;
import io.livekit.server.*;
import livekit.LivekitModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivekitRoomService {

    private final RoomServiceClient roomClient;
    private final ObjectMapper objectMapper;
    private final LivekitProperties livekitProperties;
    private final ParticipantMapper participantMapper;


    public CreateStreamResponse createStream(CreateStreamParams params) throws IOException {
        RoomMetadata metadata = params.getMetadata();
        String roomName = (params.getRoomName() == null || params.getRoomName().isEmpty())
                ? generateRoomId()
                : params.getRoomName();

        AccessToken at = new AccessToken(livekitProperties.getApiKey(), livekitProperties.getApiSecret());
        at.setIdentity(metadata.getCreatorIdentity());
        at.addGrants(new RoomJoin(true), new RoomName(roomName), new CanPublish(true), new CanSubscribe(true));

        log.debug("[LiveKit] Creating room={} with metadata={}", roomName, metadata);

        roomClient.createRoom(
                roomName, null, null, null,
                objectMapper.writeValueAsString(metadata),
                null, null, null, null
        ).execute();

        ConnectionDetails connectionDetails = new ConnectionDetails();
        connectionDetails.setWsUrl(livekitProperties.getUrl());
        connectionDetails.setToken(at.toJwt());

        String authToken = createAuthToken(roomName, metadata.getCreatorIdentity());

        CreateStreamResponse response = new CreateStreamResponse();
        response.setAuthToken(authToken);
        response.setConnectionDetails(connectionDetails);

        log.info("[LiveKit] Created stream for room={} by creator={}", roomName, metadata.getCreatorIdentity());
        return response;
    }

    public JoinStreamResponse joinStream(JoinStreamParams params) throws IOException {
        String roomName = params.getRoomName();
        String identity = params.getIdentity();

        log.debug("[LiveKit] Checking if participant={} already exists in room={}", identity, roomName);

        Response<LivekitModels.ParticipantInfo> resp = roomClient.getParticipant(roomName, identity).execute();
        if (resp.isSuccessful() && resp.body() != null) {
            log.warn("[LiveKit] Participant {} already exists in room={}", identity, roomName);
            throw new RuntimeException("Participant already exists");
        }

        AccessToken at = new AccessToken(livekitProperties.getApiKey(), livekitProperties.getApiSecret());
        at.setIdentity(identity);
        at.addGrants(new RoomJoin(true), new RoomName(roomName), new CanPublish(false),
                new CanSubscribe(true), new CanPublishData(true));

        String authToken = createAuthToken(roomName, identity);

        ConnectionDetails conn = new ConnectionDetails();
        conn.setWsUrl(livekitProperties.getUrl());
        conn.setToken(at.toJwt());

        JoinStreamResponse res = new JoinStreamResponse();
        res.setAuthToken(authToken);
        res.setConnectionDetails(conn);

        log.info("[LiveKit] {} joined stream in room={}", identity, roomName);
        return res;
    }

    public List<RoomInfoRepsonse> listRooms() throws IOException {
        log.debug("[LiveKit] Fetching list of active rooms...");
        Response<List<LivekitModels.Room>> resp =
                roomClient.listRooms(Collections.emptyList()).execute();

        if (!resp.isSuccessful() || resp.body() == null) {
            log.error("[LiveKit] Failed to fetch rooms: {}",
                    (resp.errorBody() != null ? resp.errorBody().string() : "unknown"));
            throw new RuntimeException("Failed to fetch rooms: " +
                    (resp.errorBody() != null ? resp.errorBody().string() : "unknown"));
        }

        List<RoomInfoRepsonse> rooms = resp.body().stream()
                .map(r -> {
                    RoomInfoRepsonse dto = new RoomInfoRepsonse();
                    dto.setRoomId(r.getSid());
                    dto.setName(r.getName());
                    dto.setNumParticipants(r.getNumParticipants());

                    try {
                        if (r.getMetadata() != null && !r.getMetadata().isEmpty()) {
                            RoomMetadata metadata = objectMapper.readValue(r.getMetadata(), RoomMetadata.class);
                            dto.setMetadata(metadata);
                        }
                    } catch (Exception e) {
                        log.warn("[LiveKit] Failed to parse metadata for room {}: {}", r.getName(), e.getMessage());
                    }

                    return dto;
                })
                .toList();

        log.info("[LiveKit] Found {} active rooms", rooms.size());
        return rooms;
    }

    public List<ParticipantDto> listParticipants(String roomName) throws IOException {
        log.debug("[LiveKit] Fetching participants for room={}", roomName);

        Response<List<LivekitModels.ParticipantInfo>> resp = roomClient.listParticipants(roomName).execute();
        if (!resp.isSuccessful() || resp.body() == null) {
            log.error("[LiveKit] Failed to fetch participants for room={}", roomName);
            throw new RuntimeException("Failed to fetch participants for room: " + roomName);
        }

        List<ParticipantDto> participants = participantMapper.toDtoList(resp.body());

        log.info("[LiveKit] Found {} participants in room={}", participants.size(), roomName);
        return participants;
    }

    public void stopStream(StopStreamParams params) throws IOException {
        String roomName = params.getRoomName();
        String identity = params.getIdentity();

        log.debug("[LiveKit] Request to stop room={} by identity={}", roomName, identity);

        Response<List<LivekitModels.Room>> resp = roomClient.listRooms(List.of(roomName)).execute();
        if (!resp.isSuccessful() || resp.body() == null || resp.body().isEmpty()) {
            log.error("[LiveKit] Room {} does not exist", roomName);
            throw new RuntimeException("Room does not exist");
        }

        LivekitModels.Room room = resp.body().get(0);
        RoomMetadata metadata = objectMapper.readValue(room.getMetadata(), RoomMetadata.class);

        if (!metadata.getCreatorIdentity().equals(identity)) {
            log.warn("[LiveKit] User={} tried to stop room={} but is not the creator", identity, roomName);
            throw new RuntimeException("Only the creator can stop the stream");
        }

        roomClient.deleteRoom(roomName).execute();
        log.info("[LiveKit] Room {} stopped by creator={}", roomName, identity);
    }

    private String generateRoomId() {
        return "room-" + UUID.randomUUID();
    }

    private String createAuthToken(String roomName, String creatorIdentity) {
        return UUID.randomUUID().toString();
    }
}
