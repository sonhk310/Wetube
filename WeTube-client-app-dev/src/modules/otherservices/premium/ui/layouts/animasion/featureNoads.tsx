import Image from "next/image";
import React from "react";

export default function FeatureNoAds() {
  return (
    <section className="py-20 px-6 bg-white">
      <h2 className="text-3xl md:text-5xl font-extrabold text-center mb-16">
        Xem video không bị quảng cáo làm phiền
      </h2>

      <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-12 items-center">
        {/* Text content */}
        <div>
          <h3 className="text-2xl font-bold mb-4">
            Không quảng cáo, trọn vẹn từng video
          </h3>
          <p className="text-gray-600 leading-relaxed">
            Với Premium, WeTube không hiện thẻ quảng cáo ở trang chủ, trang xem
            và không chen quảng cáo trước video. <br />
            Bạn chỉ xem nội dung của các nhà sáng tạo mình yêu thích.
          </p>
        </div>

        {/* Image with gradient ring and play icon */}
        <div className="relative flex items-center justify-center">
          <div className="relative w-72 h-72 rounded-full p-[4px] bg-gradient-to-tr from-blue-500 via-purple-500 to-green-500">
            <div className="w-full h-full rounded-full overflow-hidden bg-white">
              <Image
                src="/image/video-thumbnail.png"
                alt="Xem không quảng cáo"
                fill
                sizes="288px"
                className="object-cover"
              />
            </div>
          </div>

          {/* Play icon */}
          <div className="absolute bottom-4 right-4 bg-white p-3 rounded-full shadow-lg cursor-pointer hover:scale-105 transition">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="w-5 h-5 fill-black"
              viewBox="0 0 24 24"
            >
              <path d="M8 5v14l11-7z" />
            </svg>
          </div>
        </div>
      </div>
    </section>
  );
}
