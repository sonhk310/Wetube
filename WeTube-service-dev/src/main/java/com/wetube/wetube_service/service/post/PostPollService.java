package com.wetube.wetube_service.service.post;

import com.wetube.wetube_service.dto.post.PollSummaryDto;

import java.util.UUID;

public interface PostPollService {
    PollSummaryDto getPollSummary(UUID postId, UUID userIdOrNull);
    void vote(UUID postId, UUID optionId, UUID userId);
}


