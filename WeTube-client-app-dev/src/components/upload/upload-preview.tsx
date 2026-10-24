"use client";

import { Copy } from "lucide-react";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn } from "@/lib/utils";

interface UploadPreviewProps {
  src: string;
  isShort: boolean;
  reason: string;
  fileName: string;
  /** Id bản nháp, có sau khi tải lên xong; trước đó chưa có đường liên kết. */
  videoId?: string;
}

/** Xem trước tệp trên máy; Shorts hiện ở khung dọc. */
export function UploadPreview({ src, isShort, reason, fileName, videoId }: UploadPreviewProps) {
  const [, copy] = useCopyToClipboard();
  const link = videoId && typeof window !== "undefined" ? `${window.location.origin}/watch/${videoId}` : "";

  const copyLink = async () => {
    if (await copy(link)) toastEmitter.success("Đã sao chép liên kết");
    else toastEmitter.error("Không sao chép được, hãy chép thủ công");
  };

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-gray-50">
      <div className={cn("bg-black", isShort ? "flex justify-center" : "")}>
        {src && (
          <video
            src={src}
            controls
            className={cn("bg-black", isShort ? "aspect-[9/16] max-h-[36vh]" : "aspect-video w-full")}
          />
        )}
      </div>
      <div className="space-y-3 p-3 text-sm">
        <div>
          <p className="text-xs text-gray-500">Đường liên kết video</p>
          {link ? (
            <div className="flex items-center gap-1">
              <a
                href={link}
                target="_blank"
                rel="noreferrer"
                className="min-w-0 flex-1 truncate text-blue-600 hover:underline"
              >
                {link}
              </a>
              <button
                type="button"
                onClick={copyLink}
                title="Sao chép đường liên kết"
                className="shrink-0 rounded-full p-1.5 text-gray-600 hover:bg-gray-200"
              >
                <Copy className="h-4 w-4" />
                <span className="sr-only">Sao chép đường liên kết</span>
              </button>
            </div>
          ) : (
            <p className="text-gray-500">Có sau khi tải lên xong</p>
          )}
        </div>
        <div>
          <p className="text-xs text-gray-500">Tên tệp</p>
          <p className="truncate" title={fileName}>
            {fileName}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <span
            className={cn(
              "rounded px-2 py-0.5 text-xs font-medium",
              isShort ? "bg-red-100 text-red-700" : "bg-gray-200 text-gray-700"
            )}
          >
            {isShort ? "Video này sẽ là Shorts" : "Video thường"}
          </span>
          <span className="text-xs text-gray-500">{reason}</span>
        </div>
      </div>
    </div>
  );
}
