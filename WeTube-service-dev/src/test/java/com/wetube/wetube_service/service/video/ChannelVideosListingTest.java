package com.wetube.wetube_service.service.video;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.mapper.video.VideoMapper;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.video.impl.VideoServiceImpl;

/** Danh sách video của một kênh: lọc video thường / Shorts và sắp xếp (trang kênh). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChannelVideosListingTest {

    @Mock VideoRepository videoRepository;
    @Mock VideoMapper videoMapper;

    @InjectMocks VideoServiceImpl videoService;

    private final UUID channelOwner = UUID.randomUUID();
    private final Page<Video> empty = new PageImpl<>(List.of());

    @SuppressWarnings("unchecked")
    @Test
    void ownerSeesEverythingButDeletedNewestFirstByDefault() {
        when(videoRepository.findChannelVideosExcluding(any(), any(), any(), any())).thenReturn(empty);

        videoService.getVideosByUserId(channelOwner, 0, 12, channelOwner, false, "all", "latest");

        ArgumentCaptor<Collection<Boolean>> flags = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findChannelVideosExcluding(eq(channelOwner), eq(ActiveStatus.DELETED), flags.capture(), page.capture());
        assertThat(flags.getValue()).containsExactlyInAnyOrder(true, false);
        assertThat(page.getValue().getSort()).isEqualTo(Sort.by("createdAt").descending());
        verify(videoRepository, never()).findChannelVideos(any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void otherViewersOnlySeeListedShortsSortedByViewsThenNewest() {
        when(videoRepository.findChannelVideos(any(), any(), any(), any())).thenReturn(empty);

        videoService.getVideosByUserId(channelOwner, 0, 12, UUID.randomUUID(), false, "short", "popular");

        ArgumentCaptor<Collection<Boolean>> flags = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findChannelVideos(eq(channelOwner), eq(VideoAccess.LISTED), flags.capture(), page.capture());
        assertThat(flags.getValue()).containsExactly(true);
        assertThat(page.getValue().getSort())
                .isEqualTo(Sort.by("totalView").descending().and(Sort.by("createdAt").descending()));
        verify(videoRepository, never()).findChannelVideosExcluding(any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void anonymousViewerGetsRegularVideosOldestFirst() {
        when(videoRepository.findChannelVideos(any(), any(), any(), any())).thenReturn(empty);

        videoService.getVideosByUserId(channelOwner, 0, 12, null, false, "video", "oldest");

        ArgumentCaptor<Collection<Boolean>> flags = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findChannelVideos(eq(channelOwner), eq(VideoAccess.LISTED), flags.capture(), page.capture());
        assertThat(flags.getValue()).containsExactly(false);
        assertThat(page.getValue().getSort()).isEqualTo(Sort.by("createdAt").ascending());
    }

    @Test
    void adminSeesTheSameAsTheOwner() {
        when(videoRepository.findChannelVideosExcluding(any(), any(), any(), any())).thenReturn(empty);

        videoService.getVideosByUserId(channelOwner, 0, 12, UUID.randomUUID(), true, "all", "latest");

        verify(videoRepository).findChannelVideosExcluding(eq(channelOwner), eq(ActiveStatus.DELETED), any(), any());
    }

    @Test
    void unknownTypeOrSortIsRejectedWith400() {
        assertThatThrownBy(() -> videoService.getVideosByUserId(channelOwner, 0, 12, null, false, "podcast", "latest"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        assertThatThrownBy(() -> videoService.getVideosByUserId(channelOwner, 0, 12, null, false, "all", "random"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verify(videoRepository, never()).findChannelVideos(any(), any(), any(), any());
    }
}
