"use client";

import Image from "next/image";
import Link from "next/link";
import { formatCompact } from "@/lib/utils";
import type { ChannelVideo } from "@/types/channel";

// Chỉ cần 4 trường này nên dùng được cho cả video của kênh lẫn mục gợi ý ở trang chủ
type ShortCardVideo = Pick<ChannelVideo, "id" | "title" | "thumbnailUrl" | "totalView">;

/** Thẻ dọc 9:16 của một Short (trang kênh, trang chủ); bấm mở trình phát Shorts. */
export default function ChannelShortCard({ video, className }: { video: ShortCardVideo; className?: string }) {
  return (
    <Link href={`/shorts/${video.id}`} className={className}>
      <div className="relative aspect-[9/16] overflow-hidden rounded-xl bg-gray-300">
        <Image
          src={video.thumbnailUrl || "/images/thumbnail.png"}
          alt={video.title}
          fill
          sizes="210px"
          className="object-cover"
        />
      </div>
      <h3 className="mt-2 line-clamp-2 text-sm font-medium text-black">{video.title}</h3>
      <p className="mt-0.5 text-xs text-gray-600">{formatCompact(video.totalView)} lượt xem</p>
    </Link>
  );
}
