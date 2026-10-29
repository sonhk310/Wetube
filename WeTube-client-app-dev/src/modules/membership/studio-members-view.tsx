"use client";

import { useState } from "react";
import { BadgeCheck, Pencil, Plus, Trash2 } from "lucide-react";
import {
  useCreateTierMutation,
  useDeleteTierMutation,
  useGetMyMembersQuery,
  useUpdateTierMutation,
} from "@/app/api/membershipApi";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/contexts/auth-context";
import { apiErrorMessage } from "@/lib/apiError";
import { confirmDialog } from "@/lib/dialogs";
import { toastEmitter } from "@/lib/toastEmitter";
import { formatYmd } from "@/lib/utils";
import { formatVnd } from "@/modules/otherservices/premium/lib/plans";
import {
  MAX_PAID_TIERS,
  MAX_TIER_PRICE,
  MEMBERSHIP_DAYS,
  MIN_TIER_PRICE,
  WETUBE_FEE_PERCENT,
  type OwnerTier,
  type TierInput,
} from "@/types/membership";

/** Lỗi nhập cấp hội viên, cùng luật với backend; null nếu hợp lệ. */
export function validateTier(input: TierInput): string | null {
  const title = input.title.trim();
  if (title.length < 1 || title.length > 50) return "Tên cấp từ 1 đến 50 ký tự.";
  if (input.description.length > 500) return "Mô tả quyền lợi tối đa 500 ký tự.";
  if (!Number.isInteger(input.price) || input.price < MIN_TIER_PRICE || input.price > MAX_TIER_PRICE) {
    return `Giá từ ${formatVnd(MIN_TIER_PRICE)} đến ${formatVnd(MAX_TIER_PRICE)}, tính bằng số nguyên.`;
  }
  return null;
}

