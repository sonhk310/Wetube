package com.wetube.wetube_service.service.live;

import java.util.UUID;

/** Việc dọn dẹp khi một buổi phát kết thúc hoặc bị huỷ (chat, Poll...). Spring tiêm mọi bean vào LiveStreamService. */
public interface LiveCleanup {

    void onEnded(UUID streamId);
}
