/**
 * Ghi buổi phát ngay trên trình duyệt của chủ kênh (MediaRecorder): một nguồn hình cố định + tiếng micro và tiếng
 * tab / hệ thống (trộn bằng Web Audio). Bản ghi nằm trong bộ nhớ của tab cho tới khi được tải lên.
 *
 * LiveKit tự thay track bên dưới của cùng một nguồn (nối lại khi mạng chập chờn, lấy lại camera khi track kết thúc, điện
 * thoại quay lại từ nền), nên nguồn được đọc lại mỗi giây. Chrome / Edge ghi một track hình do mình tạo
 * (MediaStreamTrackGenerator) và chép khung hình từ track nguồn hiện tại, nên ghi tiếp qua các lần thay. Trình duyệt
 * khác ghi thẳng track nguồn và dừng khi nó bị thay.
 */

/** Khoảng 0,8 Mb/giây hình + 64 kb/giây tiếng ≈ 6,5 MB mỗi phút. */
const VIDEO_BPS = 800_000;
const AUDIO_BPS = 64_000;
/** Đường tải lên qua backend nhận tối đa 100 MB mỗi tệp: dừng sớm hơn một chút. */
export const MAX_RECORDING_BYTES = 95 * 1024 * 1024;
/** Nguồn hình vắng (đã tắt, hoặc LiveKit đang nối lại) quá chừng này giây thì coi như đã tắt. */
const SOURCE_GONE_SECONDS = 5;

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
  stop: (reason?: RecorderStopReason) => void;
}

/** Nguồn hiện tại: track hình đang phát (null nếu đã tắt / đang nối lại) và các track tiếng. */
export interface RecorderSources {
  video: () => MediaStreamTrack | null;
  audio: () => MediaStreamTrack[];
}

type TrackProcessorCtor = new (init: { track: MediaStreamTrack }) => { readable: ReadableStream<VideoFrame> };
type TrackGenerator = MediaStreamTrack & { writable: WritableStream<VideoFrame> };
type TrackGeneratorCtor = new (init: { kind: "video" }) => TrackGenerator;

/** Chrome / Edge (cả Android): chép khung hình được từ track nguồn sang một track do mình giữ. */
function frameRelay(): { track: MediaStreamTrack; follow: (source: MediaStreamTrack) => void; close: () => void } | null {
  const g = globalThis as { MediaStreamTrackProcessor?: TrackProcessorCtor; MediaStreamTrackGenerator?: TrackGeneratorCtor };
  if (!g.MediaStreamTrackProcessor || !g.MediaStreamTrackGenerator) return null;
  const Processor = g.MediaStreamTrackProcessor;
  const generator = new g.MediaStreamTrackGenerator({ kind: "video" });
  const writer = generator.writable.getWriter();
  let current: MediaStreamTrack | null = null;
  let reader: ReadableStreamDefaultReader<VideoFrame> | null = null;

  const follow = (source: MediaStreamTrack) => {
    if (source === current) return;
    current = source;
    void reader?.cancel().catch(() => undefined);
    const mine = new Processor({ track: source }).readable.getReader();
    reader = mine;
    void (async () => {
      try {
        for (;;) {
          const { value, done } = await mine.read();
          if (done || !value) return;
          if (reader !== mine) {
            value.close();
            return;
          }
          await writer.write(value); // writer giữ và đóng khung hình
        }
      } catch {
        // Nguồn kết thúc hoặc đã chuyển sang nguồn mới
      }
    })();
  };

  const close = () => {
    void reader?.cancel().catch(() => undefined);
    reader = null;
    void writer.close().catch(() => undefined);
    generator.stop();
  };
  return { track: generator, follow, close };
}

/**
 * Bắt đầu ghi. Tự dừng khi ghi đủ {@code limitSeconds}, chạm {@link MAX_RECORDING_BYTES} hoặc nguồn hình tắt hẳn; lúc
 * nào dừng cũng gọi {@code onDone} đúng một lần với phần đã ghi.
 */
export function startLiveRecorder({
  sources,
  limitSeconds,
  onDone,
}: {
  sources: RecorderSources;
  limitSeconds: number;
  onDone: (result: RecordingResult) => void;
}): LiveRecorder {
  const mimeType = recordingMimeType();
  const firstVideo = sources.video();
  if (!mimeType || !firstVideo) throw new Error("Trình duyệt này không ghi hình được");

  const audioContext = new AudioContext();
  const mixed = audioContext.createMediaStreamDestination();
  let audioIds = "";
  let audioNodes: MediaStreamAudioSourceNode[] = [];
  const followAudio = () => {
    const tracks = sources.audio().filter((t) => t.readyState === "live");
    const ids = tracks.map((t) => t.id).join(",");
    if (ids === audioIds) return;
    audioIds = ids;
    audioNodes.forEach((n) => n.disconnect());
    audioNodes = tracks.map((t) => {
      const node = audioContext.createMediaStreamSource(new MediaStream([t]));
      node.connect(mixed);
      return node;
    });
  };
  followAudio();

  const relay = frameRelay();
  relay?.follow(firstVideo);
  const recordedVideo = relay ? relay.track : firstVideo;

  const recorder = new MediaRecorder(new MediaStream([recordedVideo, ...mixed.stream.getAudioTracks()]), {
    mimeType,
    videoBitsPerSecond: VIDEO_BPS,
    audioBitsPerSecond: AUDIO_BPS,
  });
  const chunks: Blob[] = [];
  let bytes = 0;
  let reason: RecorderStopReason = "USER";
  const startedAt = Date.now();
  let stoppedAt = 0;
  let goneSince = 0;

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
    relay?.close();
    audioNodes.forEach((n) => n.disconnect());
    void audioContext.close();
    onDone({
      blob: new Blob(chunks, { type: mimeType.split(";")[0] }),
      seconds: ((stoppedAt || Date.now()) - startedAt) / 1000,
      reason,
    });
  };

  const timer = window.setInterval(() => {
    if ((Date.now() - startedAt) / 1000 >= limitSeconds) {
      stop("LIMIT");
      return;
    }
    const video = sources.video();
    const live = video && video.readyState === "live" ? video : null;
    if (!relay) {
      // Không chép được khung hình: nguồn bị thay hay tắt thì dừng, giữ phần đã ghi
      if (live !== firstVideo) stop("SOURCE_ENDED");
    } else if (live) {
      goneSince = 0;
      relay.follow(live);
    } else if (!goneSince) {
      goneSince = Date.now();
    } else if ((Date.now() - goneSince) / 1000 >= SOURCE_GONE_SECONDS) {
      stop("SOURCE_ENDED");
    }
    followAudio();
  }, 1000);

  recorder.start(1000);
  return { stop };
}
