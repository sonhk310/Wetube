"use client";

import { useEffect, useState, type RefObject } from "react";
import { Check, ChevronLeft, ChevronRight, Gauge, Settings, Subtitles } from "lucide-react";
import { cn } from "@/lib/utils";
import type { VideoSubtitleDto } from "@/types/videoSubtitleDto";

export const PLAYBACK_RATES = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2];

const LANGUAGE_LABELS: Record<string, string> = {
  vi: "Tiếng Việt",
  en: "Tiếng Anh",
  ko: "Tiếng Hàn",
  ja: "Tiếng Nhật",
  zh: "Tiếng Trung",
};

export const subtitleLabel = (language: string) =>
  LANGUAGE_LABELS[language?.toLowerCase()] ?? language ?? "Phụ đề";

const rateLabel = (rate: number) => (rate === 1 ? "Chuẩn" : `${rate}`);

interface SettingsMenuProps {
  rootRef: RefObject<HTMLDivElement | null>;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rate: number;
  onRate: (rate: number) => void;
  subtitles: VideoSubtitleDto[];
  /** -1: tắt phụ đề */
  subtitleIndex: number;
  onSubtitle: (index: number) => void;
  /** Chiều cao tối đa của bảng (để không tràn khỏi khung video). */
  maxHeight: number;
}

type Page = "main" | "rate" | "subtitles";

export default function SettingsMenu({
  rootRef,
  open,
  onOpenChange,
  rate,
  onRate,
  subtitles,
  subtitleIndex,
  onSubtitle,
  maxHeight,
}: SettingsMenuProps) {
  const [page, setPage] = useState<Page>("main");

  useEffect(() => {
    if (!open) setPage("main");
  }, [open]);

  const row = "flex w-full items-center gap-3 px-4 py-2.5 text-left hover:bg-white/10";

  return (
    <div ref={rootRef} className="relative">
      <button
        type="button"
        aria-label="Cài đặt"
        title="Cài đặt"
        aria-expanded={open}
        onClick={() => onOpenChange(!open)}
        className="flex h-10 w-10 items-center justify-center rounded-full text-white transition hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-white"
      >
        <Settings size={22} className={cn("transition-transform duration-200", open && "rotate-45")} />
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 bottom-12 w-64 overflow-y-auto rounded-xl bg-neutral-900/45 py-2 text-sm text-white shadow-lg ring-1 ring-white/15 backdrop-blur-xl backdrop-saturate-150"
          style={{ maxHeight }}
        >
          {page === "main" && (
            <>
              <button type="button" role="menuitem" className={row} onClick={() => setPage("rate")}>
                <Gauge size={20} />
                <span className="flex-1">Tốc độ phát</span>
                <span className="text-white/70">{rateLabel(rate)}</span>
                <ChevronRight size={18} />
              </button>
              {subtitles.length > 0 && (
                <button type="button" role="menuitem" className={row} onClick={() => setPage("subtitles")}>
                  <Subtitles size={20} />
                  <span className="flex-1">Phụ đề</span>
                  <span className="truncate text-white/70">
                    {subtitleIndex >= 0 ? subtitleLabel(subtitles[subtitleIndex]?.language) : "Tắt"}
                  </span>
                  <ChevronRight size={18} />
                </button>
              )}
            </>
          )}

          {page !== "main" && (
            <button
              type="button"
              className={cn(row, "mb-1 border-b border-white/15 pb-3 font-medium")}
              onClick={() => setPage("main")}
            >
              <ChevronLeft size={20} />
              {page === "rate" ? "Tốc độ phát" : "Phụ đề"}
            </button>
          )}

          {page === "rate" &&
            PLAYBACK_RATES.map((r) => (
              <button
                key={r}
                type="button"
                role="menuitemradio"
                aria-checked={r === rate}
                className={row}
                onClick={() => {
                  onRate(r);
                  onOpenChange(false);
                }}
              >
                <span className="w-5">{r === rate && <Check size={18} />}</span>
                {rateLabel(r)}
              </button>
            ))}

          {page === "subtitles" &&
            [{ index: -1, label: "Tắt" }, ...subtitles.map((s, index) => ({ index, label: subtitleLabel(s.language) }))].map(
              ({ index, label }) => (
                <button
                  key={index}
                  type="button"
                  role="menuitemradio"
                  aria-checked={index === subtitleIndex}
                  className={row}
                  onClick={() => {
                    onSubtitle(index);
                    onOpenChange(false);
                  }}
                >
                  <span className="w-5">{index === subtitleIndex && <Check size={18} />}</span>
                  {label}
                </button>
              )
            )}
        </div>
      )}
    </div>
  );
}
