"use client";
import { useAuth } from "@/contexts/auth-context";
import { API_PREFIX } from "@/constants/appConstant";
import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/apiFetch";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";

type SubPack = {
  id: string;
  name: string;
  description: string;
  durationDays: number;
  price: number;
  active?: boolean;
};

export default function PremiumFeatures() {
  const [plans, setPlans] = useState<SubPack[]>([]);
  const [loading, setLoading] = useState(true);
  const [payLoading, setPayLoading] = useState<string | null>(null); 

  const {user}=useAuth();
  useEffect(() => {
    const fetchSubPacks = async () => {
      try {
        const res = await apiFetch(`${API_PREFIX}/subpacks`);
        const data: SubPack[] = await res.json();

        const updatedData = data
        .filter(plan => plan.id !== "550e8400-e29b-41d4-a716-446655440000")
        .map((plan, index) => ({
          ...plan,
          active: index === 0,
        }));

        setPlans(updatedData);
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

  const handlePayment = async (subPackId: string) => {
    setPayLoading(subPackId);
    try {
      const res = await apiFetch(
        `${API_PREFIX}/payment/momo/create`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            userId: user?.sub,
            subPackId,
            returnUrl: "https://wetube-client-app.onrender.com/",
          }),
        }
      );

      if (!res.ok) throw new Error("Gọi API thanh toán thất bại");

      const data = await res.json();
      console.log("MoMo response:", data);

      if (data.payUrl) window.location.href = data.payUrl;
      else alert("Không nhận được liên kết thanh toán từ MoMo");
    } catch (err) {
      console.error(err);
      alert("Có lỗi khi tạo thanh toán");
    } finally {
      setPayLoading(null);
    }
  };

  return (
    <section className="py-20 px-4 bg-gray-50">
      <div className="text-center mb-16">
        <h2 className="text-4xl sm:text-5xl font-extrabold leading-tight text-gray-900">
          Tham gia Premium để dùng mọi tính năng
        </h2>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 max-w-6xl mx-auto">
        {plans.map((plan) => (
          <div
            key={plan.id}
            className={`rounded-2xl p-6 transition-all duration-300 shadow-sm hover:shadow-lg hover:scale-[1.02] ${
              plan.active
                ? "border border-blue-500 bg-white"
                : "border border-gray-200 bg-white"
            }`}
          >
            <div className="flex items-center gap-3 mb-4">
              <h3 className="text-xl font-semibold">{plan.name}</h3>
            </div>

            <p className="text-lg font-bold text-gray-900 mb-2">
              {plan.price.toLocaleString("vi-VN", { style: "currency", currency: "VND" })}
            </p>
            <p className="text-sm text-gray-700 mb-1">{plan.description}</p>
            <p className="text-xs text-gray-500 mb-4">
              Duration: {plan.durationDays} days
            </p>

            <button
              onClick={() => handlePayment(plan.id)}
              disabled={payLoading === plan.id}
              className={`w-full rounded-full px-4 py-2 text-sm font-medium transition ${
                plan.active
                  ? "bg-blue-600 text-white hover:bg-blue-700"
                  : "bg-blue-100 text-blue-700 hover:bg-blue-200"
              } disabled:opacity-50`}
            >
              {payLoading === plan.id
                ? <Spinner className="size-5 mx-auto text-current" />
                : `Select ${plan.name}`}
            </button>
          </div>
        ))}
      </div>
    </section>
  );
}
