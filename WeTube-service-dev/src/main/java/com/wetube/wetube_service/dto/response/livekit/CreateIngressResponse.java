package com.wetube.wetube_service.dto.response.livekit;

import com.fasterxml.jackson.databind.JsonNode;
import livekit.LivekitIngress;
import lombok.Data;

@Data
public class CreateIngressResponse {
    private JsonNode ingress;
    private String authToken;
    private ConnectionDetails connectionDetails;
}
