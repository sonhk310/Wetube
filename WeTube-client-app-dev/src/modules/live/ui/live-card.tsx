"use client";

import Image from "next/image";
import Link from "next/link";
import { timeAgo } from "@/lib/utils";
import { formatLiveDuration } from "../lib/format";
import type { LiveCard } from "../types";
import { LiveBadge } from "./live-badge";

const FALLBACK_AVATAR = "https://placehold.co/160x160?text=W";

/**
 * Thẻ một buổi phát. Đang phát: nhãn TRỰC TIẾP + số người xem. Đã phát: thời lượng + người xem cao nhất.
 * Ảnh bìa là banner của kênh (không có thì nền tối với ảnh đại diện kênh ở giữa).
 */
export default function LiveCardItem({ live }: { live: LiveCard }) {
  const avatar = live.channelPicture || FALLBACK_AVATAR;
  const isLive = live.status === "LIVE";
  const duration = formatLiveDuration(live.startedAt, live.endedAt);

  return (
    <Link href={`/live/${live.id}`} className="group block">
      <div className="relative aspect-video w-full overflow-hidden rounded-xl bg-neutral-900">
        {live.channelBackground ? (
          <Image src={live.channelBackground} alt="" fill sizes="(min-width: 1024px) 25vw, 50vw" className="object-cover" />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <Image src={avatar} alt="" width={72} height={72} className="size-[72px] rounded-full object-cover" />
          </div>
        )}
        {isLive ? (
          <>
            <LiveBadge className="absolute left-2 top-2" />
            <span className="absolute bottom-2 right-2 rounded bg-black/75 px-1.5 py-0.5 text-xs font-semibold text-white">
              {live.viewers.toLocaleString("vi-VN")} đang xem
            </span>
          </>
        ) : duration ? (
          <span className="absolute bottom-2 right-2 rounded bg-black/75 px-1.5 py-0.5 text-xs font-semibold text-white">
            {duration}
          </span>
        ) : null}
      </div>
      <div className="mt-3 flex gap-3">
        <Image src={avatar} alt="" width={36} height={36} className="size-9 shrink-0 rounded-full object-cover" />
        <div className="min-w-0">
          <p className="line-clamp-2 font-semibold leading-snug group-hover:text-blue-700">{live.title}</p>
          <p className="mt-0.5 truncate text-sm text-gray-600">{live.channelName}</p>
          {!isLive ? (
            <p className="text-xs text-gray-500">
              {live.endedAt ? `Đã phát ${timeAgo(live.endedAt)}` : "Đã phát"} · cao nhất {live.peakViewers.toLocaleString("vi-VN")} người xem
            </p>
          ) : null}
        </div>
      </div>
    </Link>
  );
}
