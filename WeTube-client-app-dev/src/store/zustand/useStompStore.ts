// store/useStompStore.ts
import { WS_PREFIX } from "@/constants/appConstant";
import { Client, Frame, IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { create } from "zustand";

interface StompState {
  client: Client | null;
  connected: boolean;
  connect: () => Promise<void>;
  disconnect: () => void;
  publish: (destination: string, body: any) => void;
  subscribe: (
    destination: string,
    callback: (msg: any) => void
  ) => StompSubscription | null;
  unsubscribe: (subscription: StompSubscription | null) => void;
}

export const useStompStore = create<StompState>((set, get) => ({
  client: null,
  connected: false,

  connect: async () => {
    return new Promise<void>((resolve, reject) => {
      const { client, connected } = get();

      if (connected) {
        console.log("[STOMP] Already connected");
        return resolve();
      }

      let newClient = client;
      let settled = false;

      if (!newClient) {
        newClient = new Client({
          brokerURL: undefined,
          webSocketFactory: () => new SockJS(WS_PREFIX),
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
          debug: (msg) => console.log("[STOMP DEBUG]", msg),
        });

        newClient.onConnect = (frame: Frame) => {
          if (!settled) {
            settled = true;
            console.log("[STOMP] Connected", frame);
            set({ connected: true });
            resolve();
          }
        };

        newClient.onStompError = (frame: Frame) => {
          if (!settled) {
            settled = true;
            console.error("[STOMP] Broker error", frame);
            newClient?.deactivate();
            set({ connected: false, client: null });
            reject(new Error("STOMP broker error"));
          }
        };

        newClient.onWebSocketClose = (evt) => {
          console.warn("[STOMP] WebSocket closed", evt);
          set({ connected: false });
        };

        newClient.onWebSocketError = (evt) => {
          console.error("[STOMP] WebSocket error", evt);
          set({ connected: false });
        };

        set({ client: newClient });
      }

      if (!newClient.active) {
        console.log("[STOMP] Activating client...");
        newClient.activate();
      } else {
        if (!settled) {
          settled = true;
          resolve();
        }
      }
    });
  },

  disconnect: () => {
    const client = get().client;
    if (client && client.active) {
      console.log("[STOMP] Deactivating client...");
      client.deactivate();
    } else {
      console.log("[STOMP] No active client to deactivate");
    }
    set({ client: null, connected: false });
  },

  publish: (destination: string, body: any) => {
    const { client, connected } = get();
    if (!connected || !client) {
      console.warn("[STOMP] Cannot publish, client not connected");
      return;
    }

    try {
      client.publish({
        destination,
        body: JSON.stringify(body),
      });
      console.log(`[STOMP] Published to ${destination}:`, body);
    } catch (err) {
      console.error("[STOMP] Failed to publish message:", err);
    }
  },

  subscribe: (destination: string, callback: (msg: IMessage) => void) => {
    const { client, connected } = get();
    if (!connected || !client) {
      console.warn("[STOMP] Cannot subscribe, client not connected");
      return null;
    }

    console.log(`[STOMP] Subscribing to ${destination}`);
    return client.subscribe(destination, (message: IMessage) => {
      // Gửi nguyên vẹn message (chưa parse)
      console.log(`[STOMP] Received from ${destination}:`, message);
      callback(message); // Trả nguyên message cho callback
    });
  },

  unsubscribe: (subscription: StompSubscription | null) => {
    if (subscription) {
      console.log("[STOMP] Unsubscribing...");
      subscription.unsubscribe();
    } else {
      console.log("[STOMP] No subscription to unsubscribe");
    }
  },
}));
