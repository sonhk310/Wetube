"use client";

import { ListChecks, Loader2, Upload } from "lucide-react";
import { cn } from "@/lib/utils";
import type { UploadState } from "./types";

function formatEta(seconds: number): string {
  if (seconds < 60) return `Còn ${Math.max(1, Math.ceil(seconds))} giây`;
  return `Còn ${Math.ceil(seconds / 60)} phút`;
}

/** Thanh mảnh dưới tiêu đề: chạy theo % tải lên, nhấp nháy khi đang xử lý, ẩn khi xong. */
export function UploadTopBar({ upload }: { upload: UploadState }) {
  if (upload.phase === "done") return <div className="h-1 border-b border-gray-200" />;
  return (
    <div className="h-1 bg-gray-200">
      <div
        className={cn(
          "h-full transition-all duration-300",
          upload.phase === "error" ? "w-full bg-red-500" : "bg-blue-600",
          upload.phase === "processing" && "w-full animate-pulse"
        )}
        style={upload.phase === "uploading" ? { width: `${upload.percent}%` } : undefined}
      />
    </div>
  );
}

interface UploadFooterStatusProps {
  upload: UploadState;
  /** Cạnh ngắn của video (px), để ghi HD / SD. */
  height: number;
  checksOk: boolean;
  onRetry: () => void;
}

/** Bên trái footer, như YouTube: icon tải lên, chất lượng, kiểm tra + dòng trạng thái. */
export function UploadFooterStatus({ upload, height, checksOk, onRetry }: UploadFooterStatusProps) {
  const done = upload.phase === "done";
  return (
    <div className="flex min-w-0 flex-1 items-center gap-3 text-sm text-gray-700">
      <Upload
        className={cn("h-5 w-5 shrink-0", done ? "text-blue-600" : "text-gray-500")}
        aria-label={done ? "Đã tải lên" : "Đang tải lên"}
      />
      {height > 0 && (
        <span
          title={done ? "Đã xử lý" : "Đang xử lý"}
          className={cn(
            "shrink-0 rounded border-2 px-1 text-[10px] font-bold leading-4",
            done ? "border-blue-600 text-blue-600" : "border-gray-500 text-gray-500"
          )}
        >
          {height >= 720 ? "HD" : "SD"}
        </span>
      )}
      <ListChecks
        className={cn("h-5 w-5 shrink-0", checksOk ? "text-green-600" : "text-gray-500")}
        aria-label={checksOk ? "Kiểm tra: không có vấn đề" : "Kiểm tra: còn lỗi ở bước Chi tiết"}
      />
      <span className="min-w-0 truncate">
        {upload.phase === "uploading" &&
          `Đang tải lên ${upload.percent}%${upload.etaSeconds !== undefined ? ` … ${formatEta(upload.etaSeconds)}` : ""}`}
        {upload.phase === "processing" && (
          <span className="inline-flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" /> Đang xử lý…
          </span>
        )}
        {done && "Đã tải lên xong"}
        {upload.phase === "error" && <span className="text-red-600">{upload.message}</span>}
      </span>
      {upload.phase === "error" && (
        <button type="button" onClick={onRetry} className="shrink-0 font-medium text-blue-600 hover:underline">
          Thử lại
        </button>
      )}
    </div>
  );
}
