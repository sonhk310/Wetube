package com.wetube.wetube_service.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRoom {
    private String id;
    private String title;
    private float duration;
    private String thumbnailUrl;
    private String videoUrl;
    private String author;
    private String authorImg;
    private long totalView;
    private int position;
    private String createdAt;
}
