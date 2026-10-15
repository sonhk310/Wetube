package com.wetube.wetube_service.service.video;

import java.util.List;
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

    List<VideoDto> getAllVideo();

    VideoDto addTags(UUID videoId, String hashtagText);


    //search
    List<VideoDto> searchByTitle(String title);

    Page<VideoDto> searchByTitlePaging(String title, int page, int size);

    Page<VideoDto> getAllVideosPaging(int page, int size);

    List<VideoDto> getVideoResult(String query);

    VideoDetailResponseDto getDetailWithRecommend(UUID videoId, UUID userId);

    CustomPageResponse<VideoDto> getVideosByUserId(UUID userId, int page, int size);

    List<VideoDto> getVideosByTag(String tagName);

    VideoFormDetailDto getVideoDetail(UUID videoId);

    VideoDto updateVideo(UUID videoId, VideoUpdateDto updateDto, MultipartFile thumbnailFile, UUID authenticatedUserId);
}
