package com.wetube.wetube_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.cloudinary.Cloudinary;

class CloudinaryPlaybackTest {

    private final Cloudinary cloudinary = new Cloudinary("cloudinary://key:secret@demo");

    private static Map<String, Object> upload(String format, Map<String, Object> video) {
        return video == null ? Map.of("format", format) : Map.of("format", format, "video", video);
    }

    @Test
    void mp4H264IsPlayedAsUploaded() {
        assertThat(CloudinaryService.playsAsUploaded(upload("mp4", Map.of("codec", "h264", "pix_format", "yuv420p"))))
                .isTrue();
        assertThat(CloudinaryService.playsAsUploaded(upload("mp4", Map.of("codec", "h264")))).isTrue();
    }

    @Test
    void otherCodecsContainersOrUnknownAreTranscoded() {
        assertThat(CloudinaryService.playsAsUploaded(upload("mp4", Map.of("codec", "hevc")))).isFalse();
        assertThat(CloudinaryService.playsAsUploaded(upload("mov", Map.of("codec", "h264")))).isFalse();
        assertThat(CloudinaryService.playsAsUploaded(upload("webm", Map.of("codec", "vp9")))).isFalse();
        // 10-bit H.264 trình duyệt không phát được
        assertThat(CloudinaryService.playsAsUploaded(upload("mp4", Map.of("codec", "h264", "pix_format", "yuv420p10le"))))
                .isFalse();
        // Cloudinary không trả thông tin codec: chuyển mã như trước cho chắc
        assertThat(CloudinaryService.playsAsUploaded(upload("mp4", null))).isFalse();
    }

    @Test
    void playbackUrlIsOriginalOrTranscoded() {
        assertThat(CloudinaryService.playbackUrl(cloudinary, "abc", true))
                .isEqualTo("https://res.cloudinary.com/demo/video/upload/abc.mp4");
        assertThat(CloudinaryService.playbackUrl(cloudinary, "abc", false))
                .isEqualTo("https://res.cloudinary.com/demo/video/upload/q_auto,vc_h264/abc.mp4");
    }

    @Test
    void eagerTranscodeMatchesPlaybackUrl() {
        // q_auto,vc_h264 + đuôi mp4: đúng bản mà link phát xin
        assertThat(CloudinaryService.transcodeEager().generate()).isEqualTo("q_auto,vc_h264/mp4");
    }

    @Test
    void thumbnailIsLandscapeForVideosAndPortraitForShorts() {
        assertThat(CloudinaryService.thumbnailUrl(cloudinary, "img", false))
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/c_fill,h_720,q_auto,w_1280/img.webp");
        // Shorts: khung dọc 9:16, không cắt ảnh dọc thành ảnh ngang
        assertThat(CloudinaryService.thumbnailUrl(cloudinary, "img", true))
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/c_fill,h_960,q_auto,w_540/img.webp");
    }

    @Test
    void publicIdStillReadableFromBothUrls() {
        assertThat(CloudinaryService.publicIdFromDeliveryUrl("https://res.cloudinary.com/demo/video/upload/abc.mp4"))
                .isEqualTo("abc");
        assertThat(CloudinaryService.publicIdFromDeliveryUrl(
                "https://res.cloudinary.com/demo/video/upload/q_auto,vc_h264/abc.mp4")).isEqualTo("abc");
    }
}
