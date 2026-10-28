"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Monitor, Video } from "lucide-react";
import { useCreateLiveMutation, useUploadLiveThumbnailMutation } from "@/app/api/liveApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/ui/spinner";
import { Textarea } from "@/components/ui/textarea";
import { apiErrorMessage } from "@/lib/apiError";
import { cn } from "@/lib/utils";
import { toastEmitter } from "@/lib/toastEmitter";
import type { LiveProtocol, LiveSource } from "../types";
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

/** Bước chuẩn bị: tiêu đề, mô tả, nguồn phát (webcam hoặc OBS) và chat. */
export default function LiveSetupForm({ onConflict }: { onConflict: () => void }) {
  const [createLive, { isLoading }] = useCreateLiveMutation();
  const [uploadThumbnail] = useUploadLiveThumbnailMutation();
  const [thumbnail, setThumbnail] = useState<File | null>(null);
  const thumbnailPreview = useMemo(() => (thumbnail ? URL.createObjectURL(thumbnail) : null), [thumbnail]);
  useEffect(() => () => (thumbnailPreview ? URL.revokeObjectURL(thumbnailPreview) : undefined), [thumbnailPreview]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [source, setSource] = useState<LiveSource>("BROWSER");
  const [protocol, setProtocol] = useState<LiveProtocol>("WHIP");
  const [chatEnabled, setChatEnabled] = useState(true);

  const submit = async () => {
    try {
      const created = await createLive({
        title: title.trim(),
        description: description.trim() || undefined,
        source,
        protocol: source === "ENCODER" ? protocol : undefined,
        chatEnabled,
      }).unwrap();
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
    <div className="mx-auto grid max-w-5xl gap-8 p-6 lg:grid-cols-2">
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

        <div className="space-y-2">
          <Label>Ảnh thu nhỏ</Label>
          <LiveThumbnailPicker preview={thumbnailPreview} onPick={setThumbnail} />
        </div>

        <div className="space-y-2">
          <Label>Nguồn phát</Label>
          <div className="flex flex-col gap-3 sm:flex-row">
            <SourceOption
              active={source === "BROWSER"}
              onClick={() => setSource("BROWSER")}
              icon={<Video className="size-5" />}
              title="Webcam"
              hint="Phát ngay từ trình duyệt bằng camera và micro."
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
          </div>
        ) : null}

        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={chatEnabled} onChange={(e) => setChatEnabled(e.target.checked)} />
          Bật trò chuyện trực tiếp
        </label>

        <Button className="rounded-full" size="lg" disabled={!title.trim() || isLoading} onClick={() => void submit()}>
          {isLoading ? <Spinner className="size-4" /> : "Bắt đầu"}
        </Button>
      </div>

      <div>
        {source === "BROWSER" ? (
          <CameraPreview />
        ) : (
          <div className="flex aspect-video w-full items-center justify-center rounded-xl bg-neutral-900 px-6 text-center text-sm text-neutral-300">
            Sau khi bấm Bắt đầu, bạn sẽ nhận địa chỉ máy chủ và khoá luồng để dán vào phần mềm phát.
          </div>
        )}
      </div>
    </div>
  );
}
