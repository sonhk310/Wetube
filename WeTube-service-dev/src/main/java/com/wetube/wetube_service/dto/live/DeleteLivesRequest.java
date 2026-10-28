package com.wetube.wetube_service.dto.live;

import java.util.List;
import java.util.UUID;

/** deleteVideos: xoá cả video xem lại của các buổi được chọn (mặc định không, video vẫn nằm ở Nội dung). */
public record DeleteLivesRequest(List<UUID> ids, boolean deleteVideos) {
}
