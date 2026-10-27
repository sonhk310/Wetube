package com.wetube.wetube_service.dto.live;

import java.util.List;

/** Một Poll kèm kết quả; myVote là lựa chọn của người đang xem (null nếu chưa chọn hoặc là khách). */
public record PollView(String id, String question, List<String> options, List<Integer> counts, int total,
                       boolean open, Integer myVote) {
}
