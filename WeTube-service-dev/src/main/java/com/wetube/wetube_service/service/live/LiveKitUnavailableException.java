package com.wetube.wetube_service.service.live;

/** LiveKit không dùng được: lỗi mạng, lỗi máy chủ hoặc hết hạn mức của gói (quota). */
public class LiveKitUnavailableException extends RuntimeException {

    private final boolean quota;

    public LiveKitUnavailableException(String message, boolean quota) {
        super(message);
        this.quota = quota;
    }

    public LiveKitUnavailableException(String message, boolean quota, Throwable cause) {
        super(message, cause);
        this.quota = quota;
    }

    /** True khi LiveKit từ chối vì hết hạn mức (HTTP 403/429 hoặc nội dung báo quota / limit). */
    public boolean isQuota() {
        return quota;
    }
}
