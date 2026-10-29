package com.wetube.wetube_service.service.video;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.RateLimiter;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.premium.PremiumUserService;

import lombok.RequiredArgsConstructor;

/**
 * Tải video về máy, quyền lợi của Premium. Backend không chuyển dữ liệu video: chỉ kiểm tra quyền rồi trả link
 * Cloudinary có flag tải về, controller chuyển hướng (302) tới đó. Đây là tiện lợi cho Premium chứ không phải
 * khoá chống tải: link MP4 vốn nằm sẵn trong trang xem.
 */
@Service
@RequiredArgsConstructor
public class VideoDownloadService {

    private final PremiumUserService premiumUserService;
    private final VideoAccess videoAccess;
    private final VideoRepository videoRepository;
    private final RateLimiter rateLimiter;

    /** 401 chưa đăng nhập; 403 chưa có Premium; 404 video không có / không xem được; 429 quá 30 lần mỗi giờ. */
    public String downloadUrl(UUID videoId, Jwt jwt) {
        UUID userId = CurrentUser.id(jwt);
        if (premiumUserService.premiumUntil(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tải xuống là quyền lợi của WeTube Premium.");
        }
        videoAccess.requireViewable(videoId, jwt);
        Video video = videoRepository.findById(videoId)
                .filter(v -> v.getVideoUrl() != null && !v.getVideoUrl().isBlank())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy video"));
        rateLimiter.check(RateLimiter.Limit.DOWNLOAD, userId);
        return CloudinaryService.downloadUrl(video.getVideoUrl(), video.getTitle());
    }
}
