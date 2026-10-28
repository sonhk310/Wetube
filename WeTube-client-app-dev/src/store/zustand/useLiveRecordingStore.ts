import { create } from "zustand";
import { API_PREFIX } from "@/constants/appConstant";
import { refreshSession } from "@/lib/apiClient";
import {
  recordingMimeType,
  startLiveRecorder,
  type LiveRecorder,
  type RecorderSources,
  type RecorderStopReason,
  type RecordingResult,
} from "@/modules/live/lib/liveRecorder";
import type { LiveView } from "@/modules/live/types";

/**
 * Bản ghi buổi phát của chủ kênh (một buổi tại một thời điểm), dùng chung giữa bảng đang phát và trang Studio sau khi
 * buổi kết thúc: recording → stopping → stopped (bản ghi trong bộ nhớ) → uploading → done, hoặc failed (Thử lại được
 * nếu retryable). unsupported: trình duyệt không ghi được.
 */
export type RecordingPhase = "idle" | "unsupported" | "recording" | "stopping" | "stopped" | "uploading" | "failed" | "done";

interface LiveRecordingState {
  liveId: string | null;
  phase: RecordingPhase;
  stopReason: RecorderStopReason | null;
  result: RecordingResult | null;
  /** Tiến độ tải lên, 0..1. */
  progress: number;
  error: string | null;
  retryable: boolean;
  /** Kết quả từ máy chủ sau khi tải lên (có replayVideoId, hoặc recordingMessage nếu không đăng). */
  published: LiveView | null;

  start: (liveId: string, sources: RecorderSources, limitSeconds: number) => void;
  stop: (liveId: string, reason?: RecorderStopReason) => void;
  upload: () => void;
  dismiss: () => void;
}

/** Có bản ghi chưa tải lên xong (đóng tab là mất). */
export const hasPendingRecording = (phase: RecordingPhase, retryable: boolean) =>
  phase === "recording" || phase === "stopping" || phase === "stopped" || phase === "uploading" ||
  (phase === "failed" && retryable);

let recorder: LiveRecorder | null = null;

const initial = {
  liveId: null,
  phase: "idle" as RecordingPhase,
  stopReason: null,
  result: null,
  progress: 0,
  error: null,
  retryable: false,
  published: null,
};

/** Gửi bản ghi (multipart) và báo tiến độ; fetch không báo được tiến độ tải lên nên dùng XMLHttpRequest. */
function send(liveId: string, result: RecordingResult, onProgress: (p: number) => void) {
  return new Promise<{ status: number; body: unknown }>((resolve, reject) => {
    const form = new FormData();
    const ext = result.blob.type.includes("mp4") ? "mp4" : "webm";
    form.append("file", result.blob, `live-${liveId}.${ext}`);
    form.append("duration", String(Math.round(result.seconds)));
    form.append("reason", result.reason); // chỉ để máy chủ ghi log, dễ tìm nguyên nhân khi bản ghi ngắn bất thường
    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${API_PREFIX}/live/${liveId}/replay`);
    xhr.withCredentials = true;
    xhr.upload.onprogress = (e) => e.lengthComputable && onProgress(e.loaded / e.total);
    xhr.onload = () => {
      let body: unknown = null;
      try {
        body = JSON.parse(xhr.responseText);
      } catch {
        // Lỗi không phải JSON (vd. cổng trả HTML)
      }
      resolve({ status: xhr.status, body });
    };
    xhr.onerror = () => reject(new Error("network"));
    xhr.send(form);
  });
}

export const useLiveRecordingStore = create<LiveRecordingState>((set, get) => ({
  ...initial,

  start: (liveId, sources, limitSeconds) => {
    const { phase, liveId: current } = get();
    // Bản ghi của buổi trước chưa tải xong thì không ghi đè lên nó
    if (current !== liveId && hasPendingRecording(phase, get().retryable)) return;
    if (current === liveId && phase !== "idle") return;
    if (!recordingMimeType()) {
      set({ ...initial, liveId, phase: "unsupported" });
      return;
    }
    try {
      recorder = startLiveRecorder({
        sources,
        limitSeconds,
        onDone: (result) => {
          recorder = null;
          if (get().liveId === liveId) set({ phase: "stopped", result, stopReason: result.reason });
        },
      });
      set({ ...initial, liveId, phase: "recording" });
    } catch {
      set({ ...initial, liveId, phase: "unsupported" });
    }
  },

  stop: (liveId, reason) => {
    if (get().liveId !== liveId || get().phase !== "recording") return;
    set({ phase: "stopping" });
    recorder?.stop(reason);
  },

  upload: () => {
    const { liveId, result, phase } = get();
    if (!liveId || !result || (phase !== "stopped" && phase !== "failed")) return;
    if (result.blob.size === 0) {
      set({ phase: "failed", error: "Không ghi được gì nên buổi này không có video xem lại.", retryable: false });
      return;
    }
    set({ phase: "uploading", progress: 0, error: null });
    const onProgress = (progress: number) => set({ progress });
    void (async () => {
      try {
        let res = await send(liveId, result, onProgress);
        // Phiên đăng nhập hết hạn giữa buổi (access token 1 giờ): làm mới rồi gửi lại một lần
        if (res.status === 401 && (await refreshSession())) res = await send(liveId, result, onProgress);
        if (res.status >= 200 && res.status < 300) {
          set({ phase: "done", result: null, published: res.body as LiveView });
          return;
        }
        const message = (res.body as { message?: unknown } | null)?.message;
        set({
          phase: "failed",
          error: typeof message === "string" && message ? message : "Không tải được bản ghi lên, hãy thử lại.",
          // 409: máy chủ không còn chờ bản ghi này (đã có video, hoặc đã bỏ chờ); 403: không phải chủ kênh
          retryable: res.status !== 409 && res.status !== 403,
        });
      } catch {
        set({ phase: "failed", error: "Không kết nối được máy chủ, hãy thử lại.", retryable: true });
      }
    })();
  },

  dismiss: () => {
    recorder?.stop();
    recorder = null;
    set(initial);
  },
}));
