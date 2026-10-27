package com.wetube.wetube_service.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.wetube.wetube_service.entity.live.LiveStream;

/** Xung đột khoá lạc quan (@Version) phải thành 409 có thông báo tiếng Việt, không phải 500. */
class GlobalExceptionHandlerLockTest {

    @Test
    void optimisticLockConflictIs409WithAVietnameseMessage() {
        ResponseEntity<Object> response = new GlobalExceptionHandler()
                .handleOptimisticLock(new ObjectOptimisticLockingFailureException(LiveStream.class, "id"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(((Map<?, ?>) response.getBody()).get("message").toString()).contains("thử lại");
    }
}
