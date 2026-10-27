"use client";

import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { toastEmitter } from "@/lib/toastEmitter";
import { ChevronLeft, ChevronRight, Facebook, Linkedin, Mail, Send, Share2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useCopyToClipboard } from "@/lib/clipboard";
import { cn } from "@/lib/utils";

interface ShareDialogProps {
  readonly videoId?: string;
  /** Liên kết cố định (vd. kênh) thay cho liên kết video; khi đó không có "Bắt đầu tại". */
  readonly link?: string;
  readonly title: string;
  /** Vị trí đang phát (giây), dùng cho tuỳ chọn "Bắt đầu tại". */
  readonly currentTime?: number;
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

/** "1:02:03" / "2:05" / "125" -> số giây; null nếu không hợp lệ. */
const parseTimestamp = (text: string): number | null => {
  const parts = text.trim().split(":");
  if (parts.length > 3 || parts.some((p) => !/^\d+$/.test(p))) return null;
  return parts.reduce((total, part) => total * 60 + Number(part), 0);
};

// Logo X và WhatsApp (lucide không có)
const XLogo = () => (
  <svg viewBox="0 0 24 24" className="size-6" fill="currentColor" aria-hidden>
    <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
  </svg>
);

const WhatsAppLogo = () => (
  <svg viewBox="0 0 24 24" className="size-7" fill="currentColor" aria-hidden>
    <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 0 1-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 0 1-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 0 1 2.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0 0 12.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 0 0 5.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 0 0-3.48-8.413Z" />
  </svg>
);

interface ShareTarget {
  label: string;
  icon: ReactNode;
  /** Lớp màu của vòng tròn. */
  circle: string;
  onClick: () => void;
}

export default function ShareDialog({
  videoId,
  link,
  title,
  currentTime = 0,
  open,
  onOpenChange,
}: ShareDialogProps) {
  const [, copy] = useCopyToClipboard();
  const [withTime, setWithTime] = useState(false);
  // Chụp lại thời điểm lúc mở hộp thoại để đường dẫn không nhảy khi video đang chạy
  const [startAt, setStartAt] = useState(0);
  // Chữ trong ô thời gian (người dùng sửa được, như YouTube)
  const [timeText, setTimeText] = useState("0:00");
  const [canNativeShare, setCanNativeShare] = useState(false);
  const rowRef = useRef<HTMLDivElement>(null);
  const [scroll, setScroll] = useState({ left: false, right: false });

  useEffect(() => {
    if (open) {
      const t = Math.floor(currentTime);
      setStartAt(t);
      setTimeText(formatTimestamp(t));
      setWithTime(false);
      setCanNativeShare(typeof navigator !== "undefined" && typeof navigator.share === "function");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const url = useMemo(() => {
    if (link) return link;
    if (typeof window === "undefined") return "";
    const base = `${window.location.origin}/watch/${videoId}`;
    return withTime && startAt > 0 ? `${base}?t=${startAt}` : base;
  }, [link, videoId, withTime, startAt]);

  const commitTime = () => {
    const parsed = parseTimestamp(timeText);
    if (parsed === null) {
      setTimeText(formatTimestamp(startAt));
      return;
    }
    setStartAt(parsed);
    setTimeText(formatTimestamp(parsed));
  };

  const handleCopy = async () => {
    const ok = await copy(url);
    if (ok) toastEmitter.success("Đã sao chép liên kết");
    else toastEmitter.error("Không sao chép được, hãy chép thủ công");
  };

  const popup = (shareUrl: string) =>
    window.open(shareUrl, "_blank", "noopener,noreferrer,width=640,height=560");

  const encodedUrl = encodeURIComponent(url);
  const encodedTitle = encodeURIComponent(title);

  const targets: ShareTarget[] = [
    {
      label: "Facebook",
      icon: <Facebook className="size-7" fill="currentColor" strokeWidth={0} />,
      circle: "bg-[#1877F2] text-white",
      onClick: () => popup(`https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`),
    },
    {
      label: "WhatsApp",
      icon: <WhatsAppLogo />,
      circle: "bg-[#25D366] text-white",
      onClick: () => popup(`https://wa.me/?text=${encodeURIComponent(`${title} ${url}`)}`),
    },
    {
      label: "X",
      icon: <XLogo />,
      circle: "bg-black text-white",
      onClick: () => popup(`https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedTitle}`),
    },
    {
      label: "Telegram",
      icon: <Send className="size-6 -ml-0.5" />,
      circle: "bg-[#229ED9] text-white",
      onClick: () => popup(`https://t.me/share/url?url=${encodedUrl}&text=${encodedTitle}`),
    },
    {
      label: "LinkedIn",
      icon: <Linkedin className="size-6" />,
      circle: "bg-[#0A66C2] text-white",
      onClick: () => popup(`https://www.linkedin.com/sharing/share-offsite/?url=${encodedUrl}`),
    },
    {
      label: "Email",
      icon: <Mail className="size-6" />,
      circle: "bg-gray-500 text-white",
      onClick: () => {
        window.location.href = `mailto:?subject=${encodedTitle}&body=${encodeURIComponent(`${title}\n${url}`)}`;
      },
    },
    ...(canNativeShare
      ? [
          {
            // Điện thoại: mở bảng chia sẻ của máy (Zalo, Messenger...)
            label: "Khác",
            icon: <Share2 className="size-6" />,
            circle: "bg-gray-200 text-gray-800",
            onClick: () => {
              navigator.share({ title, url }).catch(() => {});
            },
          },
        ]
      : []),
  ];

  // Mũi tên chỉ hiện ở phía còn cuộn được (giống thanh chip trang chủ)
  const updateScroll = () => {
    const el = rowRef.current;
    if (!el) return;
    setScroll({
      left: el.scrollLeft > 4,
      right: el.scrollLeft + el.clientWidth < el.scrollWidth - 4,
    });
  };

  useEffect(() => {
    if (!open) return;
    const id = window.requestAnimationFrame(updateScroll);
    return () => window.cancelAnimationFrame(id);
  }, [open, canNativeShare]);

  const scrollBy = (dir: 1 | -1) =>
    rowRef.current?.scrollBy({ left: dir * 240, behavior: "smooth" });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {/* Cột lưới mặc định rộng theo nội dung: hàng biểu tượng dài sẽ đẩy cả hộp thoại tràn ra ngoài */}
      <DialogContent className="max-w-lg grid-cols-[minmax(0,1fr)]">
        <DialogHeader>
          <DialogTitle>Chia sẻ</DialogTitle>
        </DialogHeader>

        <div className="relative">
          <div
            ref={rowRef}
            onScroll={updateScroll}
            className="flex gap-4 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
          >
            {targets.map((t) => (
              <button
                key={t.label}
                type="button"
                onClick={t.onClick}
                className="flex w-16 shrink-0 flex-col items-center gap-1.5 text-xs text-gray-700"
              >
                <span
                  className={cn(
                    "flex size-14 items-center justify-center rounded-full transition-opacity hover:opacity-85",
                    t.circle
                  )}
                >
                  {t.icon}
                </span>
                {t.label}
              </button>
            ))}
          </div>
          {scroll.left && (
            <button
              type="button"
              aria-label="Trước"
              onClick={() => scrollBy(-1)}
              className="absolute left-0 top-4 flex size-9 items-center justify-center rounded-full bg-white shadow-md hover:bg-gray-100"
            >
              <ChevronLeft className="size-5" />
            </button>
          )}
          {scroll.right && (
            <button
              type="button"
              aria-label="Tiếp"
              onClick={() => scrollBy(1)}
              className="absolute right-0 top-4 flex size-9 items-center justify-center rounded-full bg-white shadow-md hover:bg-gray-100"
            >
              <ChevronRight className="size-5" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-2 rounded-xl border bg-gray-50 py-1.5 pl-3 pr-1.5">
          <input
            readOnly
            value={url}
            onFocus={(e) => e.currentTarget.select()}
            className="min-w-0 flex-1 bg-transparent text-sm outline-none"
            aria-label={link ? "Liên kết" : "Liên kết video"}
          />
          <button
            type="button"
            onClick={handleCopy}
            className="shrink-0 rounded-full bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700"
          >
            Sao chép
          </button>
        </div>

        {!link && (
          <div className="flex items-center gap-2 border-t pt-4 text-sm">
            <label className="flex cursor-pointer select-none items-center gap-2">
              <input
                type="checkbox"
                className="size-4"
                checked={withTime}
                onChange={(e) => setWithTime(e.target.checked)}
              />
              Bắt đầu tại
            </label>
            <input
              value={timeText}
              disabled={!withTime}
              onChange={(e) => setTimeText(e.target.value)}
              onBlur={commitTime}
              onKeyDown={(e) => {
                if (e.key === "Enter") e.currentTarget.blur();
              }}
              inputMode="numeric"
              aria-label="Thời điểm bắt đầu"
              className="w-20 border-b border-gray-300 bg-transparent px-1 py-0.5 outline-none focus:border-gray-800 disabled:text-gray-400"
            />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
