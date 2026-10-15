"use client";
import Image from 'next/image';
import React, { useEffect, useState } from "react";

const HighlightVideos = [
    {
        id: 1,
        thumbnail: "https://th.bing.com/th/id/OIF.4ZIiU2o613QL6f9shpotqA?w=302&h=181&c=7&r=0&o=5&dpr=1.3&pid=1.7",
        title: "R$100.000 Super High Roller DIA 1 | BSOP Winter Millions",
        channel: "PokerStars Brasil",
        viewers: "3 N người đang xem",
        tag: "TRỰC TIẾP",
    },
    {
        id: 2,
        thumbnail: "https://i.ytimg.com/vi/kWwoj8ajwEQ/hqdefault.jpg?v=68778951&sqp=-oaymwEnCNACELwBSFryq4qpAxkIARUAAIhCGAHYAQHiAQoIGBACGAY4AUAB&rs=AOn4CLDHfx6r88ABNKiHCDPRCT7yINzmDQ",
        title: "[Live] TakrawKingsCup2025 : วันที่ 4 สนาม 1 | 25 ก.ค. 68",
        channel: "Thai PBS",
        viewers: "2,3 N người đang xem",
        tag: "TRỰC TIẾP",
    },
    {
        id: 3,
        thumbnail: "/images/video3.jpg",
        title: "Unc, Ocho & Iso Joe react to Justin Fields injury...",
        channel: "Nightcap",
        viewers: "9,6 N người đang xem",
        tag: "TRỰC TIẾP",
    },
    {
        id: 4,
        thumbnail: "/images/video4.jpg",
        title: "Vlog du lịch Đà Lạt - Trực tiếp cùng bạn bè chill chill",
        channel: "Tú Travel",
        viewers: "1,2 N người đang xem",
        tag: "TRỰC TIẾP",
    },
    {
        id: 5,
        thumbnail: "/images/video5.jpg",
        title: "Live: Giải đấu Liên Quân Mobile vòng chung kết!",
        channel: "V Gaming",
        viewers: "7,8 N người đang xem",
        tag: "TRỰC TIẾP",
    },
    {
        id: 6,
        thumbnail: "/images/video6.jpg",
        title: "Live Coding with Next.js & Tailwind",
        channel: "Code Zone",
        viewers: "900 người đang xem",
        tag: "TRỰC TIẾP",
    },
];

const HighlightList = () => {
  const [showAll, setShowAll] = useState(false);
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 640); // sm: breakpoint Tailwind
    };

    handleResize(); // Gọi khi mount
    window.addEventListener("resize", handleResize);

    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const getVideoSlice = () => {
    if (showAll) {
      return isMobile ? HighlightVideos.slice(0, 6) : HighlightVideos.slice(0, 6);
    } else {
      return isMobile ? HighlightVideos.slice(0, 2) : HighlightVideos.slice(0, 3);
    }
  };

  const displayedVideos = getVideoSlice();

  return (
    <div className="bg-white">
      <div className="flex items-center justify-between mb-4 ml-4">
        <h2 className="text-xl font-bold">High Light</h2>
        <button className="px-5 py-2 rounded-md text-black hover:bg-blue-300 hover:font-bold transition-colors mr-4">
          Xem tất cả
        </button>
      </div>

      <div className="flex flex-wrap gap-4 sm:gap-6 p-4">
        {displayedVideos.map((HighlightVideos) => (
          <div
            key={HighlightVideos.id}
            className="w-full sm:w-[calc(33.3333%-1rem)] flex flex-col"
          >
            <div className="aspect-video bg-blue-200 rounded-xl overflow-hidden relative">
              {/* <img
                src={HighlightVideos.thumbnail}
                alt={HighlightVideos.title}
                className="w-full h-full object-cover"
              /> */}
              <Image
                src={HighlightVideos.thumbnail}
                alt={HighlightVideos.title}
                fill
                sizes="(max-width: 640px) 100vw, (max-width: 768px) 50vw, 33vw"
                className="object-cover"
              />
            </div>
            <div className="flex gap-3 pt-3 pr-3 pb-3 pl-0 items-start">
              <div>
                <h3 className="text-lg text-black font-bold leading-tight break-words">
                  {HighlightVideos.channel}
                </h3>
                <p className="text-sm text-gray-400">
                  {HighlightVideos.viewers}
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="flex justify-center mt-8">
        <button
          onClick={() => setShowAll(!showAll)}
          className="px-5 py-2 rounded-md bg-neutral-700 text-white hover:bg-neutral-600 transition-colors"
        >
          {showAll ? "Ẩn bớt" : "Hiện thêm"}
        </button>
      </div>
    </div>
  );
};

export default HighlightList;
