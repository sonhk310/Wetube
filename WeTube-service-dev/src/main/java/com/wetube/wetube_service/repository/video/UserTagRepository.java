package com.wetube.wetube_service.repository.video;


import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.entity.video.UserTag.UserTagId;


public interface UserTagRepository extends JpaRepository<UserTag, UserTagId> {
        List<UserTag> findAllByUserIdOrderByPointDesc(UUID userId);
        List<UserTag> findTop5ByUserIdOrderByPointDesc(UUID userId);

}
