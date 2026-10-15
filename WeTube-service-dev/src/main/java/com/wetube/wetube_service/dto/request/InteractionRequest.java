package com.wetube.wetube_service.dto.request;

import java.util.UUID;
import com.wetube.wetube_service.enumeration.InteractionType;
import lombok.Data;

@Data
public class InteractionRequest {
    private UUID userId;
    private UUID videoId;
    private InteractionType type;
}
