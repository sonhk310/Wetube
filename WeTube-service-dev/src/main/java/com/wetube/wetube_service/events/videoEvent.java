package com.wetube.wetube_service.events;

import com.wetube.wetube_service.entity.video.Video;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class videoEvent {
    public static class VideoCreatedEvent extends ApplicationEvent {
        public VideoCreatedEvent (Video source) { super(source);}
        public Video getVideo() { return(Video) getSource();}
    }
    public static class VideoUpdatedEvent extends ApplicationEvent {
        public VideoUpdatedEvent (Video source) { super(source);}
        public Video getVideo() { return(Video) getSource();}
    }
    public static class VideoDeletedEvent extends ApplicationEvent {
        private final UUID videoId;
        public VideoDeletedEvent (UUID videoId) { super(videoId); this.videoId = videoId;}
        public UUID getVideoId() { return videoId;}
    }
}
