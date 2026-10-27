"use client";

import { CheckCircle2, CircleAlert, Loader2 } from "lucide-react";
import type { FormErrors, StepId, UploadState } from "./types";

interface StepChecksProps {
  errors: FormErrors;
  upload: UploadState;
  hasThumbnail: boolean;
  isShort: boolean;
  shortsReason: string;
  goTo: (id: StepId) => void;
}

type Row = { ok: boolean | "pending"; title: string; detail: string; step?: StepId };

export function StepChecks({ errors, upload, hasThumbnail, isShort, shortsReason, goTo }: StepChecksProps) {
  const uploadRow: Row =
    upload.phase === "done"
      ? { ok: true, title: "Tệp đã tải lên xong", detail: "Video đã được lưu dưới dạng bản nháp." }
      : upload.phase === "error"
        ? { ok: false, title: "Tải lên thất bại", detail: upload.message }
        : {
            ok: "pending",
            title: upload.phase === "processing" ? "Đang xử lý" : `Đang tải lên ${upload.percent}%`,
            detail: "Bạn vẫn có thể bấm Đăng, video sẽ được đăng ngay khi tải lên xong.",
          };

  const rows: Row[] = [
    {
      ok: !errors.title && !errors.description && !errors.tags && !errors.topicId,
      title: "Thông tin video",
      detail: errors.title || errors.description || errors.tags || errors.topicId || "Tiêu đề, mô tả, thẻ và chủ đề hợp lệ.",
      step: "details",
    },
    uploadRow,
    {
      ok: hasThumbnail && !errors.thumbnailFile,
      title: "Ảnh thu nhỏ",
      detail: errors.thumbnailFile || (hasThumbnail ? "Đã có ảnh thu nhỏ." : "Chưa có ảnh thu nhỏ."),
      step: "details",
    },
    { ok: true, title: isShort ? "Loại: Shorts" : "Loại: Video thường", detail: shortsReason },
  ];

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Kiểm tra</h2>
      <p className="text-sm text-gray-500">Kiểm tra nhanh trước khi đăng.</p>
      <ul className="divide-y divide-gray-200 rounded-lg border border-gray-200">
        {rows.map((row) => (
          <li key={row.title} className="flex items-start gap-3 p-3">
            {row.ok === "pending" ? (
              <Loader2 className="mt-0.5 h-5 w-5 shrink-0 animate-spin text-gray-500" />
            ) : row.ok ? (
              <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-green-600" />
            ) : (
              <CircleAlert className="mt-0.5 h-5 w-5 shrink-0 text-red-600" />
            )}
            <div className="min-w-0 flex-1">
              <p className="font-medium">{row.title}</p>
              <p className="text-sm text-gray-600">{row.detail}</p>
            </div>
            {row.ok === false && row.step && (
              <button
                type="button"
                onClick={() => goTo(row.step!)}
                className="text-sm font-medium text-blue-600 hover:underline"
              >
                Sửa
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
