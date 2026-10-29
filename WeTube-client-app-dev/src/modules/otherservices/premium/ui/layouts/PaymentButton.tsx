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
import { cn } from "@/lib/utils";
import { BASIC_PACK_ID, payWithMomo, payWithPaypal } from "../../lib/pay";

type Gateway = "momo" | "paypal";

interface PaymentButtonProps {
  /** Gói muốn mua; mặc định gói Basic (30 ngày). */
  subPackId?: string;
  /** Chữ trên nút; mặc định "Đăng ký / Gia hạn Premium ₫99.000 / 30 ngày". */
  label?: string;
  /** Dòng mô tả gói trong hộp thoại chọn cổng thanh toán. */
  summary?: string;
  className?: string;
}

const DEFAULT_BUTTON_CLASS =
  "bg-blue-600 text-white text-lg font-medium px-6 py-3 rounded-full hover:bg-blue-700 transition disabled:opacity-50";

/** Một nút mua Premium; bấm vào mở hộp thoại chọn cổng thanh toán (MoMo hoặc PayPal). */
export default function PaymentButton({
  subPackId = BASIC_PACK_ID,
  label,
  summary = "WeTube Premium gói Basic: ₫99.000 / 30 ngày",
  className,
}: PaymentButtonProps) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState<Gateway | null>(null);
  const {user, isPremium}=useAuth();
  const verb = isPremium ? "Gia hạn" : "Đăng ký";

  const pay = async (gateway: Gateway) => {
    // Đóng hộp thoại chọn trước: lỗi (nếu có) hiện ở hộp thoại thông báo riêng, không chồng lên nhau
    setOpen(false);
    setLoading(gateway);
    try {
      if (gateway === "momo") await payWithMomo(subPackId, user?.sub);
      else await payWithPaypal(subPackId);
    } finally {
      setLoading(null);
    }
  };

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        disabled={loading !== null}
        className={cn(DEFAULT_BUTTON_CLASS, className)}
      >
        {loading ? <Spinner className="size-6 mx-auto text-current" /> : label ?? `${verb} Premium ₫99.000 / 30 ngày`}
      </button>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Chọn cách thanh toán</DialogTitle>
            <DialogDescription>{summary}</DialogDescription>
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
