package com.wetube.wetube_service.mapper.video;

import com.wetube.wetube_service.dto.room.VideoRoom;
import com.wetube.wetube_service.dto.video.TagDto;
import com.wetube.wetube_service.dto.video.VideoDetailDto;
import com.wetube.wetube_service.dto.video.VideoDto;
import com.wetube.wetube_service.dto.video.VideoFormDetailDto;
import com.wetube.wetube_service.dto.video.VideoSubtitleDto;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.entity.video.VideoSubtitle;
import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.search.VideoDocument;
import com.wetube.wetube_service.dto.video.RecommendVideoDto;

import org.mapstruct.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VideoMapper {

    default RecommendVideoDto toRecommendDto(VideoDocument doc) {
        if (doc == null)
            return null;
        RecommendVideoDto dto = new RecommendVideoDto();
        dto.setId(UUID.fromString(doc.getId()));
        dto.setTitle(doc.getTitle());
        dto.setThumbnailUrl(doc.getThumbnailUrl());
        dto.setTotalView(doc.getTotalView());
        dto.setCreateAt(doc.getCreatedAt());
        dto.setDuration(doc.getDuration() != null ? doc.getDuration().longValue() : null);
        dto.setVideoUrl(doc.getVideoUrl());
        dto.setName(doc.getName());
        dto.setPicture(doc.getPicture());
        return dto;
    }

    default List<RecommendVideoDto> toRecommendDtoListFromDoc(List<VideoDocument> docs) {
        return docs.stream().map(this::toRecommendDto).toList();
    }

    @Mapping(target = "tags", source = "videoTags")
    @Mapping(target = "subtitles", expression = "java(mapSubtitles(entity.getSubtitles()))")
    @Mapping(target = "author.id", source = "user.id")
    @Mapping(target = "author.name", source = "user.channel.name")
    @Mapping(target = "author.picture", source = "user.channel.picture")
    VideoDto toDto(Video entity);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "duration", target = "duration")
    @Mapping(source = "thumbnailUrl", target = "thumbnailUrl")
    @Mapping(source = "videoUrl", target = "videoUrl")
    @Mapping(source = "user.name", target = "author")
    @Mapping(source = "user.picture", target = "authorImg")
    @Mapping(source = "totalView", target = "totalView")
    @Mapping(target = "position", ignore = true)
    @Mapping(source = "createdAt", target = "createdAt")
    VideoRoom toRoomDto(Video entity);

    default List<VideoDto> toDtoList(List<Video> entities) {
        if (entities == null || entities.isEmpty())
            return Collections.emptyList();
        return entities.stream().map(this::toDto).toList();
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "thumbnailUrl", source = "thumbnailUrl")
    @Mapping(target = "totalView", source = "totalView")
    @Mapping(target = "createAt", source = "createdAt")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "picture", source = "user.picture")
    @Mapping(target = "duration", source = "duration")
    RecommendVideoDto toRecommendDto(Video entity);

    default Instant map(LocalDateTime value) {
        return value != null ? value.toInstant(ZoneOffset.UTC) : null;
    }

    default List<RecommendVideoDto> toRecommendDtoList(List<Video> entities) {
        if (entities == null || entities.isEmpty())
            return Collections.emptyList();
        return entities.stream().map(this::toRecommendDto).toList();
    }

    @InheritInverseConfiguration(name = "toDto")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "videoTags", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Video toEntity(VideoDto dto);

    @Mapping(target = "subtitles", expression = "java(mapSubtitles(entity.getSubtitles()))")
    @Mapping(target = "createAt", source = "createdAt")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "picture", source = "user.picture")
    VideoDetailDto toDetailDto(Video entity);

    @Named("videoTagToTagDto")
    @Mapping(target = "id", source = "tag.id")
    @Mapping(target = "name", source = "tag.name")
    @Mapping(target = "createdAt", source = "tag.createdAt")
    @Mapping(target = "count", source = "tag.count")
    TagDto mapVideoTagToTagDto(VideoTag vt);

    @IterableMapping(qualifiedByName = "videoTagToTagDto")
    Set<TagDto> map(Set<VideoTag> videoTags);

    @Mapping(target = "id", expression = "java(uuidToString(entity.getId()))")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "tags", expression = "java(mapTagNames(entity.getVideoTags()))")
    VideoDocument mapEntityToVideoDocument(Video entity);

    @Named("uuidToString")
    default String uuidToString(UUID id) {
        return id != null ? id.toString() : null;
    }

    VideoFormDetailDto toFormDetailDto(Video video);

    // Helper to map Set<VideoTag> -> List<String> tag names (lowercase)
    default java.util.List<String> mapTagNames(Set<VideoTag> videoTags) {
        if (videoTags == null || videoTags.isEmpty())
            return java.util.Collections.emptyList();
        return videoTags.stream()
                .filter(vt -> vt.getTag() != null && vt.getTag().getName() != null)
                .map(vt -> vt.getTag().getName().trim().toLowerCase())
                .distinct()
                .toList();
    }

    default List<VideoSubtitleDto> mapSubtitles(List<VideoSubtitle> subtitles) {
        if (subtitles == null)
            return Collections.emptyList();
        return subtitles.stream()
                .map(s -> new VideoSubtitleDto(s.getLanguage(), s.getSubtitleUrl())) // 👈 map đúng field
                .toList();
    }

}
