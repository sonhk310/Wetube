package com.wetube.wetube_service.enumeration;

/** Vòng đời buổi phát trực tiếp: CREATED (chưa có tín hiệu) → LIVE → ENDED; CANCELED nếu không bao giờ lên sóng. */
public enum LiveStatus {
    CREATED,
    LIVE,
    ENDED,
    CANCELED
}
