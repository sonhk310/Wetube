package com.wetube.wetube_service.dto.post;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionCreateDto {
    private String optionText;
}
