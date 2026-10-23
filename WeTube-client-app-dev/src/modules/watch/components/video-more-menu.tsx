"use client";

import { useState } from "react";
import { EllipsisHorizontalIcon } from "@heroicons/react/24/outline";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAuth } from "@/contexts/auth-context";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { REPORT_REASONS, reportsApi } from "@/modules/reports/reports-api";

const SUBTITLE_LANGUAGES: Record<string, string> = {
  vi: "Tiếng Việt",
  en: "English",
  ko: "한국어",
  ja: "日本語",
  zh: "中文",
};

/** Nút "..." trên trang xem: báo cáo video (người xem) và tạo phụ đề AI (chủ video). */
export default function VideoMoreMenu({ videoId, channelId }: { videoId: string; channelId: string }) {
  const { user } = useAuth();
  const isOwner = !!user?.channelId && user.channelId === channelId;

  const [reportOpen, setReportOpen] = useState(false);
  const [reason, setReason] = useState("SPAM");
  const [details, setDetails] = useState("");
  const [subtitleOpen, setSubtitleOpen] = useState(false);
  const [lang, setLang] = useState("vi");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  if (!user?.sub) return null;

  const submitReport = async () => {
    setBusy(true);
    setMessage(null);
    try {
      await reportsApi.create(videoId, reason, details || undefined);
      setMessage("Đã gửi báo cáo. Bạn theo dõi kết quả trong Nhật ký báo cáo.");
      setDetails("");
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Gửi báo cáo thất bại");
    } finally {
      setBusy(false);
    }
  };

  const requestSubtitle = async () => {
    setBusy(true);
    setMessage(null);
    try {
      const res = await apiFetch(`${API_PREFIX}/videos/${videoId}/subtitles/ai?targetLang=${lang}`, {
        method: "POST",
      });
      const body = await res.json().catch(() => ({}));
      setMessage(res.ok ? body.message : body.message || `Lỗi ${res.status}`);
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Yêu cầu thất bại");
    } finally {
      setBusy(false);
    }
  };

  const closeAll = () => {
    setReportOpen(false);
    setSubtitleOpen(false);
    setMessage(null);
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer" aria-label="Thêm">
            <EllipsisHorizontalIcon className="size-6" />
          </button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {isOwner ? (
            <DropdownMenuItem onClick={() => setSubtitleOpen(true)}>Tạo phụ đề AI</DropdownMenuItem>
          ) : (
            <DropdownMenuItem onClick={() => setReportOpen(true)}>Báo cáo video</DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog open={reportOpen} onOpenChange={(open) => (open ? setReportOpen(true) : closeAll())}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Báo cáo video</DialogTitle>
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
            <button
              onClick={submitReport}
              disabled={busy}
              className="px-4 py-2 bg-black text-white rounded-md disabled:opacity-60"
            >
              Gửi báo cáo
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={subtitleOpen} onOpenChange={(open) => (open ? setSubtitleOpen(true) : closeAll())}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tạo phụ đề AI</DialogTitle>
          </DialogHeader>
          <div className="space-y-2 text-sm">
            <p>Gemini sẽ nghe video và tạo phụ đề dịch sang ngôn ngữ bạn chọn. Việc này mất khoảng 1 phút.</p>
            <select
              value={lang}
              onChange={(e) => setLang(e.target.value)}
              className="border rounded px-3 py-2 w-full"
            >
              {Object.entries(SUBTITLE_LANGUAGES).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
            {message && <p className="text-sm">{message}</p>}
          </div>
          <DialogFooter>
            <button
              onClick={requestSubtitle}
              disabled={busy}
              className="px-4 py-2 bg-black text-white rounded-md disabled:opacity-60"
            >
              Tạo phụ đề
            </button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
