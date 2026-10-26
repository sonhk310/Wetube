package com.wetube.wetube_service.service.channel.impl;

import com.wetube.wetube_service.dto.request.ChannelUpdateRequest;
import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.dto.response.HandleCheckDto;
import com.wetube.wetube_service.entity.AppUser;
import com.wetube.wetube_service.entity.channel.Channel;
import com.wetube.wetube_service.entity.channel.ChannelCategory;
import com.wetube.wetube_service.entity.channel.MembershipTier;
import com.wetube.wetube_service.entity.channel.Subscription;
import com.wetube.wetube_service.enumeration.ActiveStatus;
import com.wetube.wetube_service.enumeration.CategoryType;
import com.wetube.wetube_service.enumeration.SubscriptionType;
import com.wetube.wetube_service.exception.ChannelAlreadyExistsException;
import com.wetube.wetube_service.exception.ResourceNotFoundException;
import com.wetube.wetube_service.mapper.channel.ChannelMapper;
import com.wetube.wetube_service.repository.channel.ChannelCategoryRepository;
import com.wetube.wetube_service.repository.channel.ChannelRepository;
import com.wetube.wetube_service.repository.channel.SubscriptionRepository;
import com.wetube.wetube_service.repository.UserRepository;
import com.wetube.wetube_service.repository.video.VideoRepository;
import com.wetube.wetube_service.service.CloudinaryService;
import com.wetube.wetube_service.service.VideoIndexer;
import com.wetube.wetube_service.service.channel.ChannelIndexer;
import com.wetube.wetube_service.service.channel.ChannelHandles;
import com.wetube.wetube_service.service.channel.ChannelService;
import com.wetube.wetube_service.service.impl.GeoIPService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
@Transactional
public class ChannelServiceImpl implements ChannelService {
    private static final Duration HANDLE_COOLDOWN = Duration.ofDays(14);
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int NAME_MAX = 50;
    private static final int DESCRIPTION_MAX = 1000;
    private static final long AVATAR_MAX_BYTES = 4L * 1024 * 1024;
    private static final long BANNER_MAX_BYTES = 6L * 1024 * 1024;
    /** Số video của kênh được đẩy lại vào chỉ mục tìm kiếm khi đổi tên / ảnh. */
    private static final int REINDEX_VIDEOS_MAX = 200;

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMapper channelMapper;

    private final ChannelCategoryRepository categoryRepository;
    private final GeoIPService geoIPService;
    private final SubscriptionRepository subscriptionRepository;
    private final ChannelIndexer channelIndexer;
    private final CloudinaryService cloudinaryService;
    private final VideoRepository videoRepository;
    private final VideoIndexer videoIndexer;

