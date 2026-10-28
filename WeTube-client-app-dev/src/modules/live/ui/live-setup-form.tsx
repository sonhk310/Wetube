"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Image from "next/image";
import { Mic, Monitor, PictureInPicture2, Video, type LucideIcon } from "lucide-react";
import { useCreateLiveMutation, useGetLiveRecordingQuery, useUploadLiveThumbnailMutation } from "@/app/api/liveApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { apiErrorMessage } from "@/lib/apiError";
import { cn } from "@/lib/utils";
import { toastEmitter } from "@/lib/toastEmitter";
import {
  DEFAULT_PUBLISH_MODE,
  canShareScreen,
  hasCamera,
  savePublishMode,
  type PublishMode,
} from "../lib/publishMode";
import type { LiveProtocol, LiveSource } from "../types";
import LiveRecordingOption from "./live-recording-option";
import LiveThumbnailPicker from "./live-thumbnail-picker";

const TITLE_MAX = 100;
const DESCRIPTION_MAX = 5000;

function SourceOption({
  active,
  onClick,
  icon,
  title,
  hint,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  title: string;
  hint: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "flex flex-1 items-start gap-3 rounded-xl border p-4 text-left transition-colors",
        active ? "border-blue-600 bg-blue-50" : "border-gray-200 hover:bg-gray-50"
      )}
    >
      <span className="mt-0.5">{icon}</span>
      <span>
        <span className="block font-semibold">{title}</span>
        <span className="block text-sm text-gray-500">{hint}</span>
      </span>
    </button>
  );
}

/** Xem trước camera của trình duyệt trước khi bắt đầu; tắt hẳn camera khi đổi nguồn hoặc rời trang. */
function CameraPreview() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let stream: MediaStream | null = null;
    let cancelled = false;
    navigator.mediaDevices
      ?.getUserMedia({ video: true, audio: false })
      .then((s) => {
        if (cancelled) {
          s.getTracks().forEach((t) => t.stop());
          return;
        }
        stream = s;
        if (videoRef.current) videoRef.current.srcObject = s;
      })
      .catch(() => setError("Không bật được camera. Hãy cấp quyền camera cho trình duyệt."));
    return () => {
      cancelled = true;
      stream?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  return (
    <div className="flex aspect-video w-full items-center justify-center overflow-hidden rounded-xl bg-black">
      {error ? (
        <p className="px-6 text-center text-sm text-white">{error}</p>
      ) : (
        <video ref={videoRef} autoPlay muted playsInline className="h-full w-full -scale-x-100 object-contain" />
      )}
    </div>
  );
}

const MODE_OPTIONS: { mode: PublishMode; title: string; hint: string; needsScreen: boolean; icon: LucideIcon }[] = [
  { mode: "CAMERA", title: "Camera", hint: "Hình từ camera, tiếng từ micro.", needsScreen: false, icon: Video },
  {
    mode: "SCREEN",
    title: "Màn hình",
    hint: "Chia sẻ một tab, cửa sổ hoặc cả màn hình; không bật camera.",
    needsScreen: true,
    icon: Monitor,
  },
  {
    mode: "SCREEN_CAMERA",
    title: "Màn hình + camera",
    hint: "Màn hình là hình chính, camera nhỏ ở góc.",
    needsScreen: true,
    icon: PictureInPicture2,
  },
  { mode: "AUDIO", title: "Chỉ âm thanh", hint: "Không đăng hình, người xem thấy ảnh thu nhỏ.", needsScreen: false, icon: Mic },
];

/** Khung xem trước khi không mở camera: nói rõ hình sẽ lấy từ đâu. */
function NoCameraPreview({ mode, thumbnail }: { mode: PublishMode; thumbnail: string | null }) {
  return (
    <div className="relative flex aspect-video w-full items-center justify-center overflow-hidden rounded-xl bg-neutral-900 px-6 text-center text-sm text-neutral-300">
      {mode === "AUDIO" && thumbnail ? (
        <Image src={thumbnail} alt="" fill sizes="50vw" className="object-cover opacity-60" unoptimized />
      ) : null}
      <span className="relative">
        {mode === "AUDIO"
          ? "Chỉ phát âm thanh: người xem thấy ảnh thu nhỏ của buổi phát."
          : "Sau khi bấm Bắt đầu, bạn sẽ chọn màn hình để chia sẻ."}
      </span>
    </div>
  );
}

