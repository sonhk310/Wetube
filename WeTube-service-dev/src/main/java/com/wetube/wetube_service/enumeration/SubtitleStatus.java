package com.wetube.wetube_service.enumeration;

/**
 * Phụ đề: PROCESSING (AI đang tạo) → DRAFT (chỉ chủ video thấy, sửa được) → PUBLISHED (người xem thấy);
 * FAILED nếu AI lỗi (lý do ở errorMessage). Cột lưu VARCHAR; null (dòng cũ trước E2) coi như PUBLISHED.
 */
public enum SubtitleStatus {
    PROCESSING,
    DRAFT,
    PUBLISHED,
    FAILED
}
