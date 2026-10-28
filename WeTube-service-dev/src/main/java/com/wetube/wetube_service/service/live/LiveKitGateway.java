package com.wetube.wetube_service.service.live;

import java.util.Collection;
import java.util.List;

import com.wetube.wetube_service.enumeration.LiveProtocol;

/**
 * Mọi lời gọi tới LiveKit Cloud đi qua đây để test được bằng Mockito. Lỗi mạng / máy chủ / hết hạn mức đều thành
 * {@link LiveKitUnavailableException}.
 */
public interface LiveKitGateway {

    /** Người đang ở trong phòng; publishing = đang đăng bất kỳ luồng nào, hình hoặc tiếng (người phát). */
    record Participant(String identity, boolean publishing) {
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

    /**
     * Bắt đầu ghi cả phòng (bố cục "speaker": màn hình là hình chính, camera nhỏ) ra MP4 vào kho tương thích S3;
     * trả id phiên ghi. {@code filepath} là đường dẫn file trong bucket.
     */
    String startRecording(String room, String filepath);

    /** Báo dừng ghi; LiveKit còn vài giây để hoàn tất file nên kết quả lấy bằng {@link #recording}. */
    void stopRecording(String egressId);

    /** Trạng thái phiên ghi; null nếu LiveKit không biết id này. */
    RecordingInfo recording(String egressId);

    /**
     * finished = đã kết thúc (xong, lỗi hay chạm giới hạn thời gian); hasFile = có file trong kho; seconds = thời lượng
     * file; error = lý do lỗi của LiveKit (rỗng nếu không).
     */
    record RecordingInfo(boolean finished, boolean hasFile, long seconds, String error) {
    }

    /** Địa chỉ WebSocket của LiveKit cho trình duyệt. */
    String wsUrl();
}
