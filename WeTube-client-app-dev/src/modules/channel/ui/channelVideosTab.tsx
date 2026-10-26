"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import type { ChannelVideoSort } from "@/types/channel";
import ChannelVideos from "./channelVideos";

const SORTS: { sort: ChannelVideoSort; label: string }[] = [
  { sort: "latest", label: "Mới nhất" },
  { sort: "popular", label: "Phổ biến" },
  { sort: "oldest", label: "Cũ nhất" },
];

/** Thẻ Video của kênh: hàng chip sắp xếp rồi lưới video thường. */
export default function ChannelVideosTab() {
  const [sort, setSort] = useState<ChannelVideoSort>("latest");
  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        {SORTS.map((s) => (
          <button
            key={s.sort}
            type="button"
            onClick={() => setSort(s.sort)}
            className={cn(
              "h-8 rounded-lg px-3 text-sm font-medium",
              sort === s.sort ? "bg-black text-white" : "bg-gray-100 hover:bg-gray-200"
            )}
          >
            {s.label}
          </button>
        ))}
      </div>
      <ChannelVideos key={sort} type="video" sort={sort} size={24} />
    </div>
  );
}
