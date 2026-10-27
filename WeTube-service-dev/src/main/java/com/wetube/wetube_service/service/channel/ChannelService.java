package com.wetube.wetube_service.service.channel;

import com.wetube.wetube_service.dto.request.ChannelUpdateRequest;
import com.wetube.wetube_service.dto.response.ChannelResponseDto;
import com.wetube.wetube_service.dto.response.HandleCheckDto;
import com.wetube.wetube_service.entity.channel.Channel;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ChannelService {
    default ChannelResponseDto getChannelById(UUID channelId, UUID viewerId) {
        return getChannelById(channelId, viewerId, false);
    }

    /** viewerIsAdmin: admin xem được cả kênh đang bị ẩn (để xét báo cáo). */
    ChannelResponseDto getChannelById(UUID channelId, UUID viewerId, boolean viewerIsAdmin);
//    void updateChannel(UUID channelId, ChannelUpdateDto dto);
//    List<UserDto> getSubscribers(UUID channelId);
//    List<UserDto> getPremiumSubscribers(UUID channelId);
    void initiateChannel(UUID userID, String ip);

    ChannelResponseDto getChannelByHandle(String handle, UUID viewerId, boolean viewerIsAdmin);

    /** Handle `raw` có dùng được cho kênh của người gọi không (đúng quy tắc, còn trống, hết thời gian chờ đổi). */
    HandleCheckDto checkHandle(UUID userId, String raw);

    /** Đổi handle kênh của người gọi; ném ResponseStatusException 400 / 409 / 429 khi không đổi được. */
    ChannelResponseDto changeHandle(UUID userId, String raw);

    /** Sửa tên / mô tả / ảnh của kênh người gọi; tên và ảnh được ghi sang cả tài khoản để mọi nơi hiện giống nhau. */
    ChannelResponseDto updateChannel(UUID userId, ChannelUpdateRequest request);

    /** Tải ảnh bìa (kind = banner) hoặc ảnh đại diện (kind = avatar) lên, trả link ảnh; chưa gắn vào kênh. */
    String uploadChannelImage(UUID userId, MultipartFile file, String kind);
}
