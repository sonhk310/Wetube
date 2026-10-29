"use client";

import { useState } from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { pickAd } from "./ads";
import { useShowAds } from "./use-show-ads";

/**
 * Thẻ quảng cáo nội bộ. "feed": cùng cỡ thẻ video ở trang chủ. "side": hàng ngang gọn ở cột Tiếp theo
 * của trang xem. Không hiện cho Premium.
 */
export default function AdCard({ variant, className }: { variant: "feed" | "side"; className?: string }) {
  const showAds = useShowAds();
  const [ad] = useState(pickAd);
  if (!showAds) return null;

  const Icon = ad.icon;
  const side = variant === "side";

  return (
    <aside aria-label="Quảng cáo" className={cn("w-full", className)}>
      <div className={cn(side ? "grid grid-cols-11 gap-2" : "flex flex-col gap-3")}>
        <Link
          href={ad.href}
          className={cn(
            "relative flex aspect-video items-center justify-center overflow-hidden rounded-lg text-white",
            ad.background,
            side ? "col-span-5" : "rounded-xl"
          )}
        >
          <Icon className={side ? "size-8" : "size-14"} strokeWidth={1.5} />
        </Link>
        <div className={cn("flex min-w-0 flex-col gap-1", side && "col-span-6")}>
          <p className={cn("font-bold leading-snug", side ? "line-clamp-2 text-sm" : "text-base")}>{ad.title}</p>
          <p className={cn("text-gray-600", side ? "line-clamp-2 text-xs" : "text-sm")}>{ad.description}</p>
          <div className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1">
            <span className="rounded-sm bg-yellow-300 px-1 text-[10px] font-semibold uppercase text-black">
              Quảng cáo
            </span>
            <Link href={ad.href} className="text-xs font-semibold text-blue-600 hover:underline">
              {ad.cta}
            </Link>
          </div>
          <p className="text-[11px] text-gray-500">
            Không muốn thấy quảng cáo?{" "}
            <Link href="/otherservices/premium" className="font-medium underline">
              Dùng Premium
            </Link>
          </p>
        </div>
      </div>
    </aside>
  );
}
