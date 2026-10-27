"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { LiveKitRoom, RoomAudioRenderer, StartAudio, VideoTrack, useTracks } from "@livekit/components-react";
import { Track } from "livekit-client";
import { Maximize, Pause, PictureInPicture2, Play, Volume2, VolumeX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { useGetLiveTokenMutation } from "@/app/api/liveApi";
import { apiErrorMessage } from "@/lib/apiError";
import type { LiveStatus } from "../types";
import { LiveBadge } from "./live-badge";

/** Tab ẩn lâu hơn mức này thì ngắt WebRTC (tiết kiệm phút của gói LiveKit), quay lại thì nối lại. */
const HIDDEN_DISCONNECT_MS = 2 * 60 * 1000;

function Frame({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative flex aspect-video w-full items-center justify-center overflow-hidden rounded-xl bg-black text-center text-white">
      {children}
    </div>
  );
}

/** Hình của người phát (identity bắt đầu bằng "u:") và các nút điều khiển. */
function Stage() {
  const containerRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [paused, setPaused] = useState(false);
  const [muted, setMuted] = useState(false);
  const [volume, setVolume] = useState(1);
  const tracks = useTracks([Track.Source.Camera, Track.Source.ScreenShare]).filter(
    (t) => t.participant.identity.startsWith("u:") && t.publication
  );
  const track = tracks[0];

  const togglePlay = () => {
    const el = videoRef.current;
    if (!el) return;
    if (el.paused) {
      void el.play();
      setPaused(false);
    } else {
      el.pause();
      setPaused(true);
    }
  };

  const enterPictureInPicture = () => {
    const el = videoRef.current;
    if (el && document.pictureInPictureEnabled) void el.requestPictureInPicture();
  };

  const enterFullscreen = () => {
    const el = containerRef.current;
    if (!el) return;
    if (document.fullscreenElement) void document.exitFullscreen();
    else void el.requestFullscreen();
  };

  return (
    <div
      ref={containerRef}
      className="group relative flex aspect-video w-full items-center justify-center overflow-hidden rounded-xl bg-black"
    >
      {track ? (
        <VideoTrack ref={videoRef} trackRef={track} className="h-full w-full object-contain" />
      ) : (
        <div className="flex flex-col items-center gap-2 text-white">
          <Spinner className="size-6" />
          <p className="text-sm">Người phát đang gặp sự cố kết nối…</p>
        </div>
      )}
      <RoomAudioRenderer volume={volume} muted={muted} />
      <StartAudio
        label="Bấm để bật tiếng"
        className="absolute inset-0 flex items-center justify-center bg-black/40 text-sm font-semibold text-white"
      />
      <LiveBadge className="absolute left-3 top-3" />
      <div className="absolute inset-x-0 bottom-0 flex items-center gap-2 bg-gradient-to-t from-black/70 to-transparent p-3 text-white opacity-0 transition-opacity group-hover:opacity-100 focus-within:opacity-100">
        <button aria-label={paused ? "Phát" : "Tạm dừng"} onClick={togglePlay} className="rounded p-1 hover:bg-white/20">
          {paused ? <Play className="size-5" /> : <Pause className="size-5" />}
        </button>
        <button aria-label={muted ? "Bật tiếng" : "Tắt tiếng"} onClick={() => setMuted((m) => !m)} className="rounded p-1 hover:bg-white/20">
          {muted || volume === 0 ? <VolumeX className="size-5" /> : <Volume2 className="size-5" />}
        </button>
        <input
          type="range"
          min={0}
          max={1}
          step={0.05}
          value={muted ? 0 : volume}
          onChange={(e) => {
            const next = Number(e.target.value);
            setVolume(next);
            setMuted(next === 0);
          }}
          aria-label="Âm lượng"
          className="h-1 w-24 accent-white"
        />
        <span className="flex-1" />
        <button aria-label="Hình trong hình" onClick={enterPictureInPicture} className="rounded p-1 hover:bg-white/20">
          <PictureInPicture2 className="size-5" />
        </button>
        <button aria-label="Toàn màn hình" onClick={enterFullscreen} className="rounded p-1 hover:bg-white/20">
          <Maximize className="size-5" />
        </button>
      </div>
    </div>
  );
}

/**
 * Trình phát người xem. Chỉ nối LiveKit khi buổi đang LIVE (mỗi phút nối đều tính vào hạn mức gói free). Đã đủ người
 * xem bằng hình thì chỉ hiện thông báo; chat và bình chọn vẫn dùng được. Tab ẩn quá 2 phút thì ngắt, quay lại nối lại.
 */
export default function LivePlayer({ id, status }: { id: string; status: LiveStatus }) {
  const [getToken] = useGetLiveTokenMutation();
  const [session, setSession] = useState<{ token: string; wsUrl: string } | null>(null);
  const [full, setFull] = useState(false);
  const [problem, setProblem] = useState<string | null>(null);
  const [dropped, setDropped] = useState(false);
  const droppedRef = useRef(false);
  droppedRef.current = dropped;

  const connect = useCallback(async () => {
    setProblem(null);
    try {
      const res = await getToken({ id }).unwrap();
      if (res.full || !res.token) {
        setFull(true);
        setSession(null);
        return;
      }
      setFull(false);
      setSession({ token: res.token, wsUrl: res.wsUrl });
    } catch (err) {
      setProblem(apiErrorMessage(err, "Buổi phát tạm thời không xem được."));
    }
  }, [getToken, id]);

  useEffect(() => {
    if (status === "LIVE" && !session && !full && !problem && !dropped) void connect();
  }, [status, session, full, problem, dropped, connect]);

  useEffect(() => {
    let timer: number | undefined;
    const onVisibility = () => {
      if (document.hidden) {
        timer = window.setTimeout(() => {
          setSession(null);
          setDropped(true);
        }, HIDDEN_DISCONNECT_MS);
      } else {
        window.clearTimeout(timer);
        if (droppedRef.current) setDropped(false);
      }
    };
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  if (status === "CREATED") {
    return (
      <Frame>
        <p className="text-lg font-semibold">Buổi phát sắp bắt đầu</p>
      </Frame>
    );
  }
  if (full) {
    return (
      <Frame>
        <p className="px-6 text-base font-semibold">
          Buổi phát đang đầy người xem. Bạn vẫn xem được chat và bình chọn.
        </p>
      </Frame>
    );
  }
  if (problem) {
    return (
      <Frame>
        <div className="flex flex-col items-center gap-3 px-6">
          <p className="text-sm">{problem}</p>
          <Button variant="secondary" size="sm" onClick={() => void connect()}>
            Thử lại
          </Button>
        </div>
      </Frame>
    );
  }
  if (!session) {
    return (
      <Frame>
        {dropped ? (
          <p className="text-sm">Đã tạm ngắt vì bạn rời tab. Quay lại để xem tiếp.</p>
        ) : (
          <Spinner className="size-6" />
        )}
      </Frame>
    );
  }
  return (
    <LiveKitRoom serverUrl={session.wsUrl} token={session.token} connect audio={false} video={false}>
      <Stage />
    </LiveKitRoom>
  );
}
