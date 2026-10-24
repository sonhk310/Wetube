"use client";

import { cn } from "@/lib/utils";

interface UploadPreviewProps {
  src: string;
  isShort: boolean;
  reason: string;
  fileName: string;
}

/** Xem trước tệp trên máy; Shorts hiện ở khung dọc. */
export function UploadPreview({ src, isShort, reason, fileName }: UploadPreviewProps) {
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
      <div className="space-y-2 p-3 text-sm">
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
