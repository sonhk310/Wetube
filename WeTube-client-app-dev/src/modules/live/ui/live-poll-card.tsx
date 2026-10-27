"use client";

import { cn } from "@/lib/utils";
import type { PollView } from "../types";

/**
 * Một Poll với kết quả. Kết quả luôn hiện (kể cả trước khi chọn). Chỉ bấm chọn được khi Poll còn mở và mình chưa
 * chọn; chọn xong không đổi được. Khách bấm thì `onVote` mời đăng nhập (do nơi dùng quyết định).
 */
export default function LivePollCard({
  poll,
  onVote,
  busy = false,
}: {
  poll: PollView;
  onVote?: (option: number) => void;
  busy?: boolean;
}) {
  const canVote = poll.open && poll.myVote === null && !!onVote;
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-3">
      <div className="flex items-start justify-between gap-2">
        <p className="font-semibold">{poll.question}</p>
        <span
          className={cn(
            "shrink-0 rounded-full px-2 py-0.5 text-[11px] font-semibold",
            poll.open ? "bg-green-100 text-green-700" : "bg-gray-100 text-gray-600"
          )}
        >
          {poll.open ? "Đang mở" : "Đã đóng"}
        </span>
      </div>
      <ul className="mt-2 space-y-2">
        {poll.options.map((option, index) => {
          const count = poll.counts[index] ?? 0;
          const percent = poll.total > 0 ? Math.round((count * 100) / poll.total) : 0;
          const mine = poll.myVote === index;
          return (
            <li key={index}>
              <button
                type="button"
                disabled={!canVote || busy}
                onClick={() => onVote?.(index)}
                className={cn(
                  "relative w-full overflow-hidden rounded-lg border px-3 py-2 text-left text-sm",
                  mine ? "border-blue-600" : "border-gray-200",
                  canVote && !busy ? "hover:bg-gray-50" : "cursor-default"
                )}
              >
                <span
                  className={cn("absolute inset-y-0 left-0", mine ? "bg-blue-100" : "bg-gray-100")}
                  style={{ width: `${percent}%` }}
                />
                <span className="relative flex items-center justify-between gap-2">
                  <span className={cn("break-words", mine && "font-semibold")}>{option}</span>
                  <span className="shrink-0 text-xs text-gray-600">
                    {percent}% · {count}
                  </span>
                </span>
              </button>
            </li>
          );
        })}
      </ul>
      <p className="mt-2 text-xs text-gray-500">{poll.total.toLocaleString("vi-VN")} phiếu</p>
    </div>
  );
}
