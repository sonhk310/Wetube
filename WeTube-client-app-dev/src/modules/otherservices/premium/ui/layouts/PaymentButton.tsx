"use client";

import { useAuth } from '@/contexts/auth-context';
import React, { useState } from 'react'
import { Spinner } from "@/components/ui/spinner";
import { BASIC_PACK_ID, payWithMomo, payWithPaypal } from "../../lib/pay";

type Gateway = "momo" | "paypal";

export default function PaymentButton() {
  const [loading, setLoading] = useState<Gateway | null>(null);
  const {user, isPremium}=useAuth();
  const verb = isPremium ? "Gia hạn" : "Đăng ký";

  const pay = async (gateway: Gateway) => {
    setLoading(gateway);
    try {
      if (gateway === "momo") await payWithMomo(BASIC_PACK_ID, user?.sub);
      else await payWithPaypal(BASIC_PACK_ID);
    } finally {
      setLoading(null);
    }
  };

  return (
    <div className="flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
      <button
        onClick={() => pay("momo")}
        disabled={loading !== null}
        className="bg-blue-600 text-white text-lg font-medium px-6 py-3 rounded-full hover:bg-blue-700 transition disabled:opacity-50"
      >
        {loading === "momo" ? <Spinner className="size-6 mx-auto text-current" /> : `${verb} Premium ₫99.000 / 30 ngày bằng MoMo`}
      </button>
      <button
        onClick={() => pay("paypal")}
        disabled={loading !== null}
        className="bg-[#ffc439] text-[#003087] text-lg font-medium px-6 py-3 rounded-full hover:bg-[#f2b72d] transition disabled:opacity-50"
      >
        {loading === "paypal" ? <Spinner className="size-6 mx-auto text-current" /> : "Thanh toán bằng PayPal (tính theo USD)"}
      </button>
    </div>
  );
}
