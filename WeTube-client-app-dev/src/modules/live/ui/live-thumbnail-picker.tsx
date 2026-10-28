"use client";

import { useRef } from "react";
import Image from "next/image";
import { ImagePlus, Loader2, Pencil } from "lucide-react";
import { toastEmitter } from "@/lib/toastEmitter";

/** Khớp giới hạn của backend (POST /api/live/{id}/thumbnail). */
export const THUMBNAIL_MAX_BYTES = 2 * 1024 * 1024;

/** Lỗi của ảnh không hợp lệ, null nếu dùng được. */
export function thumbnailProblem(file: File): string | null {
  if (file.type !== "image/png" && file.type !== "image/jpeg") return "Ảnh thu nhỏ phải là PNG hoặc JPEG.";
  if (file.size > THUMBNAIL_MAX_BYTES) return "Ảnh quá lớn (tối đa 2 MB).";
  return null;
}

/** Khung ảnh thu nhỏ 16:9 của buổi phát; bấm để chọn ảnh, kiểm tra loại và dung lượng trước khi gửi lên. */
export default function LiveThumbnailPicker({
  preview,
  busy = false,
  onPick,
}: {
  preview: string | null;
  busy?: boolean;
  onPick: (file: File) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = ""; // chọn lại cùng một tệp vẫn nhận
    if (!file) return;
    const problem = thumbnailProblem(file);
    if (problem) {
      toastEmitter.error(problem);
      return;
    }
    onPick(file);
  };

  return (
    <div className="space-y-2">
      <button
        type="button"
        disabled={busy}
        onClick={() => inputRef.current?.click()}
        className="group relative block aspect-video w-full max-w-64 overflow-hidden rounded-lg border bg-gray-100 disabled:cursor-wait"
      >
        {preview ? (
          <>
            <Image src={preview} alt="Ảnh thu nhỏ" fill sizes="256px" className="object-cover" unoptimized />
            <span className="absolute inset-0 flex items-center justify-center bg-black/30 opacity-0 transition-opacity group-hover:opacity-100">
              <Pencil className="size-5 text-white" />
            </span>
          </>
        ) : (
          <span className="flex h-full w-full flex-col items-center justify-center gap-1 text-sm text-gray-500">
            <ImagePlus className="size-5" />
            Chọn ảnh thu nhỏ
          </span>
        )}
        {busy ? (
          <span className="absolute inset-0 flex items-center justify-center bg-black/40">
            <Loader2 className="size-6 animate-spin text-white" />
          </span>
        ) : null}
      </button>
      <p className="text-xs text-gray-500">PNG hoặc JPEG, tối đa 2 MB, tự cắt 16:9. Không bắt buộc.</p>
      <input ref={inputRef} type="file" accept="image/png, image/jpeg" className="hidden" onChange={onChange} />
    </div>
  );
}
