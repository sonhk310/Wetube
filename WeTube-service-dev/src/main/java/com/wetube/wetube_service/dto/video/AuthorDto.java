package com.wetube.wetube_service.dto.video;

import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorDto {
    private UUID id;
    private String name;
    private String picture;
}
