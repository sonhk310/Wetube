"use client";

import { useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";

type PaymentStatus = {
  success: boolean;
  orderId: string;
};

export default function PaymentResult() {
  const params = useSearchParams();
  const router = useRouter();
  const [status, setStatus] = useState<PaymentStatus | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Backend xác nhận thanh toán ở bước chuyển hướng rồi mới đưa người dùng về đây.
    const resultCode = params.get("paymentStatus") ?? params.get("resultCode");
    const orderId = params.get("orderId");

    if (!orderId || resultCode === null) {
      router.replace("/");
      return;
    }

    setStatus({ success: resultCode === "0", orderId });
    setLoading(false);
    const timer = setTimeout(() => router.replace("/"), 3000);
    return () => clearTimeout(timer);
  }, [params, router]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-600">
        Processing payment...
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center text-gray-700">
      {status?.success ? (
        <h1 className="text-green-600 text-2xl">Payment successful!</h1>
      ) : (
        <h1 className="text-red-600 text-2xl">Payment failed!</h1>
      )}
      <p>Order ID: {status?.orderId}</p>
      <p>The page will automatically return to Home after a few seconds....</p>
    </div>
  );
}
