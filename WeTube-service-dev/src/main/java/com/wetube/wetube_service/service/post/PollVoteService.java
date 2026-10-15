package com.wetube.wetube_service.service.post;

import com.wetube.wetube_service.dto.post.PollSummaryDto;
import com.wetube.wetube_service.dto.post.VoteRequestDto;

public interface PollVoteService {
    PollSummaryDto vote(VoteRequestDto request);
}

