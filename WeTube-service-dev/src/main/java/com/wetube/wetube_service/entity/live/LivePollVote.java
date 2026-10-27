package com.wetube.wetube_service.entity.live;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "live_poll_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LivePollVote {

    @EmbeddedId
    private LivePollVoteId id;

    @Column(name = "option_index", nullable = false)
    private int optionIndex;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
