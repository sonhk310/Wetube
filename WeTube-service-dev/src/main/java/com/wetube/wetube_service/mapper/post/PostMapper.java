package com.wetube.wetube_service.mapper.post;

import com.wetube.wetube_service.dto.post.PostDto;
import com.wetube.wetube_service.entity.post.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {
    PostDto toDto(Post post);

    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    Post toEntity(PostDto postDto);
}