/** Bước chuẩn bị: tiêu đề, mô tả, nguồn phát (trình duyệt hoặc OBS) và chat. */
export default function LiveSetupForm({ onConflict }: { onConflict: () => void }) {
  const [createLive, { isLoading }] = useCreateLiveMutation();
  const [uploadThumbnail] = useUploadLiveThumbnailMutation();
  const [thumbnail, setThumbnail] = useState<File | null>(null);
  const thumbnailPreview = useMemo(() => (thumbnail ? URL.createObjectURL(thumbnail) : null), [thumbnail]);
  useEffect(() => () => (thumbnailPreview ? URL.revokeObjectURL(thumbnailPreview) : undefined), [thumbnailPreview]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [source, setSource] = useState<LiveSource>("BROWSER");
  const [mode, setMode] = useState<PublishMode>(DEFAULT_PUBLISH_MODE);
  const [screenSupported, setScreenSupported] = useState(false);
  const [protocol, setProtocol] = useState<LiveProtocol>("WHIP");
  const [chatEnabled, setChatEnabled] = useState(true);
  const { data: recordingInfo } = useGetLiveRecordingQuery();
  const [record, setRecord] = useState(false);
  const [topicId, setTopicId] = useState("");
  // Chỉ buổi phát từ trình duyệt, máy chủ đã bật ghi hình và kênh còn phần ghi
  const recordingOn =
    source === "BROWSER" && record && !!recordingInfo?.enabled && (recordingInfo?.secondsLeft ?? 0) > 0;

  // Chỉ biết được ở trình duyệt (render phía máy chủ không có navigator)
  useEffect(() => setScreenSupported(canShareScreen()), []);

  const submit = async () => {
    try {
      const created = await createLive({
        title: title.trim(),
        description: description.trim() || undefined,
        source,
        protocol: source === "ENCODER" ? protocol : undefined,
        chatEnabled,
        recordingEnabled: recordingOn || undefined,
        topicId: recordingOn ? topicId : undefined,
      }).unwrap();
      // Phải nhớ ngay: trang đang phát đọc kiểu này khi dựng lên, ngay sau khi tạo xong
      if (source === "BROWSER") savePublishMode(created.id, mode);
      if (thumbnail) {
        // Lỗi ảnh không làm hỏng buổi phát đã tạo: chủ kênh đổi ảnh được ở bảng đang phát
        await uploadThumbnail({ id: created.id, file: thumbnail })
          .unwrap()
          .catch((err) =>
            toastEmitter.error(apiErrorMessage(err, "Chưa tải được ảnh thu nhỏ, bạn có thể đổi ảnh khi đang phát."))
          );
      }
    } catch (err) {
      // Đã có một buổi chưa kết thúc (vd. mở ở tab khác): chuyển sang buổi đó
      if ((err as { status?: number }).status === 409) {
        onConflict();
        return;
      }
      toastEmitter.error(apiErrorMessage(err, "Không tạo được buổi phát, vui lòng thử lại."));
    }
  };

  return (
    <div className="grid gap-x-10 gap-y-6 px-6 pb-8 pt-4 lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]">
      <div className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="live-title">Tiêu đề (bắt buộc)</Label>
          <Input
            id="live-title"
            value={title}
            maxLength={TITLE_MAX}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Hôm nay mình phát gì?"
          />
          <p className="text-right text-xs text-gray-500">
            {title.length}/{TITLE_MAX}
          </p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="live-description">Mô tả</Label>
          <Textarea
            id="live-description"
            value={description}
            maxLength={DESCRIPTION_MAX}
            onChange={(e) => setDescription(e.target.value)}
            className="min-h-24"
          />
        </div>

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={chatEnabled} onChange={(e) => setChatEnabled(e.target.checked)} />
          Bật trò chuyện trực tiếp
        </label>

        <div className="space-y-2">
          <Label>Nguồn phát</Label>
          <div className="flex flex-col gap-3 sm:flex-row">
            <SourceOption
              active={source === "BROWSER"}
              onClick={() => setSource("BROWSER")}
              icon={<Video className="size-5" />}
              title="Phát từ trình duyệt"
              hint="Phát ngay bằng camera, màn hình hoặc chỉ micro, không cần phần mềm."
            />
            <SourceOption
              active={source === "ENCODER"}
              onClick={() => setSource("ENCODER")}
              icon={<Monitor className="size-5" />}
              title="Phần mềm phát (OBS)"
              hint="Dùng OBS hoặc phần mềm tương tự để phát màn hình, game..."
            />
          </div>
        </div>

        {source === "BROWSER" ? (
          <div className="space-y-2">
            <Label>Hình đăng lên</Label>
            <div className="grid gap-2 sm:grid-cols-2">
              {MODE_OPTIONS.filter((o) => screenSupported || !o.needsScreen).map((o) => (
                <SourceOption
                  key={o.mode}
                  active={mode === o.mode}
                  onClick={() => setMode(o.mode)}
                  icon={<o.icon className="size-5" />}
                  title={o.title}
                  hint={o.hint}
                />
              ))}
            </div>
            {!screenSupported ? (
              <p className="text-sm text-gray-500">Trình duyệt này không chia sẻ được màn hình.</p>
            ) : null}
          </div>
        ) : null}

        {source === "BROWSER" && recordingInfo?.enabled ? (
          <LiveRecordingOption
            info={recordingInfo}
            checked={record}
            onChecked={setRecord}
            topicId={topicId}
            onTopic={setTopicId}
            title={title}
            description={description}
          />
        ) : null}

        {source === "ENCODER" ? (
          <div className="space-y-2">
            <Label>Giao thức</Label>
            <div className="flex gap-2">
              <Button type="button" variant={protocol === "WHIP" ? "default" : "outline"} onClick={() => setProtocol("WHIP")}>
                WHIP (khuyên dùng)
              </Button>
              <Button type="button" variant={protocol === "RTMP" ? "default" : "outline"} onClick={() => setProtocol("RTMP")}>
                RTMP
              </Button>
            </div>
            {protocol === "RTMP" ? (
              <p className="text-sm text-amber-700">
                Trên gói miễn phí của LiveKit, RTMP chỉ còn dùng được khoảng 1 giờ mỗi tháng. WHIP (OBS 30 trở lên)
                không bị giới hạn này.
              </p>
            ) : (
              <p className="text-sm text-gray-500">Cần OBS phiên bản 30 trở lên.</p>
            )}
            <p className="text-sm text-gray-500">
              WeTube không ghi hình buổi phát bằng OBS. Muốn có video xem lại, hãy bấm Record trong OBS rồi tải tệp
              lên như một video bình thường.
            </p>
          </div>
        ) : null}

        <Button
          className="rounded-full"
          size="lg"
          disabled={!title.trim() || (recordingOn && !topicId) || isLoading}
          onClick={() => void submit()}
        >
          {isLoading ? <Spinner className="size-4" /> : "Bắt đầu"}
        </Button>
        {recordingOn && !topicId ? (
          <p className="text-sm text-gray-500">Hãy chọn chủ đề cho video xem lại để bắt đầu.</p>
        ) : null}
      </div>

      <div className="space-y-5">
        <div className="max-w-3xl">
          {source === "BROWSER" && hasCamera(mode) ? (
            <CameraPreview />
          ) : source === "BROWSER" ? (
            <NoCameraPreview mode={mode} thumbnail={thumbnailPreview} />
          ) : (
            <div className="flex aspect-video w-full items-center justify-center rounded-xl bg-neutral-900 px-6 text-center text-sm text-neutral-300">
              Sau khi bấm Bắt đầu, bạn sẽ nhận địa chỉ máy chủ và khoá luồng để dán vào phần mềm phát.
            </div>
          )}
        </div>

        <div className="space-y-2">
          <Label>Ảnh thu nhỏ</Label>
          <LiveThumbnailPicker preview={thumbnailPreview} onPick={setThumbnail} />
        </div>
      </div>
    </div>
  );
}
