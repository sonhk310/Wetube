/**
 * Ghi buổi phát ngay trên trình duyệt của chủ kênh (MediaRecorder): một nguồn hình cố định + tiếng micro và tiếng
 * tab / hệ thống (trộn bằng Web Audio). Bản ghi nằm trong bộ nhớ của tab cho tới khi được tải lên.
 */

/** Khoảng 0,8 Mb/giây hình + 64 kb/giây tiếng ≈ 6,5 MB mỗi phút. */
const VIDEO_BPS = 800_000;
const AUDIO_BPS = 64_000;
/** Đường tải lên qua backend nhận tối đa 100 MB mỗi tệp: dừng sớm hơn một chút. */
export const MAX_RECORDING_BYTES = 95 * 1024 * 1024;

/** MP4 H.264 nếu trình duyệt ghi được (Chrome / Edge bản mới, Safari), không thì WebM. */
const MIME_CANDIDATES = [
  "video/mp4;codecs=avc1,mp4a.40.2",
  "video/mp4",
  "video/webm;codecs=vp8,opus",
  "video/webm",
];

export function recordingMimeType(): string | null {
  if (typeof MediaRecorder === "undefined") return null;
  return MIME_CANDIDATES.find((type) => MediaRecorder.isTypeSupported(type)) ?? null;
}

/** SOURCE_ENDED: nguồn hình đã chọn bị tắt / đổi; LIMIT: hết phần ghi của tháng; SIZE: chạm trần dung lượng. */
export type RecorderStopReason = "USER" | "SOURCE_ENDED" | "LIMIT" | "SIZE" | "ERROR";

export interface RecordingResult {
  blob: Blob;
  seconds: number;
  reason: RecorderStopReason;
}

export interface LiveRecorder {
  /** Đổi các luồng tiếng được trộn (micro đổi thiết bị, bắt đầu / dừng chia sẻ tiếng tab). */
  setAudio: (tracks: MediaStreamTrack[]) => void;
  stop: (reason?: RecorderStopReason) => void;
}

/**
 * Bắt đầu ghi. Tự dừng khi ghi đủ {@code limitSeconds}, chạm {@link MAX_RECORDING_BYTES} hoặc nguồn hình kết thúc;
 * lúc nào dừng cũng gọi {@code onDone} đúng một lần với phần đã ghi.
 */
export function startLiveRecorder({
  video,
  audio,
  limitSeconds,
  onDone,
}: {
  video: MediaStreamTrack;
  audio: MediaStreamTrack[];
  limitSeconds: number;
  onDone: (result: RecordingResult) => void;
}): LiveRecorder {
  const mimeType = recordingMimeType();
  if (!mimeType) throw new Error("Trình duyệt này không ghi hình được");

  const audioContext = new AudioContext();
  const mixed = audioContext.createMediaStreamDestination();
  let sources: MediaStreamAudioSourceNode[] = [];
  const setAudio = (tracks: MediaStreamTrack[]) => {
    sources.forEach((s) => s.disconnect());
    sources = tracks
      .filter((t) => t.readyState === "live")
      .map((t) => {
        const source = audioContext.createMediaStreamSource(new MediaStream([t]));
        source.connect(mixed);
        return source;
      });
  };
  setAudio(audio);

  const recorder = new MediaRecorder(new MediaStream([video, ...mixed.stream.getAudioTracks()]), {
    mimeType,
    videoBitsPerSecond: VIDEO_BPS,
    audioBitsPerSecond: AUDIO_BPS,
  });
  const chunks: Blob[] = [];
  let bytes = 0;
  let reason: RecorderStopReason = "USER";
  const startedAt = Date.now();
  let stoppedAt = 0;

  const stop = (why: RecorderStopReason = "USER") => {
    if (recorder.state === "inactive") return;
    reason = why;
    stoppedAt = Date.now();
    recorder.stop();
  };

  recorder.ondataavailable = (e) => {
    if (e.data.size === 0) return;
    chunks.push(e.data);
    bytes += e.data.size;
    if (bytes >= MAX_RECORDING_BYTES) stop("SIZE");
  };
  recorder.onerror = () => stop("ERROR");
  recorder.onstop = () => {
    window.clearInterval(timer);
    sources.forEach((s) => s.disconnect());
    void audioContext.close();
    onDone({
      blob: new Blob(chunks, { type: mimeType.split(";")[0] }),
      seconds: ((stoppedAt || Date.now()) - startedAt) / 1000,
      reason,
    });
  };

  // Nguồn hình bị dừng bằng mã (LiveKit đổi thiết bị / tắt camera) không bắn sự kiện "ended": kiểm tra mỗi giây
  const timer = window.setInterval(() => {
    if (video.readyState === "ended") stop("SOURCE_ENDED");
    else if ((Date.now() - startedAt) / 1000 >= limitSeconds) stop("LIMIT");
  }, 1000);

  recorder.start(1000);
  return { setAudio, stop };
}
