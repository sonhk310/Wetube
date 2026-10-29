"use client";

import { Crown } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { formatYmd } from "@/lib/utils";

/** Nhãn "Premium" cạnh logo WeTube cho người đang có Premium (rê chuột thấy ngày hết hạn); người khác không thấy. */
export default function PremiumLogoBadge() {
  const { user, isPremium } = useAuth();
  if (!isPremium) return null;

  return (
    <span
      title={user?.premiumUntil ? `WeTube Premium · đến ${formatYmd(user.premiumUntil)}` : "WeTube Premium"}
      className="ml-1 inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-amber-400 to-orange-500 px-2 py-0.5 text-[11px] font-bold uppercase tracking-wide text-white shadow-sm"
    >
      <Crown aria-hidden className="size-3 fill-white" />
      Premium
    </span>
  );
}
