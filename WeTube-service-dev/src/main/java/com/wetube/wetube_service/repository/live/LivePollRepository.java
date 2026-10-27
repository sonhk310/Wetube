package com.wetube.wetube_service.repository.live;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wetube.wetube_service.entity.live.LivePoll;

public interface LivePollRepository extends JpaRepository<LivePoll, UUID> {

    Optional<LivePoll> findFirstByStreamIdAndOpenTrue(UUID streamId);

    List<LivePoll> findByStreamIdOrderByCreatedAtAsc(UUID streamId);
}
