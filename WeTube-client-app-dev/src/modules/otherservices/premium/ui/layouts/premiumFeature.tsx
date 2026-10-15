"use client";
import { useAuth } from "@/contexts/auth-context";
import { useEffect, useState } from "react";

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
        const res = await fetch("https://wetube.name.vn/api/subpacks");
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

  if (loading) return <p className="text-center">Loading...</p>;

  const handlePayment = async (subPackId: string) => {
    setPayLoading(subPackId);
    try {
      const res = await fetch(
        "https://wetube.name.vn/api/payment/momo/create",
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

      if (!res.ok) throw new Error("Payment API failed");

      const data = await res.json();
      console.log("MoMo response:", data);

      if (data.payUrl) window.location.href = data.payUrl;
      else alert("Payment link from MoMo not received");
    } catch (err) {
      console.error(err);
      alert("An error occurred while creating the payment");
    } finally {
      setPayLoading(null);
    }
  };

  return (
    <section className="py-20 px-4 bg-gray-50">
      <div className="text-center mb-16">
        <h2 className="text-4xl sm:text-5xl font-extrabold leading-tight text-gray-900">
          Join Premium and enjoy all features
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
              {plan.price.toLocaleString("en-US", { style: "currency", currency: "VND" })}
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
                ? "Processing..."
                : `Select ${plan.name}`}
            </button>
          </div>
        ))}
      </div>
    </section>
  );
}
