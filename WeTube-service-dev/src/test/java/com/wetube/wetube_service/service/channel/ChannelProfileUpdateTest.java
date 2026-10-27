package com.wetube.wetube_service.service.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.wetube.wetube_service.dto.request.ChannelUpdateRequest;
import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.video.Video;
import com.wetube.wetube_service.mapper.channel.ChannelMapper;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.security.VideoAccess;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.channel.impl.ChannelServiceImpl;

/** Studio → Tuỳ chỉnh: sửa tên / mô tả / ảnh kênh và tải ảnh bìa, ảnh đại diện. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChannelProfileUpdateTest {

    private static final String OK_IMAGE = "https://res.cloudinary.com/demo/image/upload/v1/channels/a.jpg";
    private static final String NEW_IMAGE = "https://res.cloudinary.com/demo/image/upload/v2/channels/b.jpg";
    private static final String GOOGLE_IMAGE = "https://lh3.googleusercontent.com/old";

    @Mock UserRepository userRepository;
    @Mock ChannelRepository channelRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock ChannelMapper channelMapper;
    @Mock ChannelIndexer channelIndexer;
    @Mock CloudinaryService cloudinaryService;
    @Mock VideoRepository videoRepository;
    @Mock VideoIndexer videoIndexer;

    @InjectMocks ChannelServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private AppUser user;
    private Channel channel;

    @BeforeEach
    void setUp() {
        user = AppUser.builder().id(userId).name("Tên Google").picture("https://lh3.googleusercontent.com/old").build();
        channel = Channel.builder().id(UUID.randomUUID()).name("Tên Google").picture("https://lh3.googleusercontent.com/old")
                .backgroundImgUrl(OK_IMAGE).description("cũ").user(user).build();
        user.setChannel(channel);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(channelRepository.findById(channel.getId())).thenReturn(Optional.of(channel));
        when(channelMapper.toChannelResponseDto(any())).thenReturn(new ChannelResponseDto());
        when(videoRepository.findByUserId(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(new Video())));
    }

    @Test
    void channelPageCountsPublicVideosAndViews() {
        when(videoRepository.countChannelVideos(channel.getId(), VideoAccess.LISTED)).thenReturn(4L);
        when(videoRepository.sumChannelViews(channel.getId(), VideoAccess.LISTED)).thenReturn(28L);

        ChannelResponseDto dto = service.getChannelById(channel.getId(), null);

        assertThat(dto.getTotalVideos()).isEqualTo(4);
        assertThat(dto.getTotalViews()).isEqualTo(28);
    }

    @Test
    void hiddenChannelIsNotFoundExceptForItsOwner() {
        channel.setStatus(com.wetube.wetube_service.enumeration.ActiveStatus.INACTIVE);

        assertThatThrownBy(() -> service.getChannelById(channel.getId(), null))
                .isInstanceOf(com.wetube.wetube_service.exception.ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getChannelById(channel.getId(), UUID.randomUUID()))
                .isInstanceOf(com.wetube.wetube_service.exception.ResourceNotFoundException.class);
        assertThat(service.getChannelById(channel.getId(), userId)).isNotNull();
        // Admin xem được để xét báo cáo
        assertThat(service.getChannelById(channel.getId(), UUID.randomUUID(), true)).isNotNull();
    }

    private void assertBadRequest(ChannelUpdateRequest request) {
        assertThatThrownBy(() -> service.updateChannel(userId, request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(channelRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void nameAndPictureAreWrittenToTheAccountToo() {
        service.updateChannel(userId, new ChannelUpdateRequest("  Kênh của Sơn  ", null, OK_IMAGE, null));

        assertThat(channel.getName()).isEqualTo("Kênh của Sơn");
        assertThat(user.getName()).isEqualTo("Kênh của Sơn");
        assertThat(channel.getPicture()).isEqualTo(OK_IMAGE);
        assertThat(user.getPicture()).isEqualTo(OK_IMAGE);
        verify(channelRepository).save(channel);
        verify(userRepository).save(user);
        verify(channelIndexer).index(channel);
        verify(videoIndexer).index(any(Video.class));
    }

    @Test
    void fieldsLeftOutAreKept() {
        service.updateChannel(userId, new ChannelUpdateRequest(null, "mô tả mới", null, null));

        assertThat(channel.getDescription()).isEqualTo("mô tả mới");
        assertThat(channel.getName()).isEqualTo("Tên Google");
        assertThat(channel.getPicture()).isEqualTo("https://lh3.googleusercontent.com/old");
        assertThat(user.getName()).isEqualTo("Tên Google");
        // Chỉ đổi mô tả: không cần đẩy lại chỉ mục tìm kiếm
        verify(channelIndexer, never()).index(any());
        verify(videoIndexer, never()).index(any());
    }

    @Test
    void blankDescriptionClearsItAndEmptyBannerRemovesIt() {
        service.updateChannel(userId, new ChannelUpdateRequest(null, "   ", null, ""));

        assertThat(channel.getDescription()).isNull();
        assertThat(channel.getBackgroundImgUrl()).isNull();
    }

    @Test
    void invalidValuesAreRejectedWithoutSavingAnything() {
        assertBadRequest(new ChannelUpdateRequest("", null, null, null));
        assertBadRequest(new ChannelUpdateRequest("   ", null, null, null));
        assertBadRequest(new ChannelUpdateRequest("x".repeat(51), null, null, null));
        assertBadRequest(new ChannelUpdateRequest(null, "x".repeat(1001), null, null));
        assertBadRequest(new ChannelUpdateRequest(null, null, "https://evil.example/a.png", null));
        // Ảnh thu nhỏ video (ngoài thư mục channels/) không được làm ảnh kênh
        assertBadRequest(new ChannelUpdateRequest(null, null, "https://res.cloudinary.com/demo/image/upload/v1/thumb.webp", null));
        assertBadRequest(new ChannelUpdateRequest(null, null, null, "https://res.cloudinary.com/demo/video/upload/v1/channels/a.mp4"));
        assertBadRequest(new ChannelUpdateRequest(null, null, "", null));
        assertBadRequest(new ChannelUpdateRequest(null, null, null, "http://res.cloudinary.com/insecure.png"));
    }

    @Test
    void maximumLengthsAreAccepted() {
        service.updateChannel(userId, new ChannelUpdateRequest("n".repeat(50), "d".repeat(1000), null, null));

        assertThat(channel.getName()).hasSize(50);
        assertThat(channel.getDescription()).hasSize(1000);
    }

    @Test
    void replacingTheBannerDeletesTheOldOneFromCloudinary() {
        service.updateChannel(userId, new ChannelUpdateRequest(null, null, null, NEW_IMAGE));

        verify(cloudinaryService).deleteImage(OK_IMAGE);
    }

    @Test
    void removingTheBannerDeletesItFromCloudinaryToo() {
        service.updateChannel(userId, new ChannelUpdateRequest(null, null, null, ""));

        verify(cloudinaryService).deleteImage(OK_IMAGE);
    }

    @Test
    void googleAvatarIsNeverDeletedWhenReplaced() {
        service.updateChannel(userId, new ChannelUpdateRequest(null, null, NEW_IMAGE, null));

        assertThat(channel.getPicture()).isEqualTo(NEW_IMAGE);
        verify(cloudinaryService, never()).deleteImage(GOOGLE_IMAGE);
        verify(cloudinaryService, never()).deleteImage(any());
    }

    @Test
    void oldImageStillUsedByAnotherChannelIsKept() {
        when(channelRepository.existsByPictureOrBackgroundImgUrl(OK_IMAGE, OK_IMAGE)).thenReturn(true);

        service.updateChannel(userId, new ChannelUpdateRequest(null, null, null, NEW_IMAGE));

        verify(cloudinaryService, never()).deleteImage(any());
    }

    @Test
    void sameImageSentAgainDeletesNothing() {
        service.updateChannel(userId, new ChannelUpdateRequest(null, null, null, OK_IMAGE));

        verify(cloudinaryService, never()).deleteImage(any());
    }

    @Test
    void failedValidationDeletesNothing() {
        assertBadRequest(new ChannelUpdateRequest("x".repeat(51), null, null, NEW_IMAGE));

        verify(cloudinaryService, never()).deleteImage(any());
    }

    @Test
    void userWithoutChannelGets404() {
        AppUser lonely = AppUser.builder().id(UUID.randomUUID()).name("x").build();
        when(userRepository.findById(lonely.getId())).thenReturn(Optional.of(lonely));

        assertThatThrownBy(() -> service.updateChannel(lonely.getId(), new ChannelUpdateRequest("a", null, null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void avatarUploadGoesToCloudinaryAsSquareAndBannerAsWide() {
        MockMultipartFile image = new MockMultipartFile("file", "a.png", "image/png", new byte[] { 1, 2, 3 });
        when(cloudinaryService.uploadChannelImage(any(), anyBoolean())).thenReturn(OK_IMAGE);

        assertThat(service.uploadChannelImage(userId, image, "avatar")).isEqualTo(OK_IMAGE);
        verify(cloudinaryService).uploadChannelImage(image, false);
        assertThat(service.uploadChannelImage(userId, image, "banner")).isEqualTo(OK_IMAGE);
        verify(cloudinaryService).uploadChannelImage(image, true);
    }

    @Test
    void uploadRejectsBadKindNonImagesAndOversizedFiles() {
        MockMultipartFile png = new MockMultipartFile("file", "a.png", "image/png", new byte[] { 1 });
        MockMultipartFile pdf = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[] { 1 });
        MockMultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        MockMultipartFile fiveMb = new MockMultipartFile("file", "a.png", "image/png", new byte[5 * 1024 * 1024]);

        for (var args : List.of(
                new Object[] { png, "logo" },
                new Object[] { pdf, "avatar" },
                new Object[] { empty, "avatar" },
                new Object[] { fiveMb, "avatar" })) {
            assertThatThrownBy(() -> service.uploadChannelImage(userId, (MockMultipartFile) args[0], (String) args[1]))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        // 5 MB vượt giới hạn ảnh đại diện (4 MB) nhưng nằm trong giới hạn ảnh bìa (6 MB)
        service.uploadChannelImage(userId, fiveMb, "banner");
        verify(cloudinaryService).uploadChannelImage(fiveMb, true);
        verify(cloudinaryService, never()).uploadChannelImage(any(), org.mockito.ArgumentMatchers.eq(false));
    }
}
