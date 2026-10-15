package com.wetube.wetube_service.service.post.impl;

import com.wetube.wetube_service.dto.post.PollSummaryDto;
import com.wetube.wetube_service.dto.post.PostDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.interaction.Like;
import com.wetube.wetube_service.entity.post.PollOption;
import com.wetube.wetube_service.entity.post.Post;
import com.wetube.wetube_service.mapper.post.PostMapper;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.post.PollOptionRepository;
import com.wetube.wetube_service.repository.post.PostRepository;
import com.wetube.wetube_service.service.interaction.LikeService;
import com.wetube.wetube_service.service.post.PostPollService;
import com.wetube.wetube_service.service.post.PostService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final LikeService likeService;

    private final PollOptionRepository pollOptionRepository;
    private final PostPollService postPollService;

    @Override
    public List<PostDto> getPostsByUser(UUID userId) {
        return postRepository.findByUserId(userId)
                .stream()
                .map(post -> {
                    PostDto dto = postMapper.toDto(post);
                    dto.setLikeCount(
                            likeService.getLikeInfo(post.getId(), Like.TargetType.POST, userId)
                                    .getLikeCount()
                    );

                    if (pollOptionRepository.existsByPostId(post.getId())) {
                        PollSummaryDto poll = postPollService.getPollSummary(post.getId(), userId);
                        dto.setPoll(poll);
                    }
                    return dto;
                })
                .toList();
    }

    @Override
    public PostDto getPostById(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        PostDto dto = postMapper.toDto(post);
        dto.setLikeCount(
                likeService.getLikeInfo(post.getId(), Like.TargetType.POST, null)
                        .getLikeCount()
        );
        if (pollOptionRepository.existsByPostId(post.getId())) {
            dto.setPoll(postPollService.getPollSummary(post.getId(), null));
        }
        return dto;
    }

    @Override
    @Transactional
    public PostDto createPost(PostDto postDto) {
        AppUser user = userRepository.findById(postDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + postDto.getUserId()));

        Post post = new Post();
        post.setUser(user);

        post.setContent(postDto.getContent());
        post.setImageUrl(postDto.getImageUrl());
        if (post.getCommentCount() == null) {
            post.setCommentCount(0);
        }
        if (post.getLikeCount() == null) {
            post.setLikeCount(0);
        }

        Post saved = postRepository.save(post);

        if (postDto.getPoll() != null
                && postDto.getPoll().getOptions() != null
                && !postDto.getPoll().getOptions().isEmpty()) {
            var options = postDto.getPoll().getOptions().stream()
                    .filter(o -> o.getOptionText() != null && !o.getOptionText().isBlank())
                    .map(o -> PollOption.builder()
                            .post(saved)
                            .optionText(o.getOptionText())
                            .build())
                    .toList();
            if (!options.isEmpty()) {
                pollOptionRepository.saveAll(options);
            }
        }

        // build response
        PostDto dto = postMapper.toDto(saved);
        dto.setLikeCount(likeService.getLikeInfo(saved.getId(), Like.TargetType.POST, null).getLikeCount());
        if (pollOptionRepository.existsByPostId(saved.getId())) {
            dto.setPoll(postPollService.getPollSummary(saved.getId(), null));
        }
        return dto;
    }

    @Override
    @Transactional
    public PostDto updatePost(UUID id, PostDto postDto) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (postDto.getContent() != null) post.setContent(postDto.getContent());
        if (postDto.getImageUrl() != null) post.setImageUrl(postDto.getImageUrl());
        if (postDto.getLikeCount() != null) post.setLikeCount(postDto.getLikeCount());

        Post saved = postRepository.save(post);

        PostDto dto = postMapper.toDto(saved);
        dto.setLikeCount(likeService.getLikeInfo(saved.getId(), Like.TargetType.POST, null).getLikeCount());
        if (pollOptionRepository.existsByPostId(saved.getId())) {
            dto.setPoll(postPollService.getPollSummary(saved.getId(), null));
        }
        return dto;
    }

    @Override
    @Transactional
    public void deletePost(UUID id) {
        if (!postRepository.existsById(id)) {
            throw new RuntimeException("Post not found");
        }

        pollOptionRepository.deleteByPostId(id);
        postRepository.deleteById(id);
    }
}
