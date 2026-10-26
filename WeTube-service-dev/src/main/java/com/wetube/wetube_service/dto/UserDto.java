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
}
