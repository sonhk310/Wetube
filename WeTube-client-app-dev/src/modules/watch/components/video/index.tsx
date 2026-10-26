"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { motion } from "motion/react";
import { useProgressRouter } from "@/hooks/use-progress-router";
import {
  ChevronRight,
  FastForward,
  Loader2,
  Maximize,
  Minimize,
  Pause,
  PictureInPicture2,
  Play,
  RectangleHorizontal,
  RotateCcw,
  Rewind,
  Subtitles,
  Volume1,
  Volume2,
  VolumeX,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useControls, useTheater } from "@/hooks/use-controls";
import { useSubtitles } from "@/hooks/use-subtitles";
import { saveAnonWatchTime, useViewCounter } from "@/lib/viewTracking";
import { saveWatchProgress, useWatchProgressStore } from "@/lib/watchProgress";
import { SEEK_EVENT } from "@/lib/watchSeek";
import { useAuth } from "@/contexts/auth-context";
import { useRoomPlayback } from "@/modules/rooms/lib/useRoomPlayback";
import { useVideoStore } from "@/store/zustand/videoStore";
import ProgressBar from "./progress-bar";
import SettingsMenu, { PLAYBACK_RATES } from "./settings-menu";
import PlayerContextMenu, { CONTEXT_MENU_SIZE } from "./context-menu";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import EndScreen from "./end-screen";
import type { RecommendVideoDto } from "@/types/video";
import { usePlaylistQueue } from "../../lib/use-playlist-queue";
import { chapterIndexAt, parseChapters } from "../../lib/chapters";
import { useChaptersPanelStore } from "@/store/zustand/useChaptersPanelStore";
import { formatTime } from "./format-time";

const VOLUME_KEY = "wetube_player_volume";
const AUTOPLAY_KEY = "wetube_autoplay_next";
const HIDE_CONTROLS_MS = 2500;
/** Khi kéo thanh tua, tua video thật tối đa mỗi khoảng này một lần (đỡ giật). */
const SCRUB_SEEK_MS = 120;
/** Đang phát thì gửi vị trí đang xem mỗi khoảng này (người đã đăng nhập). */
const PROGRESS_INTERVAL_MS = 15_000;
/** Tua xong chừng này mới gửi vị trí mới (gom các lần tua liên tiếp). */
const SEEK_REPORT_DELAY_MS = 1000;
/** Xem chưa tới chừng này giây thì chưa tính vào lịch sử. */
const MIN_PROGRESS_SECONDS = 3;

/**
 * Phát tiếp từ vị trí cũ, trừ khi mới xem vài giây hoặc đã xem gần hết (giống YouTube).
 * Video ngắn: hai ngưỡng tính theo tỉ lệ (tối đa 20% độ dài), nếu không video 12 giây
 * sẽ không bao giờ phát tiếp được (5 giây đầu + 10 giây cuối đã dài hơn cả video).
 */
function resumePosition(saved: number | null | undefined, duration: number) {
  if (!saved || !Number.isFinite(duration)) return 0;
  const head = Math.min(5, duration * 0.2);
  const tail = Math.min(Math.max(10, duration * 0.05), duration * 0.2);
  return saved > head && saved < duration - tail ? saved : 0;
}

type FlashIcon = "play" | "pause" | "forward" | "back" | "volume" | "mute";
type Flash = { icon: FlashIcon; label?: string; id: number };

function readSavedVolume(): { volume: number; muted: boolean } {
  try {
    const saved = JSON.parse(window.localStorage.getItem(VOLUME_KEY) ?? "null");
    if (saved && typeof saved.volume === "number" && saved.volume >= 0 && saved.volume <= 1) {
      return { volume: saved.volume, muted: Boolean(saved.muted) };
    }
  } catch {
    // localStorage bị chặn hoặc dữ liệu hỏng: dùng mặc định
  }
  return { volume: 1, muted: false };
}

function saveVolume(volume: number, muted: boolean) {
  try {
    window.localStorage.setItem(VOLUME_KEY, JSON.stringify({ volume, muted }));
  } catch {
    // bỏ qua
  }
}

function isTypingTarget(target: EventTarget | null) {
  const el = target as HTMLElement | null;
  return !!el && (el.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(el.tagName));
}

/** Ảnh xem trước: Cloudinary trả khung hình đầu khi đổi đuôi video thành .jpg. */
function posterFor(videoUrl: string) {
  return videoUrl.includes("res.cloudinary.com") ? videoUrl.replace(/\.\w+$/, ".jpg") : undefined;
}

function IconButton({
  label,
  onClick,
  children,
  className,
}: {
  label: string;
  onClick: () => void;
  children: ReactNode;
  className?: string;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      onClick={onClick}
      className={cn(
        "flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-white transition hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-white",
        className
      )}
    >
      {children}
    </button>
  );
}

