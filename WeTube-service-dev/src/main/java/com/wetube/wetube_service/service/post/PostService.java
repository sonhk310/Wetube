package com.wetube.wetube_service.service.post;

import com.wetube.wetube_service.dto.post.PostDto;
import java.util.List;
import java.util.UUID;

public interface PostService {
    List<PostDto> getPostsByUser(UUID userId);
    PostDto getPostById(UUID id);
    PostDto createPost(PostDto postDto);
    PostDto updatePost(UUID id, PostDto postDto);
    void deletePost(UUID id);

}

