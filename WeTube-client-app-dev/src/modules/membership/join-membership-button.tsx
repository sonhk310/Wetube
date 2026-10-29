"use client";

import { useState } from "react";
import { BadgeCheck } from "lucide-react";
import { toast } from "sonner";
import { useGetChannelTiersQuery } from "@/app/api/membershipApi";
import { Spinner } from "@/components/ui/spinner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAuth } from "@/contexts/auth-context";
import { cn, formatYmd } from "@/lib/utils";
import { formatVnd } from "@/modules/otherservices/premium/lib/plans";
import { payMembershipWithPaypal } from "@/modules/otherservices/premium/lib/pay";
import { MEMBERSHIP_DAYS, type MemberTier, type MyMembership } from "@/types/membership";

/** Dòng giải thích điều gì xảy ra khi mua cấp đang chọn. */
function explain(selected: MemberTier, membership: MyMembership | null): string {
  if (!membership) return `Bạn được ${MEMBERSHIP_DAYS} ngày hội viên, không tự động gia hạn.`;
  if (membership.tierId === selected.id) {
    return `Gia hạn: cộng thêm ${MEMBERSHIP_DAYS} ngày sau ngày hết hạn hiện tại (${formatYmd(membership.until)}).`;
  }
  return "Đổi cấp: cấp mới bắt đầu ngay, số ngày còn lại của cấp hiện tại được quy đổi theo tỉ lệ giá.";
}

/**
 * Nút "Tham gia" hội viên kênh (M1), đặt cạnh nút Đăng ký ở trang kênh, trang xem và trang buổi phát. Kênh chưa có cấp
 * trả phí nào thì không hiện. Đã là hội viên thì thành "Hội viên · đến dd/mm" để gia hạn hoặc đổi cấp.
 */
export default function JoinMembershipButton({ channelId }: { channelId: string }) {
  const { user, login } = useAuth();
  const { data } = useGetChannelTiersQuery(channelId, { refetchOnMountOrArgChange: true });
  const [open, setOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [paying, setPaying] = useState(false);

  const tiers = data?.tiers ?? [];
  const membership = data?.membership ?? null;
  if (tiers.length === 0 && !membership) return null;

  const selected = tiers.find((t) => t.id === (selectedId ?? membership?.tierId)) ?? tiers[0];

  const onOpen = () => {
    if (!user?.sub) {
      toast("Hãy đăng nhập để tham gia hội viên", { action: { label: "Đăng nhập", onClick: login } });
      return;
    }
    setOpen(true);
  };

  const pay = async () => {
    if (!selected) return;
    setPaying(true);
    try {
      await payMembershipWithPaypal(selected.id);
    } finally {
      setPaying(false);
    }
  };

  return (
    <>
      <button
        type="button"
        onClick={onOpen}
        className={cn(
          "inline-flex h-9 shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full px-4 text-sm font-medium transition",
          membership
            ? "bg-emerald-50 text-emerald-800 hover:bg-emerald-100"
            : "bg-emerald-600 text-white hover:bg-emerald-700"
        )}
      >
        <BadgeCheck className="size-4" />
        {membership ? `Hội viên · đến ${formatYmd(membership.until)}` : "Tham gia"}
      </button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{membership ? "Hội viên của kênh" : "Tham gia hội viên"}</DialogTitle>
            <DialogDescription>
              {membership
                ? `Bạn đang là hội viên ${membership.tierTitle} đến ${formatYmd(membership.until)}.`
                : "Chọn cấp hội viên. Hội viên có huy hiệu cạnh tên khi bình luận và chat trực tiếp, cùng quyền lợi của cấp."}
            </DialogDescription>
          </DialogHeader>

          <div role="radiogroup" aria-label="Cấp hội viên" className="flex max-h-[50vh] flex-col gap-2 overflow-y-auto">
            {tiers.map((tier) => {
              const checked = selected?.id === tier.id;
              return (
                <button
                  key={tier.id}
                  type="button"
                  role="radio"
                  aria-checked={checked}
                  onClick={() => setSelectedId(tier.id)}
                  className={cn(
                    "rounded-xl border-2 p-3 text-left transition",
                    checked ? "border-emerald-600 bg-emerald-50" : "border-gray-200 hover:border-gray-300"
                  )}
                >
                  <span className="flex items-center justify-between gap-3">
                    <span className="font-semibold">
                      {tier.title}
                      {membership?.tierId === tier.id && (
                        <span className="ml-2 rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-semibold text-emerald-800">
                          Cấp hiện tại
                        </span>
                      )}
                    </span>
                    <span className="shrink-0 text-sm font-semibold">
                      {formatVnd(tier.price)} <span className="font-normal text-gray-500">/ {MEMBERSHIP_DAYS} ngày</span>
                    </span>
                  </span>
                  {tier.description && (
                    <span className="mt-1 block whitespace-pre-line text-sm text-gray-600">{tier.description}</span>
                  )}
                </button>
              );
            })}
          </div>

          {selected && <p className="text-sm text-gray-600">{explain(selected, membership)}</p>}

          <button
            type="button"
            onClick={pay}
            disabled={!selected || paying}
            className="flex flex-col items-start rounded-xl bg-[#ffc439] px-5 py-3 text-left text-[#003087] transition hover:bg-[#f2b72d] disabled:opacity-60"
          >
            {paying ? (
              <Spinner className="mx-auto size-6 text-current" />
            ) : (
              <>
                <span className="text-base font-semibold">
                  Thanh toán bằng PayPal{selected ? ` · ${formatVnd(selected.price)}` : ""}
                </span>
                <span className="text-sm text-[#003087]/80">Tính theo USD, số tiền hiện ở trang PayPal</span>
              </>
            )}
          </button>
        </DialogContent>
      </Dialog>
    </>
  );
}
