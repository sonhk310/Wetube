"use client";

import { useAuth } from '@/contexts/auth-context';
import { API_PREFIX } from '@/constants/appConstant';
import React, { useState } from 'react'
import { apiFetch } from "@/lib/apiFetch";
import { Spinner } from "@/components/ui/spinner";

export default function PaymentButton() {
  const [loading, setLoading] = useState(false);
  const {user}=useAuth();

  const handlePayment = async () => {
    setLoading(true);
    try {
      const res = await apiFetch(
        `${API_PREFIX}/payment/momo/create`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            userId: user?.sub,
            subPackId: "550e8400-e29b-41d4-a716-446655440000",
          }),
        }
      );

      if (!res.ok) throw new Error("Gọi API thanh toán thất bại");

      const data = await res.json();
      console.log("MoMo response:", data);

      if (data.payUrl) {
        window.location.href = data.payUrl;
      } else {
        alert("Không nhận được liên kết thanh toán từ MoMo");
      }
    } catch (err) {
      console.error(err);
      alert("Có lỗi khi tạo thanh toán.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handlePayment}
      disabled={loading}
      className="bg-blue-600 text-white text-lg font-medium px-6 py-3 rounded-full hover:bg-blue-700 transition disabled:opacity-50"
    >
      {loading ? <Spinner className="size-6 mx-auto text-current" /> : "Try it for 1 month 0 VND"}
    </button>
  );
}
