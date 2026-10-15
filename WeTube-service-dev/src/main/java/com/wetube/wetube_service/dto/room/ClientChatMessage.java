package com.wetube.wetube_service.dto.room;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClientChatMessage {
    private String type;
    private String sender;
    private String content;
}
