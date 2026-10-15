package com.wetube.wetube_service.dto.channel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateCategoryOrderRequest {
    @NotNull
    @NotEmpty
    @Valid
    private List<CategoryOrderDto> categories;

    @Data
    public static class CategoryOrderDto {
        @NotNull
        private UUID id;

        @NotBlank
        private String title;

        @Min(1)
        private Integer orderPosition;
    }
}