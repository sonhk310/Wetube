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
    void transformConstantsAreExactlyWhatCloudinaryGenerates() {
        assertThat(CloudinaryService.THUMB_TRANSFORM).isEqualTo("c_fill,h_720,q_auto,w_1280");
        assertThat(CloudinaryService.SHORT_THUMB_TRANSFORM).isEqualTo("c_fill,h_960,q_auto,w_540");
    }
}
