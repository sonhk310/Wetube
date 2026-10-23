package com.wetube.wetube_service.security;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Quy tắc hiển thị video dùng chung cho mọi nơi:
 * ACTIVE/PUBLIC = có trong danh sách và ai cũng xem được; UNLISTED = không có trong danh sách nhưng xem được
 * khi có link; PRIVATE và INACTIVE (admin ẩn) = chỉ chủ video / admin xem; DELETED = không ai xem được.
 */
@Component
public class VideoAccess {

    /** Trạng thái được hiển thị trong danh sách, tìm kiếm, gợi ý. */
    public static final List<ActiveStatus> LISTED = List.of(ActiveStatus.ACTIVE, ActiveStatus.PUBLIC);

    @PersistenceContext
    private EntityManager em;

    public static boolean isListed(ActiveStatus status) {
        return status == ActiveStatus.ACTIVE || status == ActiveStatus.PUBLIC;
    }

    public static boolean isOwner(Video video, UUID viewerId) {
        return viewerId != null && video.getUser() != null && viewerId.equals(video.getUser().getId());
    }

    /** Người xem này có mở được trang xem video không. */
    public static boolean canView(Video video, UUID viewerId, boolean admin) {
        ActiveStatus status = video.getVideosStatus();
        if (status == ActiveStatus.DELETED) {
            return false;
        }
        if (isListed(status) || status == ActiveStatus.UNLISTED) {
            return true;
        }
        return admin || isOwner(video, viewerId);
    }

    /** Video có nên hiện trong danh sách (playlist, kênh...) của người xem này không. */
    public static boolean canSeeInList(Video video, UUID viewerId, boolean admin) {
        if (!canView(video, viewerId, admin)) {
            return false;
        }
        return isListed(video.getVideosStatus()) || admin || isOwner(video, viewerId);
    }

    /** 404 nếu người gọi không được xem video (không lộ sự tồn tại của video riêng tư). */
    @Transactional(readOnly = true)
    public void requireViewable(UUID videoId, Jwt jwt) {
        List<Object[]> rows = em.createQuery(
                        "select v.videosStatus, v.user.id from Video v where v.id = :id", Object[].class)
                .setParameter("id", videoId).setMaxResults(1).getResultList();
        if (rows.isEmpty()) {
            throw notFound();
        }
        ActiveStatus status = (ActiveStatus) rows.get(0)[0];
        UUID ownerId = (UUID) rows.get(0)[1];
        if (status == ActiveStatus.DELETED) {
            throw notFound();
        }
        if (isListed(status) || status == ActiveStatus.UNLISTED) {
            return;
        }
        boolean allowed = jwt != null && (CurrentUser.isAdmin(jwt) || ownerId.toString().equals(jwt.getSubject()));
        if (!allowed) {
            throw notFound();
        }
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy video");
    }
}
