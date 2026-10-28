package com.wetube.wetube_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cloudinary.Cloudinary;

/** Link ảnh thu nhỏ: đủ lớn để nét, và khớp với phần biến đổi mà ThumbnailSizeInitializer ghi vào link cũ. */
class CloudinaryThumbnailTest {

    private final Cloudinary cloudinary = new Cloudinary("cloudinary://key:secret@demo");

    @Test
    void horizontalThumbnailIs16by9AndLargeEnoughForWideCards() {
        String url = CloudinaryService.thumbnailUrl(cloudinary, "abc", false);

        assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/" + CloudinaryService.THUMB_TRANSFORM + "/abc.webp");
        assertThat(CloudinaryService.THUMB_WIDTH * 9).isEqualTo(CloudinaryService.THUMB_HEIGHT * 16);
        assertThat(CloudinaryService.THUMB_WIDTH).isGreaterThanOrEqualTo(1280);
    }

    @Test
    void shortThumbnailIs9by16() {
        String url = CloudinaryService.thumbnailUrl(cloudinary, "abc", true);

        assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/" + CloudinaryService.SHORT_THUMB_TRANSFORM + "/abc.webp");
        assertThat(CloudinaryService.SHORT_THUMB_WIDTH * 16).isEqualTo(CloudinaryService.SHORT_THUMB_HEIGHT * 9);
    }

    @Test
    void aVideoWithoutAThumbnailGetsAFrameOfItself() {
        assertThat(CloudinaryService.videoFrameUrl("https://res.cloudinary.com/demo/video/upload/q_auto,vc_h264/v12/rec.mp4"))
                .isEqualTo("https://res.cloudinary.com/demo/video/upload/so_1,w_1280,h_720,c_fill/q_auto,vc_h264/v12/rec.jpg");
        assertThat(CloudinaryService.videoFrameUrl("https://res.cloudinary.com/demo/video/upload/v12/rec.mp4?x=1"))
                .isEqualTo("https://res.cloudinary.com/demo/video/upload/so_1,w_1280,h_720,c_fill/v12/rec.jpg");
    }

    @Test
    void noFrameForAnythingThatIsNotACloudinaryVideo() {
        assertThat(CloudinaryService.videoFrameUrl(null)).isNull();
        assertThat(CloudinaryService.videoFrameUrl("https://pub.example/live/a.mp4")).isNull();
        assertThat(CloudinaryService.videoFrameUrl("https://res.cloudinary.com/demo/image/upload/v1/a.jpg")).isNull();
    }

    @Test
    void transformConstantsAreExactlyWhatCloudinaryGenerates() {
        assertThat(CloudinaryService.THUMB_TRANSFORM).isEqualTo("c_fill,h_720,q_auto,w_1280");
        assertThat(CloudinaryService.SHORT_THUMB_TRANSFORM).isEqualTo("c_fill,h_960,q_auto,w_540");
    }
}
