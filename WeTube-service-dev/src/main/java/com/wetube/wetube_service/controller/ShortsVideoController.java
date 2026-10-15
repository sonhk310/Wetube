//package com.wetube.wetube_service.controller;
//
//import com.wetube.wetube_service.dto.shorts.ShortsDto;
//import com.wetube.wetube_service.service.shorts.ShortsService;
//import lombok.RequiredArgsConstructor;
//import org.owasp.html.PolicyFactory;
//import org.owasp.html.Sanitizers;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/shorts")
//public class ShortsVideoController {
//
//    private final ShortsService shortsService;
//    private static final PolicyFactory SANITIZER = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
//
//    @PostMapping(value = "/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Object> uploadShort(
//            @RequestParam("videoFile") MultipartFile videoFile,
//            @AuthenticationPrincipal Jwt jwt,
//            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
//            @RequestParam("title") String title,
//            @RequestParam(value = "description", required = false) String description,
//            @RequestParam(value = "duration", required = false) Float duration,
//            @RequestParam(value = "tags", required = false) String tags
//    ) {
//        try {
//            UUID userId = UUID.fromString(jwt.getSubject());
//
//            ShortsDto meta = ShortsDto.builder()
//                    .title(SANITIZER.sanitize(title))
//                    .description(description != null ? SANITIZER.sanitize(description) : null)
//                    .duration(duration)
//                    .tags(tags != null ? SANITIZER.sanitize(tags) : null)
//                    .build();
//
//            ShortsDto created = shortService.createShort(videoFile, thumbnailFile, meta, userId);
//
//            return ResponseEntity.status(HttpStatus.CREATED).body(created);
//
//        } catch (IllegalArgumentException e) {
//            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", e.getMessage()));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Server Error", "message", e.getMessage()));
//        }
//    }
//}
