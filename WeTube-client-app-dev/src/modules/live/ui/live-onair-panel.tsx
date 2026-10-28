"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Image from "next/image";
import { Copy, MonitorUp } from "lucide-react";
import { LiveKitRoom, VideoTrack, useTracks } from "@livekit/components-react";
import { Track } from "livekit-client";
import {
  useEndLiveMutation,
  useGetLiveTokenMutation,
  useMarkLiveStartedMutation,
  useUploadLiveThumbnailMutation,
} from "@/app/api/liveApi";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { apiErrorMessage } from "@/lib/apiError";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import { hasPendingRecording, useLiveRecordingStore } from "@/store/zustand/useLiveRecordingStore";
import { useLiveChannel } from "../lib/useLiveChannel";
import { formatElapsed, liveUrl } from "../lib/format";
import { hasCamera, hasScreen, loadPublishMode, type PublishMode } from "../lib/publishMode";
import { useRecordLive } from "../lib/useRecordLive";
import { useScreenShare } from "../lib/useScreenShare";
import type { LiveView } from "../types";
import { LiveBadge, ViewerCount } from "./live-badge";
import LiveDeviceSettings from "./live-device-settings";
import LiveEncoderPanel from "./live-encoder-panel";
import LiveChat from "./live-chat";
import LiveEndDialog from "./live-end-dialog";
import LivePollComposer from "./live-poll-composer";
import LiveThumbnailPicker from "./live-thumbnail-picker";

/**
 * Hình của chính mình (xem trước) và bảng chọn thiết bị; báo cho backend ngay khi đã đăng bất kỳ luồng nào. Màn hình
 * là hình chính, camera (nếu cùng có) nằm nhỏ ở góc, giống những gì người xem thấy. Màn hình không lật gương, camera
 * thì lật. Có ghi hình thì ghi đúng một nguồn: màn hình với các kiểu có màn hình, không thì camera.
 */
function PublisherStage({
  liveId,
  mode,
  record,
  thumbnailUrl,
  onPublished,
}: {
  liveId: string;
  mode: PublishMode;
  record: boolean;
  thumbnailUrl: string | null;
  onPublished: () => void;
}) {
  const tracks = useTracks([
    Track.Source.Camera,
    Track.Source.ScreenShare,
    Track.Source.Microphone,
    Track.Source.ScreenShareAudio,
  ]).filter((t) => t.participant.isLocal && t.publication);
  const camera = tracks.find((t) => t.source === Track.Source.Camera);
  const screen = tracks.find((t) => t.source === Track.Source.ScreenShare);
  useRecordLive(
    liveId,
    record,
    (hasScreen(mode) ? screen : camera)?.publication,
    tracks
      .filter((t) => t.source === Track.Source.Microphone || t.source === Track.Source.ScreenShareAudio)
      .map((t) => t.publication)
  );
  const screenShare = useScreenShare();
  const reported = useRef(false);
  const screenSeen = useRef(false);
  const cameraSeen = useRef(false);
  if (screen) screenSeen.current = true;
  if (camera) cameraSeen.current = true;

  useEffect(() => {
    if (tracks.length > 0 && !reported.current) {
      reported.current = true;
      onPublished();
    }
  }, [tracks.length, onPublished]);

  const main = screen ?? camera;
  const shareButton = (
    <Button variant="secondary" onClick={() => void screenShare.start()}>
      <MonitorUp className="size-4" />
      Chọn màn hình để chia sẻ
    </Button>
  );

  return (
    <div className="space-y-3">
      <div className="relative flex aspect-video w-full items-center justify-center overflow-hidden rounded-xl bg-black">
        {main ? (
          <VideoTrack trackRef={main} className={`h-full w-full object-contain ${main === camera ? "-scale-x-100" : ""}`} />
        ) : hasCamera(mode) && !cameraSeen.current ? (
          <div className="flex flex-col items-center gap-2 text-white">
            <Spinner className="size-6" />
            <p className="text-sm">Đang bật camera…</p>
          </div>
        ) : hasScreen(mode) || screenSeen.current ? (
          <div className="flex flex-col items-center gap-3 px-6 text-center text-white">
            <p className="text-sm">
              {screenSeen.current ? "Đã dừng chia sẻ màn hình." : "Chọn màn hình để bắt đầu chia sẻ với người xem."}
            </p>
            {shareButton}
          </div>
        ) : (
          <div className="relative flex h-full w-full items-center justify-center px-6 text-center text-white">
            {thumbnailUrl ? (
              <Image src={thumbnailUrl} alt="" fill sizes="50vw" className="object-cover opacity-50" unoptimized />
            ) : null}
            <p className="relative text-sm">Đang phát âm thanh. Người xem không thấy hình.</p>
          </div>
        )}
        {screen && camera ? (
          <div className="absolute bottom-3 right-3 aspect-video w-1/5 overflow-hidden rounded-lg border-2 border-white/80 bg-black">
            <VideoTrack trackRef={camera} className="h-full w-full -scale-x-100 object-cover" />
          </div>
        ) : null}
        {camera && !screen && hasScreen(mode) && !screenSeen.current ? (
          <div className="absolute left-3 top-3">{shareButton}</div>
        ) : null}
      </div>
      <LiveDeviceSettings />
    </div>
  );
}

