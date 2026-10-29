"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { PREROLL_MAX_S, PREROLL_SKIP_AFTER_S, pickAd } from "./ads";

/**
 * Quảng cáo phủ lên khung phát trước khi video bắt đầu. Không có tiếng, video chưa chạy trong lúc này.
 * "Bỏ qua" hiện sau PREROLL_SKIP_AFTER_S giây, tự đóng sau PREROLL_MAX_S giây.
 */
export default function PrerollAd({ onDone }: { onDone: () => void }) {
  const [ad] = useState(pickAd);
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const started = Date.now();
    const id = window.setInterval(() => {
      const seconds = (Date.now() - started) / 1000;
      if (seconds >= PREROLL_MAX_S) {
        window.clearInterval(id);
        onDone();
      } else {
        setElapsed(seconds);
      }
    }, 250);
    return () => window.clearInterval(id);
  }, [onDone]);

  const Icon = ad.icon;
  const canSkip = elapsed >= PREROLL_SKIP_AFTER_S;
  const waitSeconds = Math.ceil(PREROLL_SKIP_AFTER_S - elapsed);

  return (
    <div
      role="region"
      aria-label="Quảng cáo"
      className={`absolute inset-0 z-30 flex flex-col items-center justify-center gap-3 px-6 text-center text-white ${ad.background}`}
    >
      <span className="absolute left-3 top-3 rounded-sm bg-yellow-300 px-1.5 py-0.5 text-[11px] font-semibold uppercase text-black">
        Quảng cáo
      </span>
      <Icon className="size-12 sm:size-16" strokeWidth={1.5} />
      <p className="text-lg font-bold sm:text-2xl">{ad.title}</p>
      <p className="max-w-md text-sm text-white/90 sm:text-base">{ad.description}</p>
      <Link
        href={ad.href}
        className="rounded-full bg-white px-4 py-1.5 text-sm font-semibold text-black hover:bg-white/90"
      >
        {ad.cta}
      </Link>
      <p className="text-xs text-white/80">
        Không muốn thấy quảng cáo?{" "}
        <Link href="/otherservices/premium" className="font-medium underline">
          Dùng Premium
        </Link>
      </p>
      <div className="absolute bottom-3 right-3">
        {canSkip ? (
          <button
            type="button"
            onClick={onDone}
            className="rounded bg-black/70 px-3 py-1.5 text-sm font-semibold text-white hover:bg-black"
          >
            Bỏ qua
          </button>
        ) : (
          <span className="rounded bg-black/50 px-3 py-1.5 text-sm text-white/90">
            Bỏ qua sau {waitSeconds} giây
          </span>
        )}
      </div>
    </div>
  );
}
