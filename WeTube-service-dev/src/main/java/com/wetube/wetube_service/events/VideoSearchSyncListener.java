package com.wetube.wetube_service.events;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoSearchSyncListener {
    private final VideoMapper videoMapper;
    private final VideoSearchRepository videoSearchRepository;

    @EventListener
    public void onCreate (videoEvent.VideoCreatedEvent event) {
        Video video = event.getVideo();
        videoSearchRepository.save(videoMapper.mapEntityToVideoDocument(video));
    }

    @EventListener
    public void onUpdate (videoEvent.VideoUpdatedEvent event) {
        Video video = event.getVideo();
        videoSearchRepository.save(videoMapper.mapEntityToVideoDocument(video));
    }

    @EventListener
    public void onDelete (videoEvent.VideoDeletedEvent event) {
        videoSearchRepository.deleteById(String.valueOf(event.getVideoId()));
    }

}