/**
 * Phát từ trình duyệt: lấy token phát rồi vào phòng LiveKit, tự bật micro và, tuỳ kiểu, camera. Màn hình thì chủ kênh
 * tự bấm chọn (trình duyệt chỉ cho bắt đầu chia sẻ từ một cú bấm).
 */
function BrowserBroadcast({ id, record, thumbnailUrl }: { id: string; record: boolean; thumbnailUrl: string | null }) {
  const [mode] = useState(() => loadPublishMode(id));
  const [getToken] = useGetLiveTokenMutation();
  const [markStarted] = useMarkLiveStartedMutation();
  const [session, setSession] = useState<{ token: string; wsUrl: string } | null>(null);
  const [problem, setProblem] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getToken({ id, publish: true })
      .unwrap()
      .then((res) => {
        if (cancelled) return;
        if (!res.token || res.role !== "PUBLISHER") {
          setProblem("Không lấy được quyền phát.");
          return;
        }
        setSession({ token: res.token, wsUrl: res.wsUrl });
      })
      .catch((err) => {
        if (!cancelled) setProblem(apiErrorMessage(err, "Không kết nối được máy chủ phát trực tiếp."));
      });
    return () => {
      cancelled = true;
    };
  }, [id, getToken]);

  // Lỗi báo ở đây không quan trọng: job 15 giây của backend vẫn tự chuyển sang LIVE khi thấy hình
  const onPublished = useCallback(() => void markStarted(id), [markStarted, id]);

  if (problem) {
    return <p className="rounded-xl bg-red-50 p-4 text-sm text-red-700">{problem}</p>;
  }
  if (!session) {
    return (
      <div className="flex aspect-video w-full items-center justify-center rounded-xl bg-black">
        <Spinner className="size-6 text-white" />
      </div>
    );
  }
  return (
    <LiveKitRoom
      serverUrl={session.wsUrl}
      token={session.token}
      connect
      audio
      video={hasCamera(mode)}
      onMediaDeviceFailure={() =>
        setProblem("Không bật được camera hoặc micro. Hãy cấp quyền cho trình duyệt rồi tải lại trang.")
      }
    >
      <PublisherStage
        liveId={id}
        mode={mode}
        record={record && mode !== "AUDIO"}
        thumbnailUrl={thumbnailUrl}
        onPublished={onPublished}
      />
    </LiveKitRoom>
  );
}

const STOPPED_TEXT: Record<string, string> = {
  SOURCE_ENDED: "Ghi hình đã dừng vì nguồn hình đã tắt.",
  SIZE: "Đã ghi đủ độ dài tối đa của một bản ghi (khoảng 14 phút).",
  LIMIT: "Đã ghi đủ phần ghi hình của tháng.",
  ERROR: "Ghi hình bị lỗi.",
};

/**
 * Trạng thái ghi hình của buổi đang phát, theo bộ ghi thật trên máy này; lý do từ máy chủ (hết hạn mức...) hiện kèm.
 */
function RecordingNote({ live }: { live: LiveView }) {
  const { recordingStatus: status, recordingMessage: message } = live;
  const rec = useLiveRecordingStore();
  const mine = rec.liveId === live.id;
  const phase = mine ? rec.phase : "idle";
  const serverNote = message ? <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">{message}</p> : null;

  if (phase === "recording") {
    return (
      <p className="flex items-center gap-2 text-sm font-medium text-red-600">
        <span className="size-2.5 animate-pulse rounded-full bg-red-600" />
        Đang ghi hình để xem lại
      </p>
    );
  }
  if (phase === "stopping" || phase === "stopped") {
    // Máy chủ chốt vì chạm trần thì đã có lý do của máy chủ; các trường hợp khác là lý do trên máy này
    const why =
      rec.stopReason === "LIMIT" && message
        ? message
        : (rec.stopReason && STOPPED_TEXT[rec.stopReason]) || "Đã dừng ghi hình.";
    return (
      <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
        {why} Phần đã ghi sẽ được tải lên khi kết thúc buổi phát.
      </p>
    );
  }
  if (phase === "unsupported") {
    return (
      <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
        Trình duyệt này không ghi hình được nên buổi này không có video xem lại.
      </p>
    );
  }
  if (serverNote) return serverNote;
  if (status === "NONE") {
    return <p className="text-sm text-gray-500">Sẽ bắt đầu ghi hình khi buổi lên sóng.</p>;
  }
  if (status === "RECORDING") {
    if (!mine && hasPendingRecording(rec.phase, rec.retryable)) {
      return (
        <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
          Bản ghi của buổi trước chưa tải lên xong nên buổi này không ghi hình.
        </p>
      );
    }
    return <p className="text-sm text-gray-500">Sẽ ghi hình khi có hình (với kiểu màn hình: sau khi chọn màn hình).</p>;
  }
  return null;
}

function useElapsed(startedAt: string | null): string | null {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (!startedAt) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [startedAt]);
  return startedAt ? formatElapsed(now - Date.parse(startedAt)) : null;
}

