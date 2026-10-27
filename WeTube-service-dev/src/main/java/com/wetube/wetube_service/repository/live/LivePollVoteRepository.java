package com.wetube.wetube_service.repository.live;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wetube.wetube_service.entity.live.LivePollVote;
import com.wetube.wetube_service.entity.live.LivePollVoteId;

public interface LivePollVoteRepository extends JpaRepository<LivePollVote, LivePollVoteId> {

    /** Mỗi dòng là [optionIndex, số phiếu]. */
    @Query("select v.optionIndex, count(v) from LivePollVote v where v.id.pollId = :pollId group by v.optionIndex")
    List<Object[]> countByOption(@Param("pollId") UUID pollId);
}