    @Override
    public ChannelResponseDto getChannelById(UUID channelId, UUID viewerId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "id", channelId.toString()));

        ChannelResponseDto dto = channelMapper.toChannelResponseDto(channel);
        Subscription subscription = viewerId == null ? null
                : subscriptionRepository.findFirstById_Subscriber_IdAndId_Tier_Channel_Id(viewerId, channelId).orElse(null);
        dto.setSubscribed(subscription != null);
        if (subscription != null) {
            // Đăng ký từ trước khi có chế độ thông báo thì coi như "Cá nhân hoá" (mặc định lúc đăng ký)
            dto.setNotificationMode(subscription.getNotificationMode() != null
                    ? subscription.getNotificationMode() : SubscriptionType.PERSONALIZE);
        }
        return dto;
    }

    @Override
    public void initiateChannel(UUID userID, String ip) {
        AppUser user = userRepository.findById(userID)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userID.toString()));

        if (user.getChannel() != null) {
            throw new ChannelAlreadyExistsException("User with id " + userID + " already has a channel.");
        }

        String countryCode = geoIPService.getCountryCode(ip).orElse("UN"); // UN = Unknown
        Channel channel = Channel.builder()
                .name(user.getName() != null ? user.getName() : "New Channel")
                .picture(user.getPicture())
                .status(ActiveStatus.ACTIVE)
                .countryCode(countryCode)
                .totalSubscribers(0)
                .totalVideos(0)
                .totalViews(0)
                .revenue(0f)
                .user(user)
                .build();

        List<ChannelCategory> defaultCategories = new ArrayList<>();
        CategoryType[] categoryTypes = CategoryType.values();
        for (int i = 0; i < categoryTypes.length; i++) {
            ChannelCategory category = ChannelCategory.builder()
                    .categoryType(categoryTypes[i])
                    .orderPosition(i + 1)
                    .isVisible(true)
                    .channel(channel)
                    .build();
            defaultCategories.add(category);
        }

        MembershipTier defaultTier = MembershipTier.builder()
                .title(user.getName() + " Default Tier")
                .description("Default membership tier")
                .price(0f)
                .defaultTier(true)
                .channel(channel)
                .build();

        channel.setCategories(defaultCategories);
        channel.setMembershipTiers(List.of(defaultTier));
        user.setChannel(channel);

        userRepository.save(user);
        channelIndexer.index(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public ChannelResponseDto getChannelByHandle(String handle, UUID viewerId) {
        Channel channel = channelRepository.findByHandle(ChannelHandles.normalize(handle))
                .orElseThrow(() -> new ResourceNotFoundException("Channel", "handle", handle));
        return getChannelById(channel.getId(), viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public HandleCheckDto checkHandle(UUID userId, String raw) {
        ResponseStatusException problem = handleProblem(channelOf(userId), ChannelHandles.normalize(raw));
        return new HandleCheckDto(problem == null, problem == null ? null : problem.getReason());
    }

    @Override
    public ChannelResponseDto changeHandle(UUID userId, String raw) {
        Channel channel = channelOf(userId);
        String handle = ChannelHandles.normalize(raw);
        ResponseStatusException problem = handleProblem(channel, handle);
        if (problem != null) {
            throw problem;
        }
        try {
            channelRepository.updateHandle(channel.getId(), handle, LocalDateTime.now());
        } catch (DataIntegrityViolationException e) {
            // Hai người lấy cùng handle cùng lúc: index duy nhất chặn người sau
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Handle đã có người dùng");
        }
        return getChannelById(channel.getId(), userId);
    }

    @Override
    public ChannelResponseDto updateChannel(UUID userId, ChannelUpdateRequest request) {
        Channel channel = channelOf(userId);
        AppUser user = channel.getUser();
        boolean displayChanged = false;

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty() || name.length() > NAME_MAX) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên kênh phải từ 1 đến " + NAME_MAX + " ký tự");
            }
            channel.setName(name);
            user.setName(name);
            displayChanged = true;
        }
        if (request.description() != null) {
            String description = request.description().trim();
            if (description.length() > DESCRIPTION_MAX) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mô tả tối đa " + DESCRIPTION_MAX + " ký tự");
            }
            channel.setDescription(description.isEmpty() ? null : description);
        }
        String oldPicture = channel.getPicture();
        String oldBanner = channel.getBackgroundImgUrl();
        if (request.picture() != null) {
            requireUploadedImage(request.picture(), "Ảnh đại diện không hợp lệ");
            channel.setPicture(request.picture());
            user.setPicture(request.picture());
            displayChanged = true;
        }
        if (request.backgroundImgUrl() != null) {
            if (request.backgroundImgUrl().isBlank()) {
                channel.setBackgroundImgUrl(null);
            } else {
                requireUploadedImage(request.backgroundImgUrl(), "Ảnh bìa không hợp lệ");
                channel.setBackgroundImgUrl(request.backgroundImgUrl());
            }
        }

        channelRepository.save(channel);
        userRepository.save(user);
        deleteReplacedImage(oldPicture, channel.getPicture());
        deleteReplacedImage(oldBanner, channel.getBackgroundImgUrl());
        if (displayChanged) {
            // Chỉ mục tìm kiếm giữ tên / ảnh của kênh trên từng video: đẩy lại để kết quả tìm kiếm khớp
            channelIndexer.index(channel);
            videoRepository.findByUserId(user.getId(), PageRequest.of(0, REINDEX_VIDEOS_MAX)).forEach(videoIndexer::index);
        }
        return getChannelById(channel.getId(), userId);
    }

    @Override
    public String uploadChannelImage(UUID userId, MultipartFile file, String kind) {
        channelOf(userId);
        boolean banner = "banner".equals(kind);
        if (!banner && !"avatar".equals(kind)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind phải là avatar hoặc banner");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hãy chọn một ảnh");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tệp phải là ảnh");
        }
        long max = banner ? BANNER_MAX_BYTES : AVATAR_MAX_BYTES;
        if (file.getSize() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh quá lớn (tối đa " + (max / 1024 / 1024) + " MB)");
        }
        return cloudinaryService.uploadChannelImage(file, banner);
    }

    /** Ảnh kênh chỉ được là ảnh đã tải lên qua {@link #uploadChannelImage}, không nhận link tuỳ ý. */
    private static void requireUploadedImage(String url, String message) {
        if (!CloudinaryService.isChannelImage(url)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    /**
     * Ảnh cũ đã bị thay / xoá thì xoá luôn trên Cloudinary cho khỏi tốn dung lượng. Chỉ xoá ảnh kênh trong thư mục
     * channels/ (ảnh Google và ảnh thu nhỏ video thì bỏ qua) và chỉ khi không kênh nào khác còn dùng link đó.
     * Làm sau khi giao dịch lưu thành công để lỗi lưu không làm mất ảnh đang dùng.
     */
    private void deleteReplacedImage(String oldUrl, String newUrl) {
        if (oldUrl == null || oldUrl.equals(newUrl) || !CloudinaryService.isChannelImage(oldUrl)
                || channelRepository.existsByPictureOrBackgroundImgUrl(oldUrl, oldUrl)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cloudinaryService.deleteImage(oldUrl);
                }
            });
        } else {
            cloudinaryService.deleteImage(oldUrl);
        }
    }

    private Channel channelOf(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
        if (user.getChannel() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bạn chưa có kênh");
        }
        return user.getChannel();
    }

    /** Lý do handle không dùng được cho kênh này (kèm mã HTTP), hoặc null nếu dùng được. */
    private ResponseStatusException handleProblem(Channel channel, String handle) {
        if (handle.equals(channel.getHandle())) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đây đã là handle hiện tại của kênh");
        }
        String invalid = ChannelHandles.invalidReason(handle);
        if (invalid != null) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, invalid);
        }
        LocalDateTime changedAt = channel.getHandleChangedAt();
        if (changedAt != null && changedAt.plus(HANDLE_COOLDOWN).isAfter(LocalDateTime.now())) {
            String until = changedAt.plus(HANDLE_COOLDOWN).atZone(ZoneOffset.UTC).withZoneSameInstant(VN)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn có thể đổi handle lại sau ngày " + until);
        }
        if (channelRepository.existsByHandle(handle)) {
            return new ResponseStatusException(HttpStatus.CONFLICT, "Handle đã có người dùng");
        }
        return null;
    }
}
