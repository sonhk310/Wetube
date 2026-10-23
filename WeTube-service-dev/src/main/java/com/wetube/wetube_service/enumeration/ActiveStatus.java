package com.wetube.wetube_service.enumeration;

public enum ActiveStatus {
    NONE,
    INACTIVE,
    PENDING,
    ACTIVE,
    PUBLIC,
    // Thêm sau cùng: cột videos_status trong MySQL là ENUM, chỉ có thể nối thêm giá trị vào cuối.
    PRIVATE,
    UNLISTED,
    DELETED

}
