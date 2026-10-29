"use client";

import PaymentButton from "./PaymentButton";
import { MOMO_DOWN_NOTE, MOMO_ENABLED } from "../../lib/pay";

export default function PremiumTrialBanner() {
  return (
    <section className="text-center py-20 px-4 bg-white">
      <h1 className="text-4xl sm:text-5xl font-extrabold mb-4">
        Dùng Premium ngay
      </h1>

      <p className="text-lg sm:text-xl text-gray-800 mb-6">
        Gói Basic chỉ ₫99.000 / 30 ngày • Thanh toán bằng {MOMO_ENABLED ? "MoMo hoặc PayPal" : "PayPal"}
      </p>
      {!MOMO_ENABLED && <p className="mb-6 text-sm text-amber-800">{MOMO_DOWN_NOTE}</p>}

      <PaymentButton />

      <div className="text-sm font-normal text-gray-500 mt-6">
        Thanh toán một lần cho mỗi kỳ, không tự động gia hạn.
        Mua thêm khi còn hạn thì được cộng dồn vào số ngày còn lại.
      </div>
    </section>
  );
}
