"use client";

import Image from 'next/image';
import { Crown } from "lucide-react";
import PaymentButton from './PaymentButton';
import { MOMO_DOWN_NOTE, MOMO_ENABLED } from "../../lib/pay";
import { useAuth } from "@/contexts/auth-context";
import { formatYmd } from "@/lib/utils";

const features = [
  {
    icon: "/icons/no-ads.png",
    title: "Xem WeTube không quảng cáo",
    desc: "không thẻ quảng cáo, không quảng cáo chen trước hoặc giữa video",
  },
  {
    icon: "/icons/download.png",
    title: "Tải video về máy",
    desc: "lưu tệp MP4 về thiết bị bằng nút Tải xuống dưới video",
  },
  {
    icon: "/icons/background-play.png",
    title: "Phát nền trên điện thoại",
    desc: "tắt màn hình hoặc chuyển sang ứng dụng khác vẫn nghe tiếp",
  },
  {
    icon: null,
    title: "Huy hiệu Premium",
    desc: "nhãn Premium cạnh logo, vương miện cạnh tên ở bình luận và chat trực tiếp",
  },
];

export default function PremiumTryitforfree() {
  const { user } = useAuth();

  return (
    <section className="min-h-screen bg-gradient-to-b from-blue-50 to-white flex flex-col items-center text-center px-4 pt-10 pb-20">
      <div className="max-w-3xl w-full flex flex-col items-center">
       
        <div className="flex items-center justify-center gap-x-4 mb-6">
          <Image
            src="/image/Logo.png"
            alt="WeTube Premium"
            width={80}
            height={80}
            className="object-contain"
          />
          <span className="text-2xl font-bold text-gray-800">
            WeTube Premium
          </span>
        </div>

        <p className="text-4xl md:text-5xl font-bold mb-4 leading-tight">
          Trải nghiệm trọn vẹn{" "}
          <span className="text-blue-600">WeTube Premium</span>
        </p>

        <p className="text-lg text-gray-700 mb-6">
          Xem WeTube không quảng cáo, tải video về máy
          và phát nền trên điện thoại.
        </p>

        {user?.premiumUntil && (
          <p className="mb-4 rounded-full bg-green-100 px-4 py-2 text-base font-medium text-green-800">
            Bạn đang dùng Premium đến {formatYmd(user.premiumUntil)}. Gia hạn sẽ được cộng dồn vào số ngày còn lại.
          </p>
        )}

        <p className="text-base text-gray-600 mb-2">
          Gói Basic chỉ <strong>₫99.000 / 30 ngày</strong> • Thanh toán bằng {MOMO_ENABLED ? "MoMo hoặc PayPal" : "PayPal"}
        </p>
        {!MOMO_ENABLED && (
          <p className="mb-2 rounded-full bg-amber-50 px-4 py-2 text-sm text-amber-800">{MOMO_DOWN_NOTE}</p>
        )}

        <div className="mt-6">
          <PaymentButton/>
        </div>
      </div>

      <div className="mt-16 grid grid-cols-1 md:grid-cols-2 gap-8 max-w-4xl w-full">
        {features.map((feature, index) => (
          <div
            key={index}
            className="flex items-start gap-4 text-left px-4"
          >
            {feature.icon ? (
              <Image
                src={feature.icon}
                alt={feature.title}
                width={48}
                height={48}
                className="object-contain"
              />
            ) : (
              <span className="flex size-12 shrink-0 items-center justify-center rounded-full bg-gradient-to-r from-amber-400 to-orange-500">
                <Crown aria-hidden className="size-6 fill-white text-white" />
              </span>
            )}
            <div>
              <h3 className="text-xl font-semibold text-gray-800">
                {feature.title}
              </h3>
              <p className="text-gray-600">{feature.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
