"use client";

import { useEffect, useRef } from "react";
import SockJS from "sockjs-client";
import { CompatClient, Stomp } from "@stomp/stompjs";

interface SyncMessage {
  action: "PLAY" | "PAUSE" | "SEEK" | "CHANGE_VIDEO";
  videoId?: number;
  time: number;
  sender: string;
}

export function useVideoSync(
  roomId: string,
  username: string,
  videoRef: React.RefObject<HTMLVideoElement | null>,
  onChangeVideo: (id: number) => void
) {
  const clientRef = useRef<CompatClient | null>(null);

  useEffect(() => {
    if (!roomId) return;
    const socket = new SockJS("https://wetube.name.vn/ws");
    const client = Stomp.over(socket);
    client.debug = () => {};
    client.connect({}, () => {
      client.subscribe(`/topic/rooms.video.${roomId}`, (msg) => {
        const body: SyncMessage = JSON.parse(msg.body);
        if (body.sender === username) return;
        if (!videoRef.current) return;

        switch (body.action) {
          case "PLAY":
            videoRef.current.currentTime = body.time;
            videoRef.current.play();
            break;
          case "PAUSE":
            videoRef.current.currentTime = body.time;
            videoRef.current.pause();
            break;
          case "SEEK":
            videoRef.current.currentTime = body.time;
            break;
          case "CHANGE_VIDEO":
            if (body.videoId) onChangeVideo(body.videoId);
            break;
        }
      });
    });

    clientRef.current = client;

    return () => {
      if (clientRef.current && clientRef.current.connected) {
        clientRef.current.disconnect(() => {
          console.log("Disconnected from STOMP");
        });
      }
    };
  }, [roomId, username, onChangeVideo, videoRef]);

  const sendAction = (action: SyncMessage["action"], videoId?: number) => {
    if (clientRef.current && clientRef.current.connected && videoRef.current) {
      clientRef.current.send(
        `/app/video.${roomId}`,
        {},
        JSON.stringify({
          action,
          videoId,
          time: videoRef.current.currentTime,
          sender: username,
        } as SyncMessage)
      );
    }
  };

  return { sendAction };
}
