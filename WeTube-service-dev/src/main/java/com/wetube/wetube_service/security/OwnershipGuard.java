package com.wetube.wetube_service.security;

import com.wetube.wetube_service.entity.interaction.Comment;
import com.wetube.wetube_service.enumeration.PlaylistType;
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
 * Kiểm tra người gọi có phải chủ sở hữu của tài nguyên không (admin được bỏ qua).
 * Trả 401 nếu chưa đăng nhập, 404 nếu tài nguyên không tồn tại, 403 nếu không phải chủ.
 */
@Component
public class OwnershipGuard {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public void commentOwner(UUID commentId, Jwt jwt) {
        check(single("select c.user.id from Comment c where c.id = :id", commentId), jwt);
    }

    /** Xoá bình luận: người viết, chủ video chứa bình luận, hoặc admin. */
    @Transactional(readOnly = true)
    public void commentRemovable(UUID commentId, Jwt jwt) {
        UUID me = CurrentUser.id(jwt);
        UUID author = single("select c.user.id from Comment c where c.id = :id", commentId);
        if (author != null && author.equals(me)) {
            return;
        }
        UUID videoOwner = commentVideoOwnerId(commentId);
        if (videoOwner != null && videoOwner.equals(me)) {
            return;
        }
        if (author == null && videoOwner == null
                && single("select c.id from Comment c where c.id = :id", commentId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy");
        }
        if (!CurrentUser.isAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền với nội dung này");
        }
    }

    /**
     * Ghim / thả tim: chỉ đúng chủ video chứa bình luận (admin cũng không, vì tim và ghim hiển thị là
     * của chủ kênh).
     */
    @Transactional(readOnly = true)
    public void commentVideoOwner(UUID commentId, Jwt jwt) {
        UUID me = CurrentUser.id(jwt);
        if (single("select c.id from Comment c where c.id = :id", commentId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy");
        }
        UUID videoOwner = commentVideoOwnerId(commentId);
        if (videoOwner == null || !videoOwner.equals(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ video mới làm được việc này");
        }
    }

    private UUID commentVideoOwnerId(UUID commentId) {
        List<UUID> rows = em.createQuery("""
                        select v.user.id from Video v, Comment c
                        where c.id = :id and c.targetType = :video and v.id = c.targetId""", UUID.class)
                .setParameter("id", commentId)
                .setParameter("video", Comment.TargetType.VIDEO)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Transactional(readOnly = true)
    public void postOwner(UUID postId, Jwt jwt) {
        check(single("select p.user.id from Post p where p.id = :id", postId), jwt);
    }

    @Transactional(readOnly = true)
    public void playlistOwner(UUID playlistId, Jwt jwt) {
        check(single("select p.user.id from Playlist p where p.id = :id", playlistId), jwt);
    }

    /** Chủ kênh = user đang gắn với kênh đó. */
    @Transactional(readOnly = true)
    public void channelOwner(UUID channelId, Jwt jwt) {
        check(single("select u.id from AppUser u where u.channel.id = :id", channelId), jwt);
    }

    /** Playlist công khai thì ai cũng xem được; còn lại chỉ chủ hoặc admin (404 nếu không có quyền, để không lộ sự tồn tại). */
    @Transactional(readOnly = true)
    public void playlistReadable(UUID playlistId, Jwt jwt) {
        List<Object[]> rows = em.createQuery(
                        "select p.user.id, p.playlistType, p.privacy from Playlist p where p.id = :id", Object[].class)
                .setParameter("id", playlistId).setMaxResults(1).getResultList();
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy");
        }
        Object[] row = rows.get(0);
        if (isPublicPlaylist((PlaylistType) row[1], (String) row[2])) {
            return;
        }
        boolean allowed = jwt != null && (row[0].toString().equals(jwt.getSubject()) || CurrentUser.isAdmin(jwt));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy");
        }
    }

    public static boolean isPublicPlaylist(PlaylistType type, String privacy) {
        return type != PlaylistType.HISTORY && type != PlaylistType.LIKED && type != PlaylistType.WATCH_LATER
                && !"private".equalsIgnoreCase(privacy);
    }

    @Transactional(readOnly = true)
    public void videoOwner(UUID videoId, Jwt jwt) {
        check(single("select v.user.id from Video v where v.id = :id", videoId), jwt);
    }

    @Transactional(readOnly = true)
    public void tierOwner(UUID tierId, Jwt jwt) {
        check(single(
                "select u.id from AppUser u where u.channel.id = (select t.channel.id from MembershipTier t where t.id = :id)",
                tierId), jwt);
    }

    private UUID single(String jpql, UUID id) {
        List<UUID> rows = em.createQuery(jpql, UUID.class).setParameter("id", id).setMaxResults(1).getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void check(UUID ownerId, Jwt jwt) {
        UUID me = CurrentUser.id(jwt);
        if (ownerId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy");
        }
        if (!ownerId.equals(me) && !CurrentUser.isAdmin(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền với nội dung này");
        }
    }
}
