package com.wetube.wetube_service.service.video;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.wetube.wetube_service.dto.response.CustomPageResponse;
import com.wetube.wetube_service.dto.video.VideoFormDetailDto;
import com.wetube.wetube_service.dto.video.VideoUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.wetube.wetube_service.dto.video.VideoDetailResponseDto;
import com.wetube.wetube_service.dto.video.VideoDto;


public interface VideoService {
    VideoDto createVideo(MultipartFile videoFile, MultipartFile thumbnailFile, VideoDto videoDto, UUID authenticatedUserId) throws Exception;

    /**
     * Tải tệp lên và tạo bản nháp (DRAFT, tiêu đề = tên tệp). Chưa đưa vào tìm kiếm, chưa báo người đăng ký;
     * đăng bằng updateVideo. duration / width / height do client đo, dùng khi Cloudinary không trả về.
     */
    VideoDto createDraft(MultipartFile videoFile, Float duration, Integer width, Integer height, UUID userId);

    List<VideoDto> getAllVideo();

    VideoDto addTags(UUID videoId, String hashtagText);


    //search
    List<VideoDto> searchByTitle(String title);

    Page<VideoDto> searchByTitlePaging(String title, int page, int size);

    Page<VideoDto> getAllVideosPaging(int page, int size);

    List<VideoDto> getVideoResult(String query);

    VideoDetailResponseDto getDetailWithRecommend(UUID videoId, UUID userId, boolean admin);

    /** Chủ kênh (và admin) thấy mọi video trừ đã xoá; người khác chỉ thấy video công khai. */
    CustomPageResponse<VideoDto> getVideosByUserId(UUID userId, int page, int size, UUID viewerId, boolean admin);

    CustomPageResponse<VideoDto> getShorts(int page, int size);

    CustomPageResponse<VideoDto> getVideosByTag(String tagName, int page, int size);

    /** Tag dùng nhiều nhất trong các video đang công khai: [{name, count}]. */
    List<Map<String, Object>> getPopularTags(int limit);

    /** Xoá mềm: đặt DELETED, gỡ khỏi playlist/tìm kiếm và xoá file trên Cloudinary. */
    void deleteVideo(UUID videoId);

    /** Ghi nhận một lượt xem (chống đếm trùng). Trả về tổng lượt xem hiện tại và có được tính hay không. */
    Map<String, Object> registerView(UUID videoId, String viewerKey, UUID userId, boolean admin);

    VideoFormDetailDto getVideoDetail(UUID videoId);

    VideoDto updateVideo(UUID videoId, VideoUpdateDto updateDto, MultipartFile thumbnailFile, UUID authenticatedUserId);

    /**
     * Đổi chế độ hiển thị nhiều video của cùng một người (Studio, chọn nhiều hàng). Bỏ qua bản nháp,
     * video bị admin ẩn, đã xoá hoặc đã đúng chế độ. Trả về số video đã đổi.
     */
    int updateVisibility(List<UUID> videoIds, String status, UUID authenticatedUserId);
}