const FLASH_ICONS: Record<FlashIcon, ReactNode> = {
  play: <Play size={30} fill="currentColor" />,
  pause: <Pause size={30} fill="currentColor" />,
  forward: <FastForward size={30} fill="currentColor" />,
  back: <Rewind size={30} fill="currentColor" />,
  volume: <Volume2 size={30} />,
  mute: <VolumeX size={30} />,
};

/** Trình phát video trên trang xem, điều khiển giống YouTube. Dùng thẻ <video> của trình duyệt. */
/**
 * roomMode: trình phát của phòng xem chung. Trạng thái phát do phòng quyết định (modules/rooms/lib/useRoomPlayback),
 * nên không tự phát, không nhớ vị trí xem, không lặp lại / đổi tốc độ, không có màn hình hết video.
 */
export default function ActiveVideo({ videoId, roomMode = false }: { videoId: string; roomMode?: boolean }) {
  const response = useVideoStore((s) => s.videoDetail);
  // Khi chuyển video, store còn giữ video cũ một lúc: chỉ phát khi đúng video của trang
  const video = response?.detail.id === videoId ? response.detail : undefined;
  const upNext = video ? (response?.recommend?.video ?? []).filter((v) => v.id !== videoId && !v.isShort) : [];
  const router = useProgressRouter();
  // Đang phát theo danh sách (?list=): hết video thì sang video kế tiếp trong danh sách
  const queue = usePlaylistQueue(videoId);
  const queueRef = useRef(queue);
  queueRef.current = queue;
  // Phải giữ cùng một object giữa các lần render: effect đếm ngược của EndScreen phụ thuộc vào `next`,
  // object mới mỗi lần render (thanh loading render lại mỗi 100 ms) làm đếm ngược chạy lại / push lặp lại
  const nextInList = queue.next;
  const playlistNext = useMemo<RecommendVideoDto | undefined>(
    () =>
      nextInList && {
        id: nextInList.videoId,
        title: nextInList.videoTitle,
        thumbnailUrl: nextInList.thumbnailUrl ?? "",
        totalView: nextInList.totalView,
        createAt: nextInList.createdAt,
        name: nextInList.channelName ?? "",
        duration: nextInList.duration,
        picture: "",
      },
    [nextInList]
  );
  const endScreenVideos = playlistNext
    ? [playlistNext, ...upNext.filter((v) => v.id !== playlistNext.id)]
    : upNext;
  const subtitles = video?.subtitles ?? [];
  const description = video?.description;
  const { setSeek } = useControls();
  const { user } = useAuth();
  const { theater, toggleTheater } = useTheater();

  const containerRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const surfaceRef = useRef<HTMLDivElement>(null);
  const settingsRef = useRef<HTMLDivElement>(null);

  const [playing, setPlaying] = useState(false);
  const [started, setStarted] = useState(false);
  const [ended, setEnded] = useState(false);
  const [waiting, setWaiting] = useState(true);
  const [failed, setFailed] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [buffered, setBuffered] = useState(0);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [rate, setRate] = useState(1);
  const [fullscreen, setFullscreen] = useState(false);
  const [pipSupported, setPipSupported] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [subtitleIndex, setSubtitleIndex] = useState(0);
  const [active, setActive] = useState(true);
  const [scrubbing, setScrubbing] = useState(false);
  const [flash, setFlash] = useState<Flash | null>(null);
  const [autoplayNext, setAutoplayNext] = useState(true);
  // Lặp lại video (thuộc tính loop của thẻ video: hết video thì phát lại, không hiện màn hình kết thúc)
  const [loop, setLoop] = useState(false);
  const [menuPos, setMenuPos] = useState<{ x: number; y: number } | null>(null);
  const [, copy] = useCopyToClipboard();
  // Chương đọc từ mô tả (quy tắc YouTube); không đủ điều kiện thì []
  const chapters = useMemo(() => parseChapters(description, duration), [description, duration]);
  const chapter = chapters[chapterIndexAt(chapters, currentTime)];
  const showChaptersPanel = useChaptersPanelStore((s) => s.show);

  const idleTimer = useRef<number | undefined>(undefined);
  const startApplied = useRef(false);
  const scrubState = useRef<{ wasPlaying: boolean; lastSeek: number } | null>(null);
  const lastSubtitleIndex = useRef(0);
  const swallowTap = useRef(false);

  // Sang video khác: tắt lặp lại, đóng menu chuột phải (như YouTube)
  useEffect(() => {
    setLoop(false);
    setMenuPos(null);
  }, [videoId]);

  // ---------- menu chuột phải ----------
  const openContextMenu = (e: React.MouseEvent<HTMLDivElement>) => {
    // Menu đang mở mà chuột phải lần nữa: nhường cho menu của trình duyệt (giống YouTube)
    if (menuPos) {
      setMenuPos(null);
      return;
    }
    const box = containerRef.current?.getBoundingClientRect();
    if (!box) return;
    e.preventDefault();
    setSettingsOpen(false);
    const x = Math.min(e.clientX - box.left, box.width - CONTEXT_MENU_SIZE.width - 8);
    const y = Math.min(e.clientY - box.top, box.height - CONTEXT_MENU_SIZE.height - 8);
    setMenuPos({ x: Math.max(8, x), y: Math.max(8, y) });
  };

  const closeContextMenu = useCallback((target?: EventTarget | null) => {
    setMenuPos(null);
    // Bấm lên video để đóng menu thì không dừng / phát video
    if (target && target === surfaceRef.current) swallowTap.current = true;
  }, []);

  const copyVideoUrl = async (withTime: boolean) => {
    const t = Math.floor(videoRef.current?.currentTime ?? 0);
    const url = `${window.location.origin}/watch/${videoId}${withTime && t > 0 ? `?t=${t}` : ""}`;
    if (await copy(url)) toastEmitter.success("Đã sao chép liên kết");
    else toastEmitter.error("Không sao chép được, hãy chép thủ công");
  };

  // ---------- lưu vị trí đang xem (lịch sử + phát tiếp) ----------
  const progressRef = useRef<{ userId?: string; videoId?: string; lastSent: number }>({ lastSent: -1 });
  progressRef.current.userId = user?.sub;
  progressRef.current.videoId = video?.id;

  // Thời điểm đang xem gần nhất: khi rời trang, React đã gỡ thẻ video (videoRef = null) trước khi
  // hàm dọn dẹp chạy, nên lượt gửi cuối cùng phải dùng giá trị này
  const lastTimeRef = useRef(0);
  const seekReportTimer = useRef<number | undefined>(undefined);
  useEffect(() => () => window.clearTimeout(seekReportTimer.current), []);

  const reportProgress = useCallback((final: boolean) => {
    const v = videoRef.current;
    const p = progressRef.current;
    if (!p.videoId || scrubState.current) return;
    const t = v ? (v.ended ? v.duration : v.currentTime) : lastTimeRef.current;
    if (!Number.isFinite(t) || t < MIN_PROGRESS_SECONDS) return;
    if (Math.abs(t - p.lastSent) < 1) return;
    p.lastSent = t;
    // Chưa đăng nhập: chỉ gửi để tính thời gian xem cho chủ kênh
    if (p.userId) saveWatchProgress(p.videoId, t, final);
    else saveAnonWatchTime(p.videoId, t);
  }, []);

  useRoomPlayback(videoRef, roomMode, video?.videoUrl);

  const track = subtitleIndex >= 0 ? subtitles[subtitleIndex] : undefined;
  const cues = useSubtitles(track?.subtitleUrl);
  const cue = cues.find((c) => currentTime >= c.start && currentTime <= c.end);

  // Tính 1 lượt xem sau khi video đã chạy liên tục 5 giây
  useViewCounter(video?.id, playing, 5000);

  // Hộp thoại Chia sẻ cần giây đang xem; chỉ báo khi sang giây mới
  const wholeSecond = Math.floor(currentTime);
  useEffect(() => setSeek(wholeSecond), [wholeSecond, setSeek]);

  // ---------- tự phát video tiếp theo ----------
  useEffect(() => {
    try {
      setAutoplayNext(window.localStorage.getItem(AUTOPLAY_KEY) !== "0");
    } catch {
      // localStorage bị chặn: mặc định bật
    }
  }, []);

  const toggleAutoplayNext = () => {
    setAutoplayNext((prev) => {
      try {
        window.localStorage.setItem(AUTOPLAY_KEY, prev ? "0" : "1");
      } catch {
        // bỏ qua
      }
      return !prev;
    });
  };

  const openVideo = useCallback(
    (id: string) => {
      // Danh sách 1 video có bật lặp lại: "video sau" là chính nó, cùng URL nên phải tự phát lại
      if (id === videoId) {
        videoRef.current?.play().catch(() => {});
        return;
      }
      const inList = queueRef.current.detail?.videos.some((v) => v.videoId === id);
      router.push(inList ? queueRef.current.hrefFor(id) : `/watch/${id}`);
    },
    [router, videoId]
  );

  // ---------- hiện/ẩn thanh điều khiển ----------
  const poke = useCallback(() => {
    setActive(true);
    window.clearTimeout(idleTimer.current);
    idleTimer.current = window.setTimeout(() => setActive(false), HIDE_CONTROLS_MS);
  }, []);
  useEffect(() => () => window.clearTimeout(idleTimer.current), []);
  const controlsVisible = active || !playing || settingsOpen || scrubbing;

  const showFlash = useCallback((icon: FlashIcon, label?: string) => {
    setFlash({ icon, label, id: Date.now() });
  }, []);

  // ---------- thao tác ----------
  const togglePlay = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused || v.ended) {
      v.play().catch(() => {});
      showFlash("play");
    } else {
      v.pause();
      showFlash("pause");
    }
  }, [showFlash]);

  const seekTo = useCallback((time: number) => {
    const v = videoRef.current;
    if (!v || !Number.isFinite(v.duration)) return;
    const t = Math.min(Math.max(time, 0), v.duration);
    v.currentTime = t;
    setCurrentTime(t);
  }, []);

  // Bấm mốc thời gian trong mô tả / bình luận (lib/watchSeek)
  useEffect(() => {
    const onSeek = (event: Event) => {
      const time = (event as CustomEvent<number>).detail;
      const v = videoRef.current;
      if (!v || typeof time !== "number") return;
      seekTo(time);
      v.play().catch(() => {});
    };
    window.addEventListener(SEEK_EVENT, onSeek);
    return () => window.removeEventListener(SEEK_EVENT, onSeek);
  }, [seekTo]);

  const seekBy = useCallback(
    (delta: number) => {
      const v = videoRef.current;
      if (!v) return;
      seekTo(v.currentTime + delta);
      showFlash(delta > 0 ? "forward" : "back", `${Math.abs(delta)} giây`);
    },
    [seekTo, showFlash]
  );

  const changeVolume = useCallback(
    (next: number, withFlash = false) => {
      const v = videoRef.current;
      if (!v) return;
      const vol = Math.min(Math.max(next, 0), 1);
      v.volume = vol;
      v.muted = vol === 0;
      if (withFlash) showFlash(vol === 0 ? "mute" : "volume", `${Math.round(vol * 100)}%`);
    },
    [showFlash]
  );

  const toggleMute = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    if (v.muted || v.volume === 0) {
      if (v.volume === 0) v.volume = 0.5;
      v.muted = false;
      showFlash("volume");
    } else {
      v.muted = true;
      showFlash("mute");
    }
  }, [showFlash]);

  const changeRate = useCallback((next: number) => {
    const v = videoRef.current;
    if (v) v.playbackRate = next;
  }, []);

  const stepRate = useCallback(
    (direction: 1 | -1) => {
      const v = videoRef.current;
      if (!v) return;
      const i = PLAYBACK_RATES.indexOf(v.playbackRate);
      const next = PLAYBACK_RATES[Math.min(Math.max((i < 0 ? 3 : i) + direction, 0), PLAYBACK_RATES.length - 1)];
      changeRate(next);
      showFlash(direction > 0 ? "forward" : "back", `${next}x`);
    },
    [changeRate, showFlash]
  );

  const chooseSubtitle = useCallback((index: number) => {
    if (index >= 0) lastSubtitleIndex.current = index;
    setSubtitleIndex(index);
  }, []);

  const subtitleCount = subtitles.length;
  const toggleCaptions = useCallback(() => {
    if (subtitleCount === 0) return;
    setSubtitleIndex((i) => (i >= 0 ? -1 : Math.min(lastSubtitleIndex.current, subtitleCount - 1)));
  }, [subtitleCount]);

  const toggleFullscreen = useCallback(() => {
    const el = containerRef.current;
    const v = videoRef.current as (HTMLVideoElement & { webkitEnterFullscreen?: () => void }) | null;
    if (!el) return;
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
    } else if (typeof el.requestFullscreen === "function") {
      el.requestFullscreen().catch(() => {});
    } else {
      v?.webkitEnterFullscreen?.(); // iPhone: chỉ video mới vào được toàn màn hình
    }
  }, []);

  const togglePip = useCallback(async () => {
    const v = videoRef.current;
    if (!v) return;
    try {
      if (document.pictureInPictureElement) await document.exitPictureInPicture();
      else await v.requestPictureInPicture();
    } catch {
      // trình duyệt từ chối: bỏ qua
    }
  }, []);

  // ---------- kéo thanh tua ----------
  const handleScrubStart = () => {
    const v = videoRef.current;
    if (!v) return;
    scrubState.current = { wasPlaying: !v.paused, lastSeek: 0 };
    setScrubbing(true);
    v.pause();
  };

  const handleScrub = (time: number) => {
    setCurrentTime(time);
    const s = scrubState.current;
    const v = videoRef.current;
    if (!s || !v) return;
    const now = performance.now();
    if (now - s.lastSeek >= SCRUB_SEEK_MS) {
      s.lastSeek = now;
      v.currentTime = time;
    }
  };

  const handleScrubEnd = (time: number) => {
    const v = videoRef.current;
    const s = scrubState.current;
    scrubState.current = null;
    setScrubbing(false);
    if (!v) return;
    seekTo(time);
    if (s?.wasPlaying) v.play().catch(() => {});
  };

  // ---------- sự kiện của thẻ video ----------
  const updateBuffered = () => {
    const v = videoRef.current;
    if (!v) return;
    const t = v.currentTime;
    for (let i = 0; i < v.buffered.length; i++) {
      if (v.buffered.start(i) <= t + 0.5 && v.buffered.end(i) >= t) {
        setBuffered(v.buffered.end(i));
        return;
      }
    }
    setBuffered(0);
  };

  const handleLoadedMetadata = () => {
    const v = videoRef.current;
    if (!v) return;
    setDuration(v.duration);
    if (startApplied.current) return;
    startApplied.current = true;

    const saved = readSavedVolume();
    v.volume = saved.volume;
    v.muted = saved.muted;
    // Trong phòng: vị trí và phát / dừng do phòng quyết định
    if (roomMode) return;

    // Liên kết chia sẻ dạng /watch/<id>?t=<giây>; không có thì phát tiếp từ chỗ đã dừng lần trước
    const t = Number(new URLSearchParams(window.location.search).get("t"));
    // Vị trí vừa lưu trên trình duyệt mới hơn resumeAt của server (server có thể chưa kịp ghi)
    const lastPosition = useWatchProgressStore.getState().progress[videoId] ?? video?.resumeAt;
    const start = Number.isFinite(t) && t > 0 && t < v.duration ? t : resumePosition(lastPosition, v.duration);
    if (start > 0) {
      v.currentTime = start;
      setCurrentTime(start);
    }
    // Tự phát như YouTube; trình duyệt chặn thì chờ người xem bấm nút phát
    v.play().catch(() => setWaiting(false));
  };

  // Thanh tiến trình chạy mượt theo từng khung hình khi đang phát
  useEffect(() => {
    if (!playing) return;
    let frame = 0;
    const tick = () => {
      const v = videoRef.current;
      if (v && !scrubState.current) setCurrentTime(v.currentTime);
      frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [playing]);

  // Xem được vài giây là gửi ngay một lần, để video vào lịch sử luôn (không chờ lượt gửi định kỳ)
  const firstReportedFor = useRef<string | undefined>(undefined);
  useEffect(() => {
    if (!user?.sub || !video?.id || firstReportedFor.current === video.id || currentTime < MIN_PROGRESS_SECONDS) return;
    firstReportedFor.current = video.id;
    reportProgress(false);
  }, [currentTime, user?.sub, video?.id, reportProgress]);

  // Gửi vị trí định kỳ khi đang phát; gửi ngay khi ẩn tab / đóng tab / rời trang
  useEffect(() => {
    if (!playing) return;
    const id = window.setInterval(() => reportProgress(false), PROGRESS_INTERVAL_MS);
    return () => window.clearInterval(id);
  }, [playing, reportProgress]);

  useEffect(() => {
    const onHide = () => {
      if (document.visibilityState === "hidden") reportProgress(true);
    };
    const onPageHide = () => reportProgress(true);
    document.addEventListener("visibilitychange", onHide);
    window.addEventListener("pagehide", onPageHide);
    return () => {
      document.removeEventListener("visibilitychange", onHide);
      window.removeEventListener("pagehide", onPageHide);
      reportProgress(true); // chuyển sang trang / video khác
    };
  }, [reportProgress]);

  useEffect(() => {
    setPipSupported(typeof document !== "undefined" && document.pictureInPictureEnabled === true);
    const onFullscreenChange = () => setFullscreen(document.fullscreenElement === containerRef.current);
    document.addEventListener("fullscreenchange", onFullscreenChange);
    return () => document.removeEventListener("fullscreenchange", onFullscreenChange);
  }, []);

  // Bấm ra ngoài menu cài đặt thì đóng menu (bấm lên video lúc đó chỉ đóng menu, không dừng video)
  useEffect(() => {
    if (!settingsOpen) return;
    const onPointerDown = (e: PointerEvent) => {
      if (settingsRef.current?.contains(e.target as Node)) return;
      setSettingsOpen(false);
      if (e.target === surfaceRef.current) swallowTap.current = true;
    };
    document.addEventListener("pointerdown", onPointerDown, true);
    return () => document.removeEventListener("pointerdown", onPointerDown, true);
  }, [settingsOpen]);

  // ---------- phím tắt (giống YouTube) ----------
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey || e.metaKey || e.altKey || isTypingTarget(e.target)) return;
      // Đang mở hộp thoại / menu khác thì nhường phím cho nó
      if (document.querySelector('[role="dialog"], [role="menu"]')) return;
      const target = e.target as HTMLElement | null;
      if ((e.key === " " || e.key === "Enter") && target?.closest("button, a, [role='slider']")) return;
      const v = videoRef.current;
      if (!v) return;
      const inPlayer = !!containerRef.current?.contains(document.activeElement);

      // Shift+N / Shift+P: video sau / trước trong danh sách phát
      if (e.shiftKey && (e.key === "N" || e.key === "P") && queueRef.current.detail) {
        const goTo = e.key === "N" ? queueRef.current.next : queueRef.current.prev;
        if (goTo) {
          e.preventDefault();
          openVideo(goTo.videoId);
        }
        return;
      }

      switch (e.key) {
        case " ":
        case "k":
        case "K":
          e.preventDefault();
          togglePlay();
          break;
        case "j":
        case "J":
          seekBy(-10);
          break;
        case "l":
        case "L":
          seekBy(10);
          break;
        case "ArrowLeft":
          e.preventDefault();
          seekBy(-5);
          break;
        case "ArrowRight":
          e.preventDefault();
          seekBy(5);
          break;
        case "ArrowUp":
        case "ArrowDown":
          // Chỉ đổi âm lượng khi đang thao tác trên video, còn lại để cuộn trang
          if (!inPlayer && !document.fullscreenElement) return;
          e.preventDefault();
          changeVolume((v.muted ? 0 : v.volume) + (e.key === "ArrowUp" ? 0.05 : -0.05), true);
          break;
        case "m":
        case "M":
          toggleMute();
          break;
        case "f":
        case "F":
          toggleFullscreen();
          break;
        case "c":
        case "C":
          toggleCaptions();
          break;
        case "t":
        case "T":
          if (document.fullscreenElement) return;
          toggleTheater();
          break;
        case "Home":
        case "0":
          e.preventDefault();
          seekTo(0);
          break;
        case "End":
          e.preventDefault();
          seekTo(v.duration);
          break;
        case ">":
          if (!roomMode) stepRate(1);
          break;
        case "<":
          if (!roomMode) stepRate(-1);
          break;
        default:
          if (/^[1-9]$/.test(e.key) && Number.isFinite(v.duration)) {
            seekTo((v.duration * Number(e.key)) / 10);
            break;
          }
          return;
      }
      poke();
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [togglePlay, seekBy, seekTo, changeVolume, toggleMute, toggleFullscreen, toggleCaptions, toggleTheater, stepRate, poke, openVideo, roomMode]);

  // Chạm (điện thoại): lần chạm đầu chỉ hiện điều khiển; chuột: bấm là phát/dừng
  const handleSurfacePointerUp = (e: React.PointerEvent<HTMLDivElement>) => {
    if (e.button !== 0) return;
    if (swallowTap.current) {
      swallowTap.current = false;
      return;
    }
    if (e.pointerType !== "mouse" && !controlsVisible) {
      poke();
      return;
    }
    togglePlay();
    poke();
  };

  // Chế độ rạp: rộng hết trang, cao tối đa vừa màn hình (chừa chỗ cho tiêu đề), như YouTube
  const frameClass = theater ? "h-[min(56.25vw,calc(100vh-10rem))]" : "aspect-video rounded-xl";

  if (!video?.videoUrl) {
    return (
      <div className={cn("flex w-full items-center justify-center bg-black", frameClass)}>
        <Loader2 className="animate-spin text-white/80" size={48} />
      </div>
    );
  }

  const VolumeIcon = muted || volume === 0 ? VolumeX : volume < 0.5 ? Volume1 : Volume2;
  const settingsMaxHeight = Math.max(120, (containerRef.current?.clientHeight ?? 360) - 70);

  return (
    <div
      ref={containerRef}
      tabIndex={-1}
      data-player
      className={cn(
        "group relative w-full overflow-hidden bg-black outline-none select-none",
        fullscreen ? "h-full" : frameClass,
        !controlsVisible && "cursor-none"
      )}
      onPointerMove={(e) => {
        if (e.pointerType === "mouse") poke();
      }}
      onMouseLeave={() => {
        window.clearTimeout(idleTimer.current);
        setActive(false);
      }}
      onContextMenu={openContextMenu}
    >
      <video
        ref={videoRef}
        src={video.videoUrl}
        poster={posterFor(video.videoUrl)}
        playsInline
        loop={loop}
        preload="auto"
        className="h-full w-full object-contain"
        onLoadedMetadata={handleLoadedMetadata}
        onDurationChange={() => setDuration(videoRef.current?.duration ?? 0)}
        onPlay={() => {
          setPlaying(true);
          setStarted(true);
          setEnded(false);
          poke();
        }}
        onPause={() => {
          setPlaying(false);
          setWaiting(false);
          reportProgress(true);
        }}
        onEnded={() => {
          setPlaying(false);
          setEnded(!roomMode);
          reportProgress(true);
        }}
        onWaiting={() => setWaiting(true)}
        onPlaying={() => setWaiting(false)}
        onCanPlay={() => setWaiting(false)}
        onSeeked={() => {
          setWaiting(false);
          // Tua (kéo thanh, phím, mốc thời gian) thì lưu vị trí mới, không chờ lượt gửi 15 giây.
          // Gom các lần tua liên tiếp (giữ phím →) thành một lượt, để không vượt giới hạn 60 lượt/phút
          window.clearTimeout(seekReportTimer.current);
          seekReportTimer.current = window.setTimeout(() => reportProgress(false), SEEK_REPORT_DELAY_MS);
        }}
        onTimeUpdate={() => {
          const v = videoRef.current;
          if (v) lastTimeRef.current = v.currentTime;
          if (v && !scrubState.current) setCurrentTime(v.currentTime);
          updateBuffered();
        }}
        onProgress={updateBuffered}
        onVolumeChange={() => {
          const v = videoRef.current;
          if (!v) return;
          setVolume(v.volume);
          setMuted(v.muted);
          saveVolume(v.volume, v.muted);
        }}
        onRateChange={() => setRate(videoRef.current?.playbackRate ?? 1)}
        onError={() => setFailed(true)}
      />

      {/* Vùng bấm: phát/dừng, bấm đúp để toàn màn hình */}
      <div
        ref={surfaceRef}
        className="absolute inset-0 z-0"
        onPointerUp={handleSurfacePointerUp}
        onDoubleClick={toggleFullscreen}
      />

      {/* Tiêu đề khi toàn màn hình */}
      {fullscreen && (
        <div
          className={cn(
            "pointer-events-none absolute inset-x-0 top-0 z-10 bg-gradient-to-b from-black/70 to-transparent px-6 pt-5 pb-12 transition-opacity duration-200",
            controlsVisible ? "opacity-100" : "opacity-0"
          )}
        >
          <p className="truncate text-xl font-medium text-white">{video.title}</p>
        </div>
      )}

      {/* Phụ đề */}
      {cue && (
        <div
          className={cn(
            "pointer-events-none absolute inset-x-0 z-10 flex justify-center px-4 transition-[bottom] duration-200",
            controlsVisible ? "bottom-16" : "bottom-4"
          )}
        >
          <p
            className={cn(
              "max-w-[90%] whitespace-pre-line rounded bg-black/75 px-2 py-0.5 text-center text-white",
              fullscreen ? "text-2xl md:text-3xl" : "text-sm sm:text-base md:text-lg"
            )}
          >
            {cue.text}
          </p>
        </div>
      )}

      {/* Biểu tượng chớp lên giữa màn hình khi thao tác */}
      {flash && (
        <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
          <motion.div
            key={flash.id}
            initial={{ opacity: 1, scale: 0.9 }}
            animate={{ opacity: 0, scale: 1.4 }}
            transition={{ duration: 0.6, ease: "easeOut" }}
            onAnimationComplete={() => setFlash((f) => (f?.id === flash.id ? null : f))}
            className="flex flex-col items-center gap-1 rounded-full bg-black/55 p-4 text-white"
          >
            {FLASH_ICONS[flash.icon]}
            {flash.label && <span className="text-xs font-medium">{flash.label}</span>}
          </motion.div>
        </div>
      )}

      {/* Đang tải */}
      {waiting && !failed && !ended && (
        <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
          <Loader2 className="animate-spin text-white/90" size={56} />
        </div>
      )}

      {/* Hết video: đếm ngược tự phát / lưới gợi ý */}
      {ended && !failed && (
        <EndScreen
          videos={endScreenVideos}
          // Trong danh sách phát luôn tự sang video sau, không phụ thuộc công tắc Tự phát (như YouTube)
          autoplay={playlistNext ? true : autoplayNext}
          fullscreen={fullscreen}
          onReplay={togglePlay}
          onOpen={openVideo}
        />
      )}

      {/* Chưa phát (trình duyệt chặn tự phát) */}
      {!waiting && !failed && !ended && !started && !playing && (
        <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center">
          <button
            type="button"
            aria-label="Phát"
            onClick={togglePlay}
            className="pointer-events-auto flex h-16 w-16 items-center justify-center rounded-full bg-black/60 text-white transition hover:scale-105 hover:bg-red-600"
          >
            <Play size={30} fill="currentColor" className="ml-1" />
          </button>
        </div>
      )}

      {failed && (
        <div className="absolute inset-0 z-10 flex items-center justify-center bg-black/80 px-6 text-center text-white">
          Không phát được video này. Vui lòng tải lại trang hoặc thử lại sau.
        </div>
      )}

      {/* Thanh điều khiển */}
      <div
        className={cn(
          "absolute inset-x-0 bottom-0 z-20 bg-gradient-to-t from-black/80 via-black/40 to-transparent px-3 pt-12 pb-1 transition-opacity duration-200",
          controlsVisible ? "opacity-100" : "pointer-events-none opacity-0"
        )}
      >
        <ProgressBar
          currentTime={currentTime}
          duration={duration}
          buffered={buffered}
          chapters={chapters}
          onScrubStart={handleScrubStart}
          onScrub={handleScrub}
          onScrubEnd={handleScrubEnd}
        />

        <div className="flex items-center gap-0.5 text-white">
          <IconButton label={ended ? "Xem lại" : playing ? "Tạm dừng (k)" : "Phát (k)"} onClick={togglePlay}>
            {ended ? (
              <RotateCcw size={22} />
            ) : playing ? (
              <Pause size={22} fill="currentColor" />
            ) : (
              <Play size={22} fill="currentColor" />
            )}
          </IconButton>

          <div className="group/volume flex items-center">
            <IconButton label={muted || volume === 0 ? "Bật tiếng (m)" : "Tắt tiếng (m)"} onClick={toggleMute}>
              <VolumeIcon size={22} />
            </IconButton>
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={muted ? 0 : volume}
              aria-label="Âm lượng"
              onChange={(e) => changeVolume(Number(e.target.value))}
              className="h-1 w-0 cursor-pointer accent-white opacity-0 transition-all duration-200 group-hover/volume:mr-2 group-hover/volume:w-16 group-hover/volume:opacity-100 focus-visible:mr-2 focus-visible:w-16 focus-visible:opacity-100"
            />
          </div>

          <span className="ml-1 text-xs whitespace-nowrap tabular-nums sm:text-sm">
            {formatTime(currentTime)} / {formatTime(duration)}
          </span>

          {chapter && (
            <button
              type="button"
              title={chapter.title}
              onClick={showChaptersPanel}
              className="ml-1 hidden min-w-0 items-center gap-0.5 rounded px-1 text-sm text-white/90 hover:text-white sm:flex"
            >
              <span className="text-white/60">•</span>
              <span className="truncate">{chapter.title}</span>
              <ChevronRight size={16} className="shrink-0" />
            </button>
          )}

          <div className="flex-1" />

          {!roomMode && (
          <button
            type="button"
            role="switch"
            aria-checked={autoplayNext}
            aria-label="Tự phát video tiếp theo"
            title={autoplayNext ? "Tự phát đang bật" : "Tự phát đang tắt"}
            onClick={toggleAutoplayNext}
            className="hidden h-10 shrink-0 items-center px-2 sm:flex"
          >
            <span
              className={cn(
                "relative h-3.5 w-8 rounded-full transition-colors",
                autoplayNext ? "bg-white/70" : "bg-white/30"
              )}
            >
              <span
                className={cn(
                  "absolute top-1/2 flex h-5 w-5 -translate-y-1/2 items-center justify-center rounded-full shadow transition-all",
                  autoplayNext ? "left-3 bg-white text-black" : "left-0 bg-neutral-300 text-neutral-700"
                )}
              >
                {autoplayNext ? <Play size={10} fill="currentColor" /> : <Pause size={10} fill="currentColor" />}
              </span>
            </span>
          </button>
          )}

          {subtitles.length > 0 && (
            <IconButton label={subtitleIndex >= 0 ? "Tắt phụ đề (c)" : "Bật phụ đề (c)"} onClick={toggleCaptions}>
              <span className="relative">
                <Subtitles size={22} />
                {subtitleIndex >= 0 && (
                  <span className="absolute -bottom-1.5 left-0 h-[3px] w-full rounded bg-red-600" />
                )}
              </span>
            </IconButton>
          )}

          <SettingsMenu
            rootRef={settingsRef}
            open={settingsOpen}
            onOpenChange={setSettingsOpen}
            loop={loop}
            onLoop={() => setLoop((prev) => !prev)}
            rate={rate}
            onRate={changeRate}
            subtitles={subtitles}
            subtitleIndex={subtitleIndex}
            onSubtitle={chooseSubtitle}
            maxHeight={settingsMaxHeight}
            inRoom={roomMode}
          />

          {pipSupported && (
            <IconButton label="Trình phát thu nhỏ" onClick={togglePip} className="hidden sm:flex">
              <PictureInPicture2 size={22} />
            </IconButton>
          )}

          {!fullscreen && (
            <IconButton
              label={theater ? "Chế độ mặc định (t)" : "Chế độ rạp (t)"}
              onClick={toggleTheater}
              className="hidden md:flex"
            >
              <RectangleHorizontal size={theater ? 18 : 22} />
            </IconButton>
          )}

          <IconButton label={fullscreen ? "Thoát toàn màn hình (f)" : "Toàn màn hình (f)"} onClick={toggleFullscreen}>
            {fullscreen ? <Minimize size={22} /> : <Maximize size={22} />}
          </IconButton>
        </div>
      </div>

      {menuPos && (
        <PlayerContextMenu
          x={menuPos.x}
          y={menuPos.y}
          loop={loop}
          hideLoop={roomMode}
          timeLabel={formatTime(currentTime)}
          onToggleLoop={() => setLoop((prev) => !prev)}
          onCopyUrl={() => void copyVideoUrl(false)}
          onCopyUrlAtTime={() => void copyVideoUrl(true)}
          onClose={closeContextMenu}
        />
      )}
    </div>
  );
}
