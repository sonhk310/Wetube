package com.wetube.wetube_service.repository.video;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wetube.wetube_service.entity.video.UserTopic;
import com.wetube.wetube_service.entity.video.UserTopic.UserTopicId;

public interface UserTopicRepository extends JpaRepository<UserTopic, UserTopicId> {
    List<UserTopic> findAllByUserIdOrderByPointDesc(UUID userId);
}
