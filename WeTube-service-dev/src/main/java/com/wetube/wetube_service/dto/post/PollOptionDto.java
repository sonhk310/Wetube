package com.wetube.wetube_service.dto.post;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionDto {
    private UUID optionId;
    private String optionText;
    private Integer voteCount;
    private Double percentage;
}
