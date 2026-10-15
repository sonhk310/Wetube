package com.wetube.wetube_service.repository.post;

import com.wetube.wetube_service.entity.post.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {
    List<PollOption> findByPostId(UUID postId);
    boolean existsByPostId(UUID postId);
    void deleteByPostId(UUID postId);
}

