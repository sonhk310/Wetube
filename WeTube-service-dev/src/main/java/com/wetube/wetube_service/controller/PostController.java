package com.wetube.wetube_service.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.wetube.wetube_service.security.CurrentUser;
import com.wetube.wetube_service.security.OwnershipGuard;
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
    private final OwnershipGuard ownershipGuard;

    @GetMapping("/{userId}")
    public ResponseEntity<List<PostDto>> getPostsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(postService.getPostsByUser(userId));
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody PostDto postDto, @AuthenticationPrincipal Jwt jwt) {
        postDto.setUserId(CurrentUser.id(jwt));
        return ResponseEntity.ok(postService.createPost(postDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDto> updatePost(@PathVariable UUID id, @RequestBody PostDto postDto, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.postOwner(id, jwt);
        postDto.setUserId(CurrentUser.id(jwt));
        return ResponseEntity.ok(postService.updatePost(id, postDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        ownershipGuard.postOwner(id, jwt);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/polls/vote")
    public ResponseEntity<PollSummaryDto> vote(
            @PathVariable UUID postId,
            @RequestBody VoteRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        request.setPostId(postId);
        request.setUserId(CurrentUser.id(jwt));
        return ResponseEntity.ok(pollVoteService.vote(request));
    }
}

