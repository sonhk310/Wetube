package com.wetube.wetube_service.service.video.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wetube.wetube_service.dto.request.CreateSubtitleRequest;
import com.wetube.wetube_service.dto.video.SubtitleCue;
import com.wetube.wetube_service.dto.video.SubtitleManageDto;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;
import com.wetube.wetube_service.entity.video.VideoSubtitle;
import com.wetube.wetube_service.enumeration.SubtitleStatus;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.video.VideoSubtitleMapper;
import com.wetube.wetube_service.repository.VideoSubtitleRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.video.VideoSubtitleService;
import com.wetube.wetube_service.utility.WebVtt;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoSubtitleServiceImpl implements VideoSubtitleService {

    static final int MAX_CUES = 3000;
    static final int MAX_CUE_LENGTH = 500;

    private final VideoSubtitleRepository repo;
    private final VideoRepository videoRepo;
    private final VideoSubtitleMapper mapper;
    private final CloudinaryService cloudinaryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public VideoSubtitleDto addSubtitle(UUID videoId, CreateSubtitleRequest req) {
        var video = videoRepo.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId.toString()));

        var entity = new VideoSubtitle();
        entity.setVideo(video);
        entity.setLanguage(req.getLanguage());
        entity.setSubtitleUrl(req.getUrl());
        entity.setStatus(SubtitleStatus.PUBLISHED);

        repo.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public List<VideoSubtitleDto> getSubtitlesByVideo(UUID videoId) {
        return repo.findByVideo_Id(videoId)
                .stream()
                .filter(VideoSubtitle::isPublished)
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubtitleManageDto> list(UUID videoId) {
        return repo.findByVideo_Id(videoId).stream()
                .sorted(Comparator.comparing(VideoSubtitle::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(s -> toManageDto(s, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubtitleManageDto get(UUID videoId, UUID subtitleId) {
        return toManageDto(find(videoId, subtitleId), true);
    }

    @Override
    @Transactional
    public SubtitleManageDto startAi(UUID videoId, String language) {
        var video = videoRepo.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId.toString()));
        List<VideoSubtitle> existing = repo.findByVideo_IdAndLanguage(videoId, language);
        if (existing.stream().anyMatch(s -> s.getStatus() == SubtitleStatus.PROCESSING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phụ đề ngôn ngữ này đang được tạo, vui lòng chờ.");
        }
        repo.deleteAll(existing);

        var entity = new VideoSubtitle();
        entity.setVideo(video);
        entity.setLanguage(language);
        entity.setStatus(SubtitleStatus.PROCESSING);
        entity.setAiGenerated(true);
        entity.setUpdatedAt(LocalDateTime.now());
        return toManageDto(repo.save(entity), false);
    }

    @Override
    @Transactional
    public void completeAi(UUID subtitleId, List<SubtitleCue> cues) {
        // Chủ video có thể đã xoá phụ đề trong lúc AI chạy
        repo.findById(subtitleId).ifPresent(s -> {
            s.setCues(writeCues(cues));
            s.setStatus(SubtitleStatus.DRAFT);
            s.setErrorMessage(null);
            s.setUpdatedAt(LocalDateTime.now());
        });
    }

    @Override
    @Transactional
    public void failAi(UUID subtitleId, String message) {
        repo.findById(subtitleId).ifPresent(s -> {
            s.setStatus(SubtitleStatus.FAILED);
            s.setErrorMessage(message);
            s.setUpdatedAt(LocalDateTime.now());
        });
    }

    @Override
    @Transactional
    public SubtitleManageDto saveCues(UUID videoId, UUID subtitleId, List<SubtitleCue> cues) {
        VideoSubtitle s = find(videoId, subtitleId);
        if (s.getStatus() == SubtitleStatus.PROCESSING || s.getStatus() == SubtitleStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phụ đề chưa tạo xong nên chưa sửa được.");
        }
        List<SubtitleCue> clean = validate(cues);
        s.setCues(writeCues(clean));
        s.setUpdatedAt(LocalDateTime.now());
        // Đang công khai thì người xem nhận bản sửa ngay
        if (s.isPublished()) {
            s.setSubtitleUrl(upload(s, clean));
        }
        return toManageDto(s, true);
    }

    @Override
    @Transactional
    public SubtitleManageDto publish(UUID videoId, UUID subtitleId) {
        VideoSubtitle s = find(videoId, subtitleId);
        if (s.getStatus() == SubtitleStatus.PROCESSING || s.getStatus() == SubtitleStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phụ đề chưa tạo xong nên chưa công khai được.");
        }
        if (s.getCues() != null) {
            List<SubtitleCue> cues = readCues(s.getCues());
            if (cues.isEmpty()) {
                throw new IllegalArgumentException("Phụ đề không có câu nào.");
            }
            s.setSubtitleUrl(upload(s, cues));
        } else if (s.getSubtitleUrl() == null) {
            throw new IllegalArgumentException("Phụ đề không có nội dung.");
        }
        s.setStatus(SubtitleStatus.PUBLISHED);
        s.setUpdatedAt(LocalDateTime.now());
        return toManageDto(s, false);
    }

    @Override
    @Transactional
    public SubtitleManageDto unpublish(UUID videoId, UUID subtitleId) {
        VideoSubtitle s = find(videoId, subtitleId);
        if (s.isPublished()) {
            // Giữ subtitleUrl: phụ đề cũ (không có cues) công khai lại được bằng chính file đó
            s.setStatus(SubtitleStatus.DRAFT);
            s.setUpdatedAt(LocalDateTime.now());
        }
        return toManageDto(s, false);
    }

    @Override
    @Transactional
    public void delete(UUID videoId, UUID subtitleId) {
        repo.delete(find(videoId, subtitleId));
    }

    private VideoSubtitle find(UUID videoId, UUID subtitleId) {
        return repo.findByIdAndVideo_Id(subtitleId, videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtitle", "id", subtitleId.toString()));
    }

    private String upload(VideoSubtitle s, List<SubtitleCue> cues) {
        return cloudinaryService.uploadSubtitle(WebVtt.build(cues),
                "subtitles/" + s.getVideo().getId() + "_" + s.getLanguage() + ".vtt");
    }

    /** Bỏ câu trống, kiểm tra giờ, xếp theo giờ bắt đầu. */
    static List<SubtitleCue> validate(List<SubtitleCue> cues) {
        if (cues == null) {
            throw new IllegalArgumentException("Thiếu danh sách câu phụ đề.");
        }
        if (cues.size() > MAX_CUES) {
            throw new IllegalArgumentException("Phụ đề có tối đa " + MAX_CUES + " câu.");
        }
        List<SubtitleCue> clean = new ArrayList<>();
        for (SubtitleCue cue : cues) {
            String text = cue.getText() == null ? "" : cue.getText().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() > MAX_CUE_LENGTH) {
                throw new IllegalArgumentException("Mỗi câu phụ đề có tối đa " + MAX_CUE_LENGTH + " ký tự.");
            }
            if (!Double.isFinite(cue.getStart()) || !Double.isFinite(cue.getEnd())
                    || cue.getStart() < 0 || cue.getEnd() <= cue.getStart()) {
                throw new IllegalArgumentException(
                        "Câu lúc " + WebVtt.time(Math.max(0, cue.getStart())) + ": giờ kết thúc phải sau giờ bắt đầu.");
            }
            clean.add(new SubtitleCue(cue.getStart(), cue.getEnd(), text, cue.getOriginal()));
        }
        clean.sort(Comparator.comparingDouble(SubtitleCue::getStart));
        return clean;
    }

    private SubtitleManageDto toManageDto(VideoSubtitle s, boolean withCues) {
        List<SubtitleCue> cues = s.getCues() == null ? null : readCues(s.getCues());
        return SubtitleManageDto.builder()
                .id(s.getId())
                .language(s.getLanguage())
                .status(s.getStatus() == null ? SubtitleStatus.PUBLISHED : s.getStatus())
                .aiGenerated(Boolean.TRUE.equals(s.getAiGenerated()))
                .errorMessage(s.getErrorMessage())
                .subtitleUrl(s.getSubtitleUrl())
                .cueCount(cues == null ? 0 : cues.size())
                .updatedAt(s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt())
                .cues(withCues ? cues : null)
                .build();
    }

    private String writeCues(List<SubtitleCue> cues) {
        try {
            return objectMapper.writeValueAsString(cues);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize subtitle cues", e);
        }
    }

    private List<SubtitleCue> readCues(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<SubtitleCue>>() { });
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read subtitle cues", e);
        }
    }
}
