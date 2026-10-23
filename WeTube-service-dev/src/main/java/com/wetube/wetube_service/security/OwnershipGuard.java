package com.wetube.wetube_service.security;

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
