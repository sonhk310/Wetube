"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { useAuth } from "@/contexts/auth-context";

type PaymentStatus = {
  success: boolean;
  orderId: string;
};

export default function PaymentResult() {
  const params = useSearchParams();
  const router = useProgressRouter();
  const { refresh } = useAuth();
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

    const success = resultCode === "0";
    setStatus({ success, orderId });
    setLoading(false);
    // Quyền lợi Premium có ngay, không cần đăng nhập lại
    if (success) void refresh();
    const timer = setTimeout(() => router.replace("/"), 3000);
    return () => clearTimeout(timer);
  }, [params, router, refresh]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center text-gray-600">
        Đang xử lý thanh toán...
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center text-gray-700">
      {status?.success ? (
        <h1 className="text-green-600 text-2xl">Thanh toán thành công!</h1>
      ) : (
        <h1 className="text-red-600 text-2xl">Thanh toán thất bại!</h1>
      )}
      <p>Order ID: {status?.orderId}</p>
      <p>Trang sẽ tự quay về Trang chủ sau vài giây...</p>
    </div>
  );
}
