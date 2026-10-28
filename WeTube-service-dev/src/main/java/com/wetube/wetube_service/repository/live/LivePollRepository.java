package com.wetube.wetube_service.repository.live;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.live.LivePoll;

public interface LivePollRepository extends JpaRepository<LivePoll, UUID> {

    Optional<LivePoll> findFirstByStreamIdAndOpenTrue(UUID streamId);

    List<LivePoll> findByStreamIdOrderByCreatedAtAsc(UUID streamId);

    /** Xoá mọi Poll của các buổi phát này (khi xoá buổi); xoá phiếu trước bằng LivePollVoteRepository. */
    @Modifying
    @Query("delete from LivePoll p where p.streamId in :streamIds")
    void deleteByStreamIds(@Param("streamIds") Collection<UUID> streamIds);
}
