"use client";

import { useAuth } from '@/contexts/auth-context';
import React, { useState } from 'react'
import { Spinner } from "@/components/ui/spinner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { BASIC_PACK_ID, payWithMomo, payWithPaypal } from "../../lib/pay";

type Gateway = "momo" | "paypal";

/** Một nút mua Premium; bấm vào mở hộp thoại chọn cổng thanh toán (MoMo hoặc PayPal). */
export default function PaymentButton() {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState<Gateway | null>(null);
  const {user, isPremium}=useAuth();
  const verb = isPremium ? "Gia hạn" : "Đăng ký";

  const pay = async (gateway: Gateway) => {
    // Đóng hộp thoại chọn trước: lỗi (nếu có) hiện ở hộp thoại thông báo riêng, không chồng lên nhau
    setOpen(false);
    setLoading(gateway);
    try {
      if (gateway === "momo") await payWithMomo(BASIC_PACK_ID, user?.sub);
      else await payWithPaypal(BASIC_PACK_ID);
    } finally {
      setLoading(null);
    }
  };

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        disabled={loading !== null}
        className="bg-blue-600 text-white text-lg font-medium px-6 py-3 rounded-full hover:bg-blue-700 transition disabled:opacity-50"
      >
        {loading ? <Spinner className="size-6 mx-auto text-current" /> : `${verb} Premium ₫99.000 / 30 ngày`}
      </button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Chọn cách thanh toán</DialogTitle>
            <DialogDescription>WeTube Premium gói Basic: ₫99.000 / 30 ngày</DialogDescription>
          </DialogHeader>
          <div className="flex flex-col gap-3">
            <button
              onClick={() => pay("momo")}
              className="flex flex-col items-start rounded-xl bg-[#a50064] px-5 py-3 text-left text-white transition hover:bg-[#8c0055]"
            >
              <span className="text-base font-semibold">Thanh toán bằng MoMo</span>
              <span className="text-sm text-white/85">Ví MoMo, tính theo VND</span>
            </button>
            <button
              onClick={() => pay("paypal")}
              className="flex flex-col items-start rounded-xl bg-[#ffc439] px-5 py-3 text-left text-[#003087] transition hover:bg-[#f2b72d]"
            >
              <span className="text-base font-semibold">Thanh toán bằng PayPal</span>
              <span className="text-sm text-[#003087]/80">Tính theo USD, số tiền hiện ở trang PayPal</span>
            </button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