/** Studio → Hội viên (M1): các cấp hội viên của kênh, người đang là hội viên và doanh thu. */
export default function StudioMembersView() {
  const { user } = useAuth();
  const { data, isLoading, isError } = useGetMyMembersQuery();
  const [deleteTier] = useDeleteTierMutation();
  const [editing, setEditing] = useState<OwnerTier | "new" | null>(null);

  if (isLoading) {
    return (
      <div className="space-y-4 p-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }
  if (isError || !data) {
    return <p className="p-4 text-red-500">Không tải được dữ liệu hội viên.</p>;
  }

  const activeTiers = data.tiers.filter((t) => !t.archived);
  const canAdd = activeTiers.length < MAX_PAID_TIERS;

  const onDelete = async (tier: OwnerTier) => {
    const ok = await confirmDialog({
      title: `Xoá cấp "${tier.title}"?`,
      message:
        tier.memberCount > 0
          ? `Cấp đang có ${tier.memberCount} hội viên nên chỉ ngừng nhận hội viên mới; hội viên hiện tại dùng hết hạn.`
          : "Cấp đã có người mua trước đây thì chỉ ngừng nhận hội viên mới, chưa có thì bị xoá hẳn.",
      confirmLabel: "Xoá",
      destructive: true,
    });
    if (!ok) return;
    try {
      const { archived } = await deleteTier(tier.id).unwrap();
      toastEmitter.success(archived ? "Cấp đã ngừng nhận hội viên mới" : "Đã xoá cấp");
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không xoá được cấp, vui lòng thử lại."));
    }
  };

  return (
    <div className="flex flex-col gap-y-6 p-4">
      <div>
        <h1 className="text-2xl font-bold">Hội viên</h1>
        <p className="text-xs text-muted-foreground">
          Tạo các cấp hội viên để người xem ủng hộ kênh. Mỗi lần mua được {MEMBERSHIP_DAYS} ngày, không tự động gia hạn.
        </p>
      </div>

      <section aria-label="Doanh thu hội viên" className="grid gap-3 sm:grid-cols-3">
        <RevenueCard label="Tổng thu từ hội viên" value={data.revenue.total} />
        <RevenueCard label={`Phí WeTube (${WETUBE_FEE_PERCENT}%)`} value={data.revenue.wetubeFee} />
        <RevenueCard label="Kênh nhận" value={data.revenue.channelRevenue} highlight />
        <p className="text-xs text-muted-foreground sm:col-span-3">
          Số liệu để theo dõi; WeTube chưa chi trả tiền thật cho chủ kênh.
        </p>
      </section>

      <section aria-label="Các cấp hội viên" className="space-y-3">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">
            Các cấp ({activeTiers.length}/{MAX_PAID_TIERS})
          </h2>
          <Button className="rounded-full" onClick={() => setEditing("new")} disabled={!canAdd}>
            <Plus className="mr-1 size-4" />
            Thêm cấp
          </Button>
        </div>
        {!canAdd && (
          <p className="text-sm text-gray-500">
            Đã đủ {MAX_PAID_TIERS} cấp. Xoá một cấp để thêm cấp mới.
          </p>
        )}
        {data.tiers.length === 0 ? (
          <p className="rounded-lg border border-dashed p-6 text-center text-sm text-gray-500">
            Kênh chưa có cấp hội viên nào. Bấm “Thêm cấp” để người xem có thể tham gia.
          </p>
        ) : (
          <ul className="divide-y rounded-lg border">
            {data.tiers.map((tier) => (
              <li key={tier.id} className="flex flex-wrap items-start gap-x-4 gap-y-2 p-4">
                <div className="min-w-0 flex-1">
                  <p className="font-semibold">
                    {tier.title}
                    {tier.archived && (
                      <span className="ml-2 rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">
                        Đã ngừng nhận
                      </span>
                    )}
                  </p>
                  <p className="text-sm text-gray-700">
                    {formatVnd(tier.price)} / {MEMBERSHIP_DAYS} ngày · {tier.memberCount} hội viên
                  </p>
                  {tier.description && (
                    <p className="mt-1 whitespace-pre-line text-sm text-gray-600">{tier.description}</p>
                  )}
                </div>
                {!tier.archived && (
                  <div className="flex items-center gap-1">
                    <Button variant="outline" size="sm" className="rounded-full" onClick={() => setEditing(tier)}>
                      <Pencil className="mr-1 size-3.5" />
                      Sửa
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="size-8"
                      aria-label={`Xoá cấp ${tier.title}`}
                      title="Xoá cấp"
                      onClick={() => onDelete(tier)}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section aria-label="Hội viên hiện tại" className="space-y-3">
        <h2 className="text-lg font-semibold">Hội viên hiện tại ({data.members.length})</h2>
        {data.members.length === 0 ? (
          <p className="text-sm text-gray-500">Chưa có hội viên nào còn hạn.</p>
        ) : (
          <ul className="divide-y rounded-lg border">
            {data.members.map((m) => (
              <li key={m.userId} className="flex flex-wrap items-center gap-x-4 gap-y-1 p-3">
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium">
                    {m.name}
                    {m.handle && <span className="ml-1 text-sm font-normal text-gray-500">@{m.handle}</span>}
                  </p>
                </div>
                <p className="flex items-center gap-1 text-sm text-emerald-800">
                  <BadgeCheck className="size-4" />
                  {m.tierTitle}
                </p>
                <p className="text-sm text-gray-600">
                  {formatYmd(m.since)} → {formatYmd(m.until)}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      {editing && user?.channelId && (
        <TierDialog
          key={editing === "new" ? "new" : editing.id}
          channelId={user.channelId}
          tier={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}

function RevenueCard({ label, value, highlight }: { label: string; value: number; highlight?: boolean }) {
  return (
    <div className={`rounded-lg border p-4 ${highlight ? "border-emerald-300 bg-emerald-50" : ""}`}>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-xl font-bold">{formatVnd(value)}</p>
    </div>
  );
}

function TierDialog({ channelId, tier, onClose }: { channelId: string; tier: OwnerTier | null; onClose: () => void }) {
  const [createTier, { isLoading: creating }] = useCreateTierMutation();
  const [updateTier, { isLoading: updating }] = useUpdateTierMutation();
  const [title, setTitle] = useState(tier?.title ?? "");
  const [price, setPrice] = useState(tier ? String(tier.price) : "");
  const [description, setDescription] = useState(tier?.description ?? "");
  const [touched, setTouched] = useState(false);

  const input: TierInput = { title: title.trim(), price: price === "" ? NaN : Number(price), description };
  const error = validateTier(input);
  const saving = creating || updating;

  const save = async () => {
    setTouched(true);
    if (error) return;
    try {
      if (tier) await updateTier({ tierId: tier.id, input }).unwrap();
      else await createTier({ channelId, input }).unwrap();
      toastEmitter.success(tier ? "Đã lưu cấp hội viên" : "Đã thêm cấp hội viên");
      onClose();
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không lưu được cấp hội viên, vui lòng thử lại."));
    }
  };

  return (
    <Dialog open onOpenChange={(open) => !open && !saving && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{tier ? "Sửa cấp hội viên" : "Thêm cấp hội viên"}</DialogTitle>
          <DialogDescription>
            Tên và giá không trùng với cấp khác. Giá cho {MEMBERSHIP_DAYS} ngày; WeTube giữ {WETUBE_FEE_PERCENT}%.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-1">
            <label htmlFor="tier-title" className="text-sm font-medium">
              Tên cấp
            </label>
            <Input
              id="tier-title"
              value={title}
              maxLength={50}
              placeholder="Ví dụ: Người ủng hộ"
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>
          <div className="space-y-1">
            <label htmlFor="tier-price" className="text-sm font-medium">
              Giá (₫ / {MEMBERSHIP_DAYS} ngày)
            </label>
            <Input
              id="tier-price"
              type="number"
              inputMode="numeric"
              min={MIN_TIER_PRICE}
              max={MAX_TIER_PRICE}
              step={1000}
              value={price}
              placeholder={`${MIN_TIER_PRICE} – ${MAX_TIER_PRICE}`}
              onChange={(e) => setPrice(e.target.value)}
            />
          </div>
          <div className="space-y-1">
            <label htmlFor="tier-description" className="text-sm font-medium">
              Quyền lợi (không bắt buộc)
            </label>
            <Textarea
              id="tier-description"
              value={description}
              maxLength={500}
              rows={3}
              placeholder="Huy hiệu cạnh tên, nội dung riêng cho hội viên…"
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          {touched && error && <p className="text-sm text-red-600">{error}</p>}
        </div>

        <DialogFooter>
          <Button variant="ghost" className="rounded-full" onClick={onClose} disabled={saving}>
            Huỷ
          </Button>
          <Button className="rounded-full bg-blue-600 text-white hover:bg-blue-700" onClick={save} disabled={saving}>
            {tier ? "Lưu" : "Thêm cấp"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