/** Bước đang phát của chủ kênh: hình xem trước (webcam) hoặc thông tin OBS, số người xem, kết thúc buổi phát. */
export default function LiveOnAirPanel({ live }: { live: LiveView }) {
  const channel = useLiveChannel(live.id, true);
  const [, copy] = useCopyToClipboard();
  const [endLive, { isLoading: ending }] = useEndLiveMutation();
  const [uploadThumbnail, { isLoading: uploadingThumbnail }] = useUploadLiveThumbnailMutation();
  const [endOpen, setEndOpen] = useState(false);
  const status = channel.status ?? live.status;
  const viewers = channel.viewers ?? live.currentViewers;
  const elapsed = useElapsed(status === "LIVE" ? live.startedAt : null);
  const stopRecording = useLiveRecordingStore((s) => s.stop);
  const recordingHere = useLiveRecordingStore((s) => s.liveId === live.id && s.phase === "recording");

  // Máy chủ đã chốt bản ghi (chạm trần của tháng) hoặc buổi đã kết thúc từ nơi khác: dừng bộ ghi, giữ phần đã ghi
  const serverStopped = status === "ENDED" || live.recordingStatus === "STOPPING";
  useEffect(() => {
    if (serverStopped) stopRecording(live.id, status === "ENDED" ? "USER" : "LIMIT");
  }, [serverStopped, status, stopRecording, live.id]);

  // Đang phát mà đóng tab thì người xem mất hình: nhắc trước khi rời trang
  useEffect(() => {
    if (status !== "LIVE") return;
    const warn = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [status]);

  const onEnd = async () => {
    const hadRecording = recordingHere;
    try {
      await endLive(live.id).unwrap();
      // Dừng sau khi máy chủ đã kết thúc buổi: kết thúc lỗi thì vẫn đang phát và vẫn đang ghi
      stopRecording(live.id, "USER");
      toastEmitter.success(
        hadRecording ? "Đã kết thúc buổi phát. Đang tải bản ghi lên, đừng đóng tab." : "Đã kết thúc buổi phát"
      );
      setEndOpen(false);
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không kết thúc được buổi phát, vui lòng thử lại."));
    }
  };

  const onPickThumbnail = async (file: File) => {
    try {
      await uploadThumbnail({ id: live.id, file }).unwrap();
      toastEmitter.success("Đã đổi ảnh thu nhỏ");
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không tải được ảnh thu nhỏ, vui lòng thử lại."));
    }
  };

  return (
    <section className="grid grid-cols-1 gap-x-6 px-6 py-6 md:grid-cols-6 xxl:grid-cols-8">
      <div className="col-span-1 space-y-4 md:col-span-4 xxl:col-span-6">
        {live.source === "BROWSER" ? (
          <BrowserBroadcast
            id={live.id}
            record={live.recordingEnabled && live.recordingStatus === "RECORDING"}
            thumbnailUrl={live.thumbnailUrl}
          />
        ) : (
          <LiveEncoderPanel live={live} />
        )}

        <div className="flex flex-wrap items-center gap-3">
          {status === "LIVE" ? (
            <>
              <LiveBadge />
              <ViewerCount count={viewers} />
              {elapsed ? <span className="text-sm text-gray-500">Đã phát {elapsed}</span> : null}
            </>
          ) : (
            <span className="text-sm text-gray-600">
              {live.source === "ENCODER" ? "Đang chờ tín hiệu từ phần mềm phát…" : "Đang chuẩn bị…"}
            </span>
          )}
        </div>

        {live.recordingEnabled ? <RecordingNote live={live} /> : null}

        <h1 className="text-2xl font-bold tracking-tight">{live.title}</h1>

        <div className="flex flex-wrap gap-2">
          <Button
            variant="secondary"
            className="rounded-full"
            onClick={async () => {
              if (await copy(liveUrl(live.id))) toastEmitter.success("Đã chép liên kết");
            }}
          >
            <Copy className="size-4" />
            Chép liên kết
          </Button>
          <Button variant="destructive" className="rounded-full" onClick={() => setEndOpen(true)}>
            Kết thúc buổi phát
          </Button>
        </div>

        <div className="space-y-2">
          <p className="text-sm font-medium">Ảnh thu nhỏ</p>
          <LiveThumbnailPicker
            preview={live.thumbnailUrl}
            busy={uploadingThumbnail}
            onPick={(file) => void onPickThumbnail(file)}
          />
        </div>
      </div>

      <aside className="col-span-1 mt-6 flex flex-col gap-3 md:col-span-2 md:mt-0">
        <LivePollComposer
          liveId={live.id}
          poll={channel.poll === undefined ? live.poll : channel.poll}
          onChanged={channel.setMyVote}
        />
        <LiveChat
          channel={channel}
          status={status}
          chatEnabled={channel.chatEnabled ?? live.chatEnabled}
          canModerate
          className="h-[32rem] md:h-[calc(100vh-14rem)]"
        />
      </aside>

      <LiveEndDialog open={endOpen} onOpenChange={setEndOpen} onConfirm={() => void onEnd()} loading={ending} />
    </section>
  );
}
