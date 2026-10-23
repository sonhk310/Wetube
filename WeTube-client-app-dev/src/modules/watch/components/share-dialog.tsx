"use client";

import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useCopyToClipboard } from "@/lib/clipboard";

interface ShareDialogProps {
  readonly videoId: string;
  readonly title: string;
  /** Vị trí đang phát (giây), dùng cho tuỳ chọn "Bắt đầu tại". */
  readonly currentTime: number;
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
}

const formatTimestamp = (seconds: number) => {
  const total = Math.max(0, Math.floor(seconds));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const ss = s.toString().padStart(2, "0");
  return h > 0 ? `${h}:${m.toString().padStart(2, "0")}:${ss}` : `${m}:${ss}`;
};

export default function ShareDialog({
  videoId,
  title,
  currentTime,
  open,
  onOpenChange,
}: ShareDialogProps) {
  const [, copy] = useCopyToClipboard();
  const [withTime, setWithTime] = useState(false);
  // Chụp lại thời điểm lúc mở hộp thoại để đường dẫn không nhảy khi video đang chạy
  const [startAt, setStartAt] = useState(0);

  useEffect(() => {
    if (open) {
      setStartAt(Math.floor(currentTime));
      setWithTime(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const url = useMemo(() => {
    if (typeof window === "undefined") return "";
    const base = `${window.location.origin}/watch/${videoId}`;
    return withTime && startAt > 0 ? `${base}?t=${startAt}` : base;
  }, [videoId, withTime, startAt]);

  const handleCopy = async () => {
    const ok = await copy(url);
    if (ok) toast.success("Đã sao chép liên kết");
    else toast.error("Không sao chép được, hãy chép thủ công");
  };

  const open_ = (shareUrl: string) =>
    window.open(shareUrl, "_blank", "noopener,noreferrer,width=640,height=520");

  const encodedUrl = encodeURIComponent(url);
  const encodedTitle = encodeURIComponent(title);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Chia sẻ</DialogTitle>
        </DialogHeader>

        <div className="flex gap-2">
          <Input readOnly value={url} onFocus={(e) => e.currentTarget.select()} />
          <Button type="button" onClick={handleCopy}>
            Sao chép
          </Button>
        </div>

        <label className="flex items-center gap-2 text-sm cursor-pointer select-none">
          <input
            type="checkbox"
            checked={withTime}
            disabled={startAt <= 0}
            onChange={(e) => setWithTime(e.target.checked)}
          />
          Bắt đầu tại {formatTimestamp(startAt)}
        </label>

        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => open_(`https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`)}
          >
            Facebook
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() =>
              open_(`https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedTitle}`)
            }
          >
            X
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
