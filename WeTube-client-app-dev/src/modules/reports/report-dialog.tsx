"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { REPORT_REASONS, reportsApi } from "./reports-api";

/** Hộp thoại báo cáo vi phạm, dùng cho video ({ videoId }) và bình luận ({ commentId }). */
export default function ReportDialog({
  open,
  onOpenChange,
  target,
  title,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  target: { videoId: string } | { commentId: string };
  title: string;
}) {
  const [reason, setReason] = useState("SPAM");
  const [details, setDetails] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const close = (next: boolean) => {
    if (!next) {
      setMessage(null);
      setSent(false);
    }
    onOpenChange(next);
  };

  const submit = async () => {
    setBusy(true);
    setMessage(null);
    try {
      await reportsApi.create(target, reason, details || undefined);
      setMessage("Đã gửi báo cáo. Bạn theo dõi kết quả trong Nhật ký báo cáo.");
      setDetails("");
      setSent(true);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Gửi báo cáo thất bại");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className="space-y-2 text-sm">
          {Object.entries(REPORT_REASONS).map(([value, label]) => (
            <label key={value} className="flex items-center gap-2 cursor-pointer">
              <input
                type="radio"
                name="report-reason"
                value={value}
                checked={reason === value}
                onChange={() => setReason(value)}
              />
              {label}
            </label>
          ))}
          <textarea
            value={details}
            onChange={(e) => setDetails(e.target.value)}
            placeholder="Mô tả thêm (không bắt buộc)"
            maxLength={1000}
            className="w-full border rounded p-2 mt-2"
            rows={3}
          />
          {message && <p className="text-sm">{message}</p>}
        </div>
        <DialogFooter>
          {sent ? (
            <button onClick={() => close(false)} className="px-4 py-2 border rounded-md">
              Đóng
            </button>
          ) : (
            <button
              onClick={submit}
              disabled={busy}
              className="px-4 py-2 bg-black text-white rounded-md disabled:opacity-60"
            >
              Gửi báo cáo
            </button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
