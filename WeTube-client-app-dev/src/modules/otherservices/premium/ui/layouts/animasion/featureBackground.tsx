import { Headphones } from "lucide-react";
import React from "react";

/** Phát nền (Premium): giải thích cách hoạt động và giới hạn thật của trình duyệt. */
export default function FeatureBackground() {
  return (
    <section className="py-20 px-6 bg-white">
      <h2 className="text-3xl md:text-5xl font-extrabold text-center mb-16">
        Nghe tiếp khi tắt màn hình
      </h2>

      <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
        <div className="relative flex items-center justify-center md:order-first">
          <div className="relative flex size-72 items-center justify-center rounded-full p-[4px] bg-gradient-to-tr from-blue-500 via-purple-500 to-green-500">
            <div className="flex size-full items-center justify-center rounded-full bg-white">
              <Headphones aria-hidden className="size-24 text-blue-600" strokeWidth={1.5} />
            </div>
          </div>
        </div>

        <div>
          <h3 className="text-2xl font-bold mb-4">
            Phát nền trên điện thoại và máy tính bảng
          </h3>
          <p className="text-gray-600 leading-relaxed">
            Đang xem mà tắt màn hình hoặc chuyển sang ứng dụng khác, WeTube chuyển sang phát âm thanh để bạn nghe tiếp,
            rồi quay lại đúng chỗ khi bạn mở lại. <br />
            Bật hoặc tắt trong phần Cài đặt của trình phát (mục “Phát nền”). Kết quả có thể khác nhau tuỳ trình duyệt
            trên điện thoại. Trên máy tính, video vốn vẫn phát khi bạn chuyển thẻ.
          </p>
        </div>
      </div>
    </section>
  );
}
