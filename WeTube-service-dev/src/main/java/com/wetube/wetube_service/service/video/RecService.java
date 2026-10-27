package com.wetube.wetube_service.service.video;

import java.util.List;
import java.util.UUID;

import com.wetube.wetube_service.dto.video.RecommendVideoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecService {
    /**
     * Gợi ý trang chủ. Cùng {@code seed} thì thứ tự ổn định (cuộn tải thêm không bị xáo), đổi seed (mỗi lần mở
     * trang chủ) thì thứ tự đổi nhẹ.
     */
    Page<RecommendVideoDto> recommendVideos(UUID userId, Pageable pageable, String seed);

    /** Gợi ý bỏ hẳn video đã có trong lịch sử xem (chip "Chưa xem"). */
    Page<RecommendVideoDto> unwatchedVideos(UUID userId, Pageable pageable, String seed);

    /** Còn video chưa xem trong gợi ý hay không (để ẩn chip "Chưa xem" khi hết). */
    boolean hasUnwatched(UUID userId, String seed);

    List<RecommendVideoDto> findTopRankedVideos(UUID userId, int poolSize, int topN);

    /** Bỏ các danh sách gợi ý đã lưu của người dùng, lần tải trang chủ sau sẽ tính lại. */
    void evictRecommendations(UUID userId);
}
