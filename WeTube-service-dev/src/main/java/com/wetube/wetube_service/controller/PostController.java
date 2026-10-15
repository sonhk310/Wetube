package com.wetube.wetube_service.controller;

import com.wetube.wetube_service.dto.post.PollSummaryDto;
import com.wetube.wetube_service.dto.post.PostDto;
import com.wetube.wetube_service.dto.post.VoteRequestDto;
import com.wetube.wetube_service.service.post.PollVoteService;
import com.wetube.wetube_service.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PollVoteService pollVoteService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<PostDto>> getPostsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(postService.getPostsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody PostDto postDto) {
        return ResponseEntity.ok(postService.createPost(postDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(@PathVariable UUID id, @RequestBody PostDto postDto) {
        return ResponseEntity.ok(postService.updatePost(id, postDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/polls/vote")
    public ResponseEntity<PollSummaryDto> vote(
            @PathVariable UUID postId,
            @RequestBody VoteRequestDto request
    ) {
        request.setPostId(postId);
        return ResponseEntity.ok(pollVoteService.vote(request));
    }
}

