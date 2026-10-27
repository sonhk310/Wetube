import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_PREFIX, WS_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import type { LiveEvent } from "../types";

/**
 * Kết nối STOMP của một buổi phát trực tiếp. Khách (chưa đăng nhập) chỉ nghe được /topic/live/{id}; người đăng nhập
 * còn nhận lỗi riêng ở /user/queue/live và gửi được chat. Mỗi lần kết nối (kể cả tự nối lại sau khi backend deploy)
 * đều đăng ký lại topic.
 */
export interface LiveSocket {
  send: (action: "chat" | "chat/delete", body: unknown) => void;
  stop: () => void;
}

interface Handlers {
  onEvent: (event: LiveEvent) => void;
  onConnected: () => void;
  onDisconnected: () => void;
}

export function createLiveSocket(streamId: string, handlers: Handlers, authenticated: boolean): LiveSocket {
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_PREFIX),
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    // Access token chỉ sống 1 giờ: làm mới phiên (apiFetch tự làm khi gặp 401) trước mỗi lần kết nối của người đăng nhập
    beforeConnect: async () => {
      if (!authenticated) return;
      try {
        await apiFetch(`${API_PREFIX}/me`);
      } catch {
        // mất mạng: để lần kết nối này tự thất bại rồi thử lại
      }
    },
    onConnect: () => {
      const handle = (message: { body: string }) => {
        try {
          handlers.onEvent(JSON.parse(message.body) as LiveEvent);
        } catch (err) {
          console.error("Không đọc được tin của buổi phát:", err);
        }
      };
      client.subscribe(`/topic/live/${streamId}`, handle);
      if (authenticated) {
        client.subscribe("/user/queue/live", handle);
      }
      handlers.onConnected();
    },
    onWebSocketClose: () => handlers.onDisconnected(),
  });

  client.activate();

  return {
    send: (action, body) => {
      if (!client.connected) return;
      client.publish({
        destination: `/app/live/${streamId}/${action}`,
        body: JSON.stringify(body ?? {}),
        headers: { "content-type": "application/json" },
      });
    },
    stop: () => {
      void client.deactivate();
    },
  };
}
