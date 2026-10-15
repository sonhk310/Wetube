package com.wetube.wetube_service.repository.post;

import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.post.PollVote;
import com.wetube.wetube_service.entity.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
    int countByPostId(UUID postId);
    int countByPost_IdAndPollOption_Id(UUID postId, UUID optionId);
    Optional<PollVote> findByPostIdAndUserId(UUID postId, UUID userId);
//    void deleteByPostIdAndUserId(UUID postId, UUID userId);
    void deleteByUserAndPost(AppUser user, Post post);
}