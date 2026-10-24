package com.wetube.wetube_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Lịch sử tìm kiếm của người đã đăng nhập (bảng search_history, xem SearchHistoryTableInitializer). */
@Service
public class SearchHistoryService {

    public static final int QUERY_MAX = 200;
    /** Mỗi người giữ tối đa chừng này từ khoá gần nhất. */
    private static final int KEEP_PER_USER = 100;

    @PersistenceContext
    private EntityManager em;

    /** Ghi (hoặc đưa lên đầu) một từ khoá; bỏ qua chuỗi rỗng. */
    @Transactional
    public void record(UUID userId, String rawQuery) {
        String query = normalize(rawQuery);
        if (query.isEmpty()) {
            return;
        }
        String user = userId.toString();
        // Trùng (user, query) thì chỉ cập nhật thời gian; collation không phân biệt hoa thường nên giữ cách viết mới nhất
        em.createNativeQuery("""
                INSERT INTO search_history (user_id, query, searched_at) VALUES (:user, :query, :now)
                ON DUPLICATE KEY UPDATE query = VALUES(query), searched_at = VALUES(searched_at)""")
                .setParameter("user", user)
                .setParameter("query", query)
                .setParameter("now", LocalDateTime.now())
                .executeUpdate();
        em.createNativeQuery("""
                DELETE FROM search_history WHERE user_id = :user AND id NOT IN (
                    SELECT id FROM (
                        SELECT id FROM search_history WHERE user_id = :user ORDER BY searched_at DESC LIMIT :keep
                    ) newest)""")
                .setParameter("user", user)
                .setParameter("keep", KEEP_PER_USER)
                .executeUpdate();
    }

    /** Từ khoá gần nhất trước; prefix khác rỗng thì chỉ lấy từ khoá bắt đầu bằng prefix. */
    @Transactional(readOnly = true)
    public List<String> list(UUID userId, String prefix, int limit) {
        String p = normalize(prefix);
        @SuppressWarnings("unchecked")
        List<String> rows = em.createNativeQuery("""
                SELECT query FROM search_history
                WHERE user_id = :user AND (:prefix = '' OR query LIKE CONCAT(:escaped, '%'))
                ORDER BY searched_at DESC LIMIT :limit""")
                .setParameter("user", userId.toString())
                .setParameter("prefix", p)
                .setParameter("escaped", p.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_"))
                .setParameter("limit", limit)
                .getResultList();
        return rows;
    }

    @Transactional
    public void remove(UUID userId, String rawQuery) {
        em.createNativeQuery("DELETE FROM search_history WHERE user_id = :user AND query = :query")
                .setParameter("user", userId.toString())
                .setParameter("query", normalize(rawQuery))
                .executeUpdate();
    }

    @Transactional
    public void clear(UUID userId) {
        em.createNativeQuery("DELETE FROM search_history WHERE user_id = :user")
                .setParameter("user", userId.toString())
                .executeUpdate();
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String q = raw.strip().replaceAll("\\s+", " ");
        return q.length() > QUERY_MAX ? q.substring(0, QUERY_MAX) : q;
    }
}
