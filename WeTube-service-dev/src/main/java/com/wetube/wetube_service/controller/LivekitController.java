package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.request.livekit.*;
import com.wetube.wetube_service.dto.response.livekit.*;
import com.wetube.wetube_service.service.livekit.LivekitIngressService;
import com.wetube.wetube_service.service.livekit.LivekitRoomService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;


@RestController
@RequestMapping("/api/livekit")
@AllArgsConstructor
public class LivekitController {

    private final LivekitRoomService livekitRoomService;
    private final LivekitIngressService livekitIngressService;

    @PostMapping("/create-stream")
    public CreateStreamResponse createStream(@RequestBody CreateStreamParams params) throws IOException {
        return livekitRoomService.createStream(params);
    }

    @PostMapping("/join-stream")
    public JoinStreamResponse joinStream(@RequestBody JoinStreamParams params) throws IOException {
        return livekitRoomService.joinStream(params);
    }
    @GetMapping("/rooms")
    public ResponseEntity<List<RoomInfoRepsonse>> listRooms() throws IOException {
        return ResponseEntity.ok(livekitRoomService.listRooms());
    }

    @GetMapping("/rooms/participants/{roomName}")
    public List<ParticipantDto> listParticipants(@PathVariable String roomName) throws IOException {
        return livekitRoomService.listParticipants(roomName);
    }

    @PostMapping("/stop-stream")
    public void stopStream(@RequestBody StopStreamParams params) throws IOException {
        livekitRoomService.stopStream(params);
    }

    @PostMapping("/create-ingress")
    public CreateIngressResponse createIngress(@RequestBody CreateIngressParams params) throws IOException {
        livekitIngressService.deleteInactiveIngress();
        return livekitIngressService.createIngress(params);
    }

    @DeleteMapping("/ingress/inactive")
    public ResponseEntity<?> deleteInactiveIngress() throws IOException {
        return ResponseEntity.ok(livekitIngressService.deleteInactiveIngress());
    }
}

