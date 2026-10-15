package com.wetube.wetube_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponseDto<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> SearchResponseDto<T> ok(T data) {
        return new SearchResponseDto<>(true, "success", data);
    }

    public static <T> SearchResponseDto<T> fail(String message) {
        return new SearchResponseDto<>(false, message, null);
    }
}
