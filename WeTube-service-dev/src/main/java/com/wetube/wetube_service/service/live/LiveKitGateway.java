package com.wetube.wetube_service.service.live;

import java.util.Collection;
import java.util.List;

import com.wetube.wetube_service.enumeration.LiveProtocol;

/**
 * Mọi lời gọi tới LiveKit Cloud đi qua đây để test được bằng Mockito. Lỗi mạng / máy chủ / hết hạn mức đều thành
 * {@link LiveKitUnavailableException}.
 */
public interface LiveKitGateway {

    /** Người đang ở trong phòng; publishingVideo = có luồng hình (người phát). */
    record Participant(String identity, boolean publishingVideo) {
    }

    record IngressDetails(String ingressId, String url, String streamKey) {
    }

    /**
     * Tạo phòng với trần người tham gia do LiveKit giữ cứng khi vào phòng (token xin dồn cũng không vượt được) và giữ
     * phòng tối đa 1 giờ khi chưa có ai (OBS có thể nối chậm).
     */
    void createRoom(String room, String metadataJson, int maxParticipants);

    void deleteRoom(String room);

    /** Rỗng nếu phòng không tồn tại. */
    List<Participant> participants(String room);

    String publisherToken(String room, String identity, String displayName);

    String viewerToken(String room, String identity);

    IngressDetails createIngress(String room, String identity, String displayName, LiveProtocol protocol);

    void deleteIngress(String ingressId);

    /** Xoá Ingress không hoạt động / lỗi, trừ các id trong keepIngressIds; trả số đã xoá. */
    int deleteInactiveIngress(Collection<String> keepIngressIds);

    /** Địa chỉ WebSocket của LiveKit cho trình duyệt. */
    String wsUrl();
}
