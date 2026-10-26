import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_PREFIX, WS_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import type { RoomAction, RoomEvent } from "../types";

/**
 * Kết nối STOMP của một phòng xem chung. Danh tính lấy từ cookie đăng nhập lúc bắt tay, nên không có tên tự gõ.
 * Mỗi lần kết nối (kể cả tự kết nối lại sau khi backend deploy) đều đăng ký lại và gửi join để nhận lại ảnh chụp phòng.
 */
export interface RoomSocket {
  send: (action: RoomAction, body?: unknown) => void;
  stop: () => void;
}

interface Handlers {
  onEvent: (event: RoomEvent) => void;
  onConnected: () => void;
  onDisconnected: () => void;
}

let current: RoomSocket | null = null;

/** Gửi lên phòng đang mở (không làm gì nếu chưa kết nối). */
export function sendRoom(action: RoomAction, body?: unknown) {
  current?.send(action, body);
}

export function createRoomSocket(code: string, handlers: Handlers): RoomSocket {
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_PREFIX),
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    // Access token chỉ sống 1 giờ: làm mới phiên (apiFetch tự làm khi gặp 401) trước mỗi lần kết nối
    beforeConnect: async () => {
      try {
        await apiFetch(`${API_PREFIX}/me`);
      } catch {
        // mất mạng: để lần kết nối này tự thất bại rồi thử lại
      }
    },
    onConnect: () => {
      const handle = (message: { body: string }) => {
        try {
          handlers.onEvent(JSON.parse(message.body) as RoomEvent);
        } catch (err) {
          console.error("Không đọc được tin của phòng:", err);
        }
      };
      client.subscribe(`/topic/rooms/${code}`, handle);
      client.subscribe("/user/queue/rooms", handle);
      client.publish({ destination: `/app/rooms/${code}/join` });
      handlers.onConnected();
    },
    onWebSocketClose: () => handlers.onDisconnected(),
  });

  const socket: RoomSocket = {
    send: (action, body) => {
      if (!client.connected) return;
      client.publish({
        destination: `/app/rooms/${code}/${action}`,
        body: JSON.stringify(body ?? {}),
        headers: { "content-type": "application/json" },
      });
    },
    stop: () => {
      if (current === socket) current = null;
      void client.deactivate();
    },
  };

  current = socket;
  client.activate();
  return socket;
}
