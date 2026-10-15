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
}
