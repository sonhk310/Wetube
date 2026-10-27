package com.wetube.wetube_service.job;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:selfsub;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SelfSubscriptionCleanupTest {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager txManager;
    @Autowired
    EntityManager em;

    @Test
    void removesOnlySelfSubscriptionsAndRecountsThatChannel() {
        // A có kênh CA, B có kênh CB. A tự đăng ký CA, B đăng ký CA, A đăng ký CB.
        jdbc.update("INSERT INTO channels (id, total_subscribers, total_videos, total_views, revenue) VALUES ('CA', 2, 0, 0, 0)");
        jdbc.update("INSERT INTO channels (id, total_subscribers, total_videos, total_views, revenue) VALUES ('CB', 1, 0, 0, 0)");
        jdbc.update("INSERT INTO users (id, email, channel_id) VALUES ('A', 'a@test', 'CA')");
        jdbc.update("INSERT INTO users (id, email, channel_id) VALUES ('B', 'b@test', 'CB')");
        jdbc.update("INSERT INTO tiers (id, channel_id, is_default, price) VALUES ('TA', 'CA', 1, 0)");
        jdbc.update("INSERT INTO tiers (id, channel_id, is_default, price) VALUES ('TB', 'CB', 1, 0)");
        jdbc.update("INSERT INTO subscriptions (user_id, tier_id) VALUES ('A', 'TA')");
        jdbc.update("INSERT INTO subscriptions (user_id, tier_id) VALUES ('B', 'TA')");
        jdbc.update("INSERT INTO subscriptions (user_id, tier_id) VALUES ('A', 'TB')");

        SelfSubscriptionCleanup cleanup = new SelfSubscriptionCleanup(txManager);
        org.springframework.test.util.ReflectionTestUtils.setField(cleanup, "em", em);
        cleanup.run();
        // Chạy lại lần hai không đổi gì
        cleanup.run();

        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM subscriptions", Integer.class));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM subscriptions WHERE user_id = 'A' AND tier_id = 'TA'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT total_subscribers FROM channels WHERE id = 'CA'", Integer.class));
        // Kênh không có tự đăng ký giữ nguyên số đếm
        assertEquals(1, jdbc.queryForObject("SELECT total_subscribers FROM channels WHERE id = 'CB'", Integer.class));
    }
}
