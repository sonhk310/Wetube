package com.wetube.wetube_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Link tải về: flag fl_attachment:<tên tệp> ngay sau /upload/, tên tệp lấy từ tiêu đề đã bỏ dấu. */
class CloudinaryDownloadTest {

    @Test
    void attachmentFlagGoesRightAfterUploadAndKeepsTheRestOfTheLink() {
        assertThat(CloudinaryService.downloadUrl("https://res.cloudinary.com/demo/video/upload/v12/abc.mp4", "Hello"))
                .isEqualTo("https://res.cloudinary.com/demo/video/upload/fl_attachment:Hello/v12/abc.mp4");
        assertThat(CloudinaryService.downloadUrl("https://res.cloudinary.com/demo/video/upload/q_auto,vc_h264/v12/abc.mp4", "Hello"))
                .isEqualTo("https://res.cloudinary.com/demo/video/upload/fl_attachment:Hello/q_auto,vc_h264/v12/abc.mp4");
    }

    @Test
    void aLinkThatIsNotACloudinaryVideoIsLeftAlone() {
        assertThat(CloudinaryService.downloadUrl("https://example.com/a.mp4", "Hello")).isEqualTo("https://example.com/a.mp4");
        assertThat(CloudinaryService.downloadUrl("https://res.cloudinary.com/demo/image/upload/v1/a.jpg", "Hello"))
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/a.jpg");
        assertThat(CloudinaryService.downloadUrl(null, "Hello")).isNull();
    }

    @Test
    void fileNameDropsDiacriticsAndOddCharacters() {
        assertThat(CloudinaryService.downloadFileName("Đường đến thành công: tập 1/2 (HD)")).isEqualTo("Duong_den_thanh_cong_tap_1_2_HD");
        assertThat(CloudinaryService.downloadFileName("a/b\\c?d#e%f&g")).isEqualTo("a_b_c_d_e_f_g");
    }

    @Test
    void emptyOrSymbolOnlyTitleFallsBackToVideo() {
        assertThat(CloudinaryService.downloadFileName(null)).isEqualTo("video");
        assertThat(CloudinaryService.downloadFileName("")).isEqualTo("video");
        assertThat(CloudinaryService.downloadFileName("!!! ???")).isEqualTo("video");
        assertThat(CloudinaryService.downloadFileName("😀😀")).isEqualTo("video");
    }

    @Test
    void longTitleIsCutWithoutATrailingUnderscore() {
        String name = CloudinaryService.downloadFileName("a".repeat(59) + " " + "b".repeat(20));

        assertThat(name).hasSize(59).doesNotEndWith("_");
    }
}
