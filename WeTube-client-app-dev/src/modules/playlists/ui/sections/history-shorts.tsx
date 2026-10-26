"use client";

import { useRef, useState } from "react";
import Image from "next/image";
import { ChevronRight, MoreVertical, Trash2 } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { cn, formatCompact } from "@/lib/utils";

export interface HistoryShort {
  videoId: string;
  videoTitle: string;
  thumbnailUrl?: string;
  totalView?: number;
}

function ShortCard({ short, onRemove }: { short: HistoryShort; onRemove: (videoId: string) => void }) {
  const router = useProgressRouter();
  return (
    <div className="w-[210px] shrink-0">
      <div
        className="relative aspect-[9/16] cursor-pointer overflow-hidden rounded-xl bg-gray-300"
        onClick={() => router.push(`/shorts/${short.videoId}`)}
      >
        <Image
          src={short.thumbnailUrl || "/images/thumbnail.png"}
          alt={short.videoTitle}
          fill
          sizes="210px"
          className="object-cover"
        />
      </div>
      <div className="mt-2 flex items-start gap-1">
        <div className="min-w-0 flex-1">
          <h3 className="line-clamp-2 text-sm font-medium text-black">{short.videoTitle}</h3>
          <p className="mt-0.5 text-xs text-gray-600">{formatCompact(short.totalView ?? 0)} lượt xem</p>
        </div>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              aria-label="Thêm tùy chọn"
              className="flex size-8 shrink-0 items-center justify-center rounded-full hover:bg-gray-100"
            >
              <MoreVertical className="size-4" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => onRemove(short.videoId)}>
              <Trash2 className="size-4" />
              Xoá khỏi nhật ký xem
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </div>
  );
}

/**
 * Như YouTube: Shorts trong nhật ký xem gom thành một hàng thẻ dọc.
 * wrap = true (tab Shorts) thì xếp nhiều hàng thay vì cuộn ngang.
 */
export default function HistoryShorts({
  shorts,
  onRemove,
  wrap = false,
  showTitle = true,
}: {
  shorts: HistoryShort[];
  onRemove: (videoId: string) => void;
  wrap?: boolean;
  showTitle?: boolean;
}) {
  const rowRef = useRef<HTMLDivElement>(null);
  const [atEnd, setAtEnd] = useState(false);

  if (!shorts.length) return null;

  const onScroll = () => {
    const el = rowRef.current;
    if (el) setAtEnd(el.scrollLeft + el.clientWidth >= el.scrollWidth - 4);
  };

  return (
    <section className="mb-6">
      {showTitle && (
        <h2 className="mb-4 flex items-center gap-2 text-xl font-bold">
          <svg viewBox="0 0 24 24" className="size-6 text-red-600" fill="currentColor" aria-hidden>
            <path d="M17.77 10.32l-1.2-.5L18 9.06a3.74 3.74 0 00-3.5-6.62L6 6.94a3.74 3.74 0 00.23 6.74l1.2.49L6 14.93a3.75 3.75 0 003.5 6.63l8.5-4.5a3.74 3.74 0 00-.23-6.74zM10 14.65v-5.3L15 12l-5 2.65z" />
          </svg>
          Shorts
        </h2>
      )}
      <div className="relative">
        <div
          ref={rowRef}
          onScroll={wrap ? undefined : onScroll}
          className={cn("flex gap-3", wrap ? "flex-wrap gap-y-6" : "overflow-x-auto pb-1 [scrollbar-width:none]")}
        >
          {shorts.map((s) => (
            <ShortCard key={s.videoId} short={s} onRemove={onRemove} />
          ))}
        </div>
        {!wrap && !atEnd && shorts.length > 3 && (
          <button
            type="button"
            aria-label="Cuộn sang phải"
            onClick={() => rowRef.current?.scrollBy({ left: 440, behavior: "smooth" })}
            className="absolute right-2 top-[130px] flex size-10 items-center justify-center rounded-full border bg-white shadow hover:bg-gray-100"
          >
            <ChevronRight className="size-5" />
          </button>
        )}
      </div>
    </section>
  );
}
