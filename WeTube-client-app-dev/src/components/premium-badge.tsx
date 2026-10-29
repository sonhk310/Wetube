import { Crown } from "lucide-react";
import { cn } from "@/lib/utils";

/** Huy hiệu nhỏ cạnh tên người dùng đang có WeTube Premium (bình luận, chat trực tiếp). */
export default function PremiumBadge({ className }: { className?: string }) {
  return (
    <span title="Thành viên WeTube Premium" className={cn("inline-flex shrink-0 align-middle", className)}>
      <Crown aria-label="Premium" className="size-3.5 fill-amber-400 text-amber-500" />
    </span>
  );
}
