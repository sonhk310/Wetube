package com.wetube.wetube_service.dto.post;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollCreateDto {
    private List<PollOptionCreateDto> options;
}
