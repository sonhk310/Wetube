package com.wetube.wetube_service.service.analytics;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Lượt xem và thời gian xem theo ngày của từng video (bảng video_daily_stats, không có entity JPA,
 * tạo ở job/VideoDailyStatsTableInitializer). Ghi lỗi thì chỉ log, không làm hỏng việc xem video.
 */
@Slf4j
@Component
public class VideoStatsStore {

    @PersistenceContext
    private EntityManager em;

    /** Cộng dồn vào dòng (video, ngày); transaction riêng để lỗi ở đây không rollback việc đang làm. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void add(UUID videoId, UUID ownerId, LocalDate day, int views, long watchSeconds) {
        try {
            em.createNativeQuery("""
                    INSERT INTO video_daily_stats (video_id, day, owner_id, views, watch_seconds)
                    VALUES (:videoId, :day, :ownerId, :views, :watchSeconds)
                    ON DUPLICATE KEY UPDATE views = views + :views, watch_seconds = watch_seconds + :watchSeconds""")
                    .setParameter("videoId", videoId.toString())
                    .setParameter("day", Date.valueOf(day))
                    .setParameter("ownerId", ownerId.toString())
                    .setParameter("views", views)
                    .setParameter("watchSeconds", watchSeconds)
                    .executeUpdate();
        } catch (Exception e) {
            log.warn("Không ghi được số liệu video {} ngày {}: {}", videoId, day, e.getMessage());
        }
    }

    /** Tổng của các video thuộc chủ kênh theo ngày: mỗi dòng [java.sql.Date day, Number views, Number watchSeconds]. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Object[]> dailyTotals(UUID ownerId, LocalDate from, LocalDate to) {
        return em.createNativeQuery("""
                SELECT day, SUM(views), SUM(watch_seconds) FROM video_daily_stats
                WHERE owner_id = ?1 AND day BETWEEN ?2 AND ?3
                GROUP BY day""")
                .setParameter(1, ownerId.toString())
                .setParameter(2, Date.valueOf(from))
                .setParameter(3, Date.valueOf(to))
                .getResultList();
    }

    /** Ngày đầu tiên có số liệu của chủ kênh. */
    @Transactional(readOnly = true)
    public Optional<LocalDate> firstDay(UUID ownerId) {
        Object first = em.createNativeQuery("SELECT MIN(day) FROM video_daily_stats WHERE owner_id = ?1")
                .setParameter(1, ownerId.toString())
                .getSingleResult();
        if (first instanceof Date d) {
            return Optional.of(d.toLocalDate());
        }
        if (first instanceof LocalDate d) {
            return Optional.of(d);
        }
        return Optional.empty();
    }
}
