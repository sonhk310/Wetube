package com.wetube.wetube_service.dto;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private UUID id;
//    private String email;
    private String name;
    private String picture;
    /** @handle của kênh người này (không có "@"); null nếu chưa có kênh. */
    private String handle;
    /** Đang có Premium (huy hiệu cạnh tên ở bình luận); chỉ được điền ở nơi cần huy hiệu, còn lại false. */
    private boolean premium;
}
