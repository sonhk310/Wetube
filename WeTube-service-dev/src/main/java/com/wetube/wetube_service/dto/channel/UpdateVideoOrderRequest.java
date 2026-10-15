package com.wetube.wetube_service.dto.channel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateVideoOrderRequest {
    @NotNull
    private UUID categoryId;

    @NotNull
    @NotEmpty
    @Valid
    private List<VideoOrderDto> videos;

    @Data
    public static class VideoOrderDto {
        @NotNull
        private UUID id;

        @Min(1)
        private Integer orderPosition;
    }
}