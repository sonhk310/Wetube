package com.wetube.wetube_service.dto.room;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchMember {
    private String userId;
    private String username;
    private boolean host;
}
