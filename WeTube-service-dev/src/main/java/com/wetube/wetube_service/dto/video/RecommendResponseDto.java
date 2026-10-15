package com.wetube.wetube_service.dto.video;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendResponseDto {
    private List<RecommendVideoDto> video;
    private List<TagDto> tags;
}
