"use client";
import { API_PREFIX } from "@/constants/appConstant";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/apiFetch";
import { Skeleton } from "@/components/ui/skeleton";
import { BASIC_PACK_ID } from "../../lib/pay";
import { durationLabel, formatVnd, pricePerMonth, savingPercent } from "../../lib/plans";
import PaymentButton from "./PaymentButton";

type SubPack = {
  id: string;
  name: string;
  description: string;
  durationDays: number;
  price: number;
};

/** Các gói dài hạn (gói Basic 30 ngày đã có nút mua ở đầu trang): cùng quyền lợi, khác thời hạn và giá. */
export default function PremiumFeatures() {
  const [packs, setPacks] = useState<SubPack[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchSubPacks = async () => {
      try {
        const res = await apiFetch(`${API_PREFIX}/subpacks`);
        const data: SubPack[] = await res.json();
        setPacks(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error("Error fetching subpacks:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchSubPacks();
  }, []);

  if (loading) {
    return (
      <section className="py-20 px-4 bg-gray-50">
        <Skeleton className="h-12 w-2/3 max-w-xl mx-auto mb-16" />
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 max-w-6xl mx-auto">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-56 rounded-2xl" />
          ))}
        </div>
      </section>
    );
  }

  const basic = packs.find((p) => p.id === BASIC_PACK_ID);
  const longer = packs
    .filter((p) => p.id !== BASIC_PACK_ID)
    .sort((a, b) => a.durationDays - b.durationDays)
    .map((p) => ({
      ...p,
      saving: basic ? savingPercent(p.price, p.durationDays, basic.price, basic.durationDays) : 0,
    }));
  if (longer.length === 0) return null;
  const bestSaving = Math.max(...longer.map((p) => p.saving));

  return (
    <section className="py-20 px-4 bg-gray-50">
      <div className="text-center mb-16">
        <h2 className="text-4xl sm:text-5xl font-extrabold leading-tight text-gray-900">
          Chọn gói dài hơn để tiết kiệm hơn
        </h2>
        <p className="mt-4 text-lg font-normal text-gray-600">
          Mọi gói đều có đủ quyền lợi Premium, chỉ khác thời hạn và giá.
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 max-w-6xl mx-auto">
        {longer.map((pack) => {
          const best = bestSaving > 0 && pack.saving === bestSaving;
          const name = `Gói ${durationLabel(pack.durationDays)}`;
          return (
            <div
              key={pack.id}
              className={`flex flex-col rounded-2xl bg-white p-6 transition-all duration-300 shadow-sm hover:shadow-lg hover:scale-[1.02] ${
                best ? "border-2 border-blue-500" : "border border-gray-200"
              }`}
            >
              <div className="mb-4 flex items-center justify-between gap-3">
                <h3 className="text-xl font-semibold">{name}</h3>
                {best && (
                  <span className="rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
                    Tiết kiệm nhất
                  </span>
                )}
              </div>

              <p className="text-2xl font-bold text-gray-900">{formatVnd(pack.price)}</p>
              <p className="mb-1 text-sm font-normal text-gray-600">
                {pack.durationDays} ngày · khoảng {formatVnd(pricePerMonth(pack.price, pack.durationDays))} / tháng
              </p>
              {pack.saving > 0 && (
                <p className="mb-3 text-sm font-semibold text-green-700">
                  Rẻ hơn {pack.saving}% so với mua gói 30 ngày liên tục
                </p>
              )}
              <p className="mb-5 text-sm font-normal text-gray-700">
                Không quảng cáo · Tải video về máy · Phát nền
              </p>

              <PaymentButton
                subPackId={pack.id}
                label={`Chọn ${name}`}
                summary={`WeTube Premium ${name.toLowerCase()}: ${formatVnd(pack.price)} / ${pack.durationDays} ngày`}
                className="mt-auto w-full px-4 py-2 text-sm"
              />
            </div>
          );
        })}
      </div>
    </section>
  );
}
