import { BadgeCheck } from "lucide-react";
import { cn } from "@/lib/utils";

/** Huy hiệu nhỏ cạnh tên hội viên của kênh (bình luận dưới video, chat trực tiếp); rê chuột thấy tên cấp. */
export default function MemberBadge({ tier, className }: { tier: string; className?: string }) {
  return (
    <span title={`Hội viên · ${tier}`} className={cn("inline-flex shrink-0 align-middle", className)}>
      <BadgeCheck aria-label={`Hội viên ${tier}`} className="size-3.5 fill-emerald-500 text-white" />
    </span>
  );
}
