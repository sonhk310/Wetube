package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.wetube.wetube_service.entity.video.Tag;
import com.wetube.wetube_service.entity.video.UserTag;
import com.wetube.wetube_service.entity.video.UserTopic;
import com.wetube.wetube_service.entity.video.VideoTag;
import com.wetube.wetube_service.repository.video.UserTagRepository;
import com.wetube.wetube_service.repository.video.UserTopicRepository;
import com.wetube.wetube_service.repository.video.VideoTagRepository;
import com.wetube.wetube_service.service.interaction.impl.UserInteractionServiceImpl;
import com.wetube.wetube_service.enumeration.InteractionType;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.entity.video.Video;

class TagAffinityServiceTest {

    private final VideoTagRepository videoTagRepo = mock(VideoTagRepository.class);
    private final UserTagRepository userTagRepo = mock(UserTagRepository.class);
    private final VideoRepository videoRepo = mock(VideoRepository.class);
    private final UserTopicRepository userTopicRepo = mock(UserTopicRepository.class);
    private final TagAffinityService service = new TagAffinityService(videoTagRepo, userTagRepo, videoRepo,
            userTopicRepo, new TopicCatalog());

    private final UUID userId = UUID.randomUUID();
    private final UUID videoId = UUID.randomUUID();

    private VideoTag tagged(UUID tagId) {
        Tag tag = new Tag();
        tag.setId(tagId);
        VideoTag vt = new VideoTag();
        vt.setTag(tag);
        return vt;
    }

    @Test
    void firstPointCreatesTheRowForEveryTagOfTheVideo() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(videoTagRepo.findByVideo_Id(videoId)).thenReturn(List.of(tagged(a), tagged(b)));
        when(userTagRepo.findById(any())).thenReturn(Optional.empty());

        service.addQuietly(userId, videoId, TagAffinityService.LIKE);

        ArgumentCaptor<UserTag> saved = ArgumentCaptor.forClass(UserTag.class);
        verify(userTagRepo, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(UserTag::getTagId).containsExactly(a, b);
        assertThat(saved.getAllValues()).extracting(UserTag::getPoint).containsOnly(5.0);
    }

    @Test
    void laterPointsAddUpAndAreCappedAt1000() {
        UUID a = UUID.randomUUID();
        UserTag existing = UserTag.builder().userId(userId).tagId(a).point(995).build();
        when(videoTagRepo.findByVideo_Id(videoId)).thenReturn(List.of(tagged(a)));
        when(userTagRepo.findById(any())).thenReturn(Optional.of(existing));

        service.addQuietly(userId, videoId, TagAffinityService.SHARE);

        assertThat(existing.getPoint()).isEqualTo(1000.0);
        verify(userTagRepo).save(existing);
    }

    @Test
    void theVideosTopicAndItsGroupGetPointsToo() {
        when(videoRepo.findTopicIdById(videoId)).thenReturn("music.lo-fi");
        UserTopic group = UserTopic.builder().userId(userId).topicId("music").point(10).build();
        when(userTopicRepo.findById(new UserTopic.UserTopicId(userId, "music.lo-fi"))).thenReturn(Optional.empty());
        when(userTopicRepo.findById(new UserTopic.UserTopicId(userId, "music"))).thenReturn(Optional.of(group));

        service.addQuietly(userId, videoId, TagAffinityService.COMMENT);

        ArgumentCaptor<UserTopic> saved = ArgumentCaptor.forClass(UserTopic.class);
        verify(userTopicRepo, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getTopicId()).isEqualTo("music.lo-fi");
        assertThat(saved.getAllValues().get(0).getPoint()).isEqualTo(8.0);
        assertThat(group.getPoint()).isEqualTo(18.0);
    }

    @Test
    void aVideoWithoutATopicOrWithAnUnknownOneGivesNoTopicPoints() {
        when(videoRepo.findTopicIdById(videoId)).thenReturn(null, "music.khong-co");

        service.addQuietly(userId, videoId, TagAffinityService.VIEW);
        service.addQuietly(userId, videoId, TagAffinityService.VIEW);

        verify(userTopicRepo, never()).save(any());
    }

    @Test
    void aFailureIsLoggedNotThrown() {
        when(videoTagRepo.findByVideo_Id(videoId)).thenThrow(new RuntimeException("db down"));

        service.addQuietly(userId, videoId, TagAffinityService.VIEW);

        verify(userTagRepo, never()).save(any());
    }

    @Test
    void onlyAShareReportedByTheClientAddsPoints() {
        VideoRepository videoRepo = mock(VideoRepository.class);
        TagAffinityService affinity = mock(TagAffinityService.class);
        when(videoRepo.findById(videoId)).thenReturn(Optional.of(new Video()));
        var interactions = new UserInteractionServiceImpl(videoRepo, affinity);

        interactions.saveInteraction(userId, videoId, InteractionType.VIEW);
        interactions.saveInteraction(userId, videoId, InteractionType.LIKE);
        interactions.saveInteraction(userId, videoId, InteractionType.COMMENT);
        verify(affinity, never()).addQuietly(any(), any(), org.mockito.ArgumentMatchers.anyDouble());

        interactions.saveInteraction(userId, videoId, InteractionType.SHARE);
        verify(affinity).addQuietly(userId, videoId, TagAffinityService.SHARE);
    }
}
