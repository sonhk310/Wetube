import { Eye } from "lucide-react";
import { cn } from "@/lib/utils";

/** Nhãn đỏ TRỰC TIẾP. */
export function LiveBadge({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-md bg-red-600 px-1.5 py-0.5 text-[11px] font-bold uppercase tracking-wide text-white",
        className
      )}
    >
      <span className="size-1.5 animate-pulse rounded-full bg-white" />
      Trực tiếp
    </span>
  );
}

/** "12 người đang xem". */
export function ViewerCount({ count, className }: { count: number; className?: string }) {
  return (
    <span className={cn("inline-flex items-center gap-1 text-sm text-gray-600", className)}>
      <Eye className="size-4" />
      {count.toLocaleString("vi-VN")} người đang xem
    </span>
  );
}
