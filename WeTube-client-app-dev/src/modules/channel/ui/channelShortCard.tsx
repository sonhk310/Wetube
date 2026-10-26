"use client";

import Image from "next/image";
import Link from "next/link";
import { formatCompact } from "@/lib/utils";
import type { ChannelVideo } from "@/types/channel";

/** Thẻ dọc 9:16 của một Short trong trang kênh; bấm mở trình phát Shorts. */
export default function ChannelShortCard({ video, className }: { video: ChannelVideo; className?: string }) {
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
