package com.wetube.wetube_service.job;

import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.repository.video.VideoSearchRepository;
import com.wetube.wetube_service.search.VideoDocument;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Component
public class ElasticBootstrapLoader implements CommandLineRunner {

    private final VideoRepository videoRepository;
    private final VideoSearchRepository videoSearchRepository;

    public ElasticBootstrapLoader(VideoRepository videoRepository,
                                  VideoSearchRepository videoSearchRepository) {
        this.videoRepository = videoRepository;
        this.videoSearchRepository = videoSearchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(String... args) {
        System.out.println("🚀 Bắt đầu sync dữ liệu cũ MySQL → Elasticsearch...");

        var videos = videoRepository.findAll();

        var docs = videos.stream().map(v -> {
            var tags = v.getVideoTags().stream()
                    .map(VideoTag::getTag)        
                    .map(tag -> tag.getName())    
                    .collect(Collectors.toList());

            return new VideoDocument(
                    v.getId().toString(),
                    v.getTitle(),
                    v.getDescription(),
                    v.getUser().getId().toString(),
                    tags,
                    java.util.List.of(),          
                    v.getCreatedAt().atZone(ZoneOffset.UTC).toInstant(),
                    v.getThumbnailUrl(),
                    v.getVideoUrl(),
                    v.getTotalView(),
                    v.getDuration(),
                    v.getUser().getName(),
                    v.getUser().getPicture()
            );
        }).toList();

        videoSearchRepository.saveAll(docs);

        System.out.println("✅ Sync xong " + docs.size() + " video(s) vào Elasticsearch!");
    }
}
