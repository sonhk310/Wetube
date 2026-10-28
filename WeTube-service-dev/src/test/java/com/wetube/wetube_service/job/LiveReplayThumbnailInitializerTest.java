package com.wetube.wetube_service.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;

/** Chỉ video xem lại của buổi phát, chưa có ảnh thu nhỏ và là video Cloudinary mới được gán khung hình. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.datasource.url=jdbc:h2:mem:livereplaythumb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LiveReplayThumbnailInitializerTest {

    private static final String CLOUD_VIDEO = "https://res.cloudinary.com/x/video/upload/q_auto,vc_h264/v1/rec.mp4";
    private static final String FRAME = "https://res.cloudinary.com/x/video/upload/so_1,w_1280,h_720,c_fill/q_auto,vc_h264/v1/rec.jpg";

    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    EntityManager em;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void tables() {
        jdbc.execute("DROP TABLE IF EXISTS videos");
        jdbc.execute("DROP TABLE IF EXISTS live_streams");
        jdbc.execute("CREATE TABLE videos (id VARCHAR(36) PRIMARY KEY, video_url VARCHAR(500), thumbnail_url VARCHAR(500))");
        jdbc.execute("CREATE TABLE live_streams (id VARCHAR(36) PRIMARY KEY, replay_video_id VARCHAR(36))");
    }

    private void video(String id, String url, String thumbnail, boolean replay) {
        jdbc.update("INSERT INTO videos VALUES (?, ?, ?)", id, url, thumbnail);
        if (replay) {
            jdbc.update("INSERT INTO live_streams VALUES (?, ?)", "s-" + id, id);
        }
    }

    private String thumbnailOf(String id) {
        return jdbc.queryForObject("SELECT thumbnail_url FROM videos WHERE id = ?", String.class, id);
    }

    private void run() {
        LiveReplayThumbnailInitializer initializer = new LiveReplayThumbnailInitializer();
        ReflectionTestUtils.setField(initializer, "em", em);
        // @Transactional của runner không có tác dụng khi tự dựng đối tượng: mở transaction JPA ở đây
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> initializer.run());
    }

    @Test
    void aReplayWithoutAThumbnailGetsAFrameOfTheRecording() {
        video("a", CLOUD_VIDEO, null, true);
        video("b", CLOUD_VIDEO, "", true);

        run();

        assertThat(thumbnailOf("a")).isEqualTo(FRAME);
        assertThat(thumbnailOf("b")).isEqualTo(FRAME);
    }

    @Test
    void anExistingThumbnailAnOrdinaryVideoAndANonCloudinaryFileAreLeftAlone() {
        video("own", CLOUD_VIDEO, "https://res.cloudinary.com/x/image/upload/t.jpg", true);
        video("plain", CLOUD_VIDEO, null, false);
        video("elsewhere", "https://pub.example/live/a.mp4", null, true);

        run();

        assertThat(thumbnailOf("own")).isEqualTo("https://res.cloudinary.com/x/image/upload/t.jpg");
        assertThat(thumbnailOf("plain")).isNull();
        assertThat(thumbnailOf("elsewhere")).isNull();
    }

    @Test
    void runningItTwiceChangesNothingMore() {
        video("a", CLOUD_VIDEO, null, true);

        run();
        run();

        assertThat(thumbnailOf("a")).isEqualTo(FRAME);
    }
}
