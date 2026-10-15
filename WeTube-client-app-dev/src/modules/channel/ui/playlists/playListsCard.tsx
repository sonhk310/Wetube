"use client";

import { useState } from "react";
import { Play } from "lucide-react";
import Link from "next/link";
import { PlaylistItem } from "./mockPL";
import Image from 'next/image';

export default function PlaylistCard({ playlist }: { playlist: PlaylistItem }) {
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  return (
    <Link
      href={`/playlist/${playlist.id}`}
      className="block w-[280px] group text-white"
    >
      <div className="relative h-[160px]">
        {/* Các thumbnail stack */}
        {playlist.stackThumbnails.map((thumb, index) => {
          const isHovered = hoverIndex === index;
          return (
            <Image
              key={index}
              src={thumb}
              alt={`Thumb ${index}`}
              fill
              sizes="280px"
              onMouseEnter={() => setHoverIndex(index)}
              onMouseLeave={() => setHoverIndex(null)}
              className={`
                absolute w-full h-full object-cover rounded-lg
                transition-all duration-300
                ${index === 0 ? "z-30" : index === 1 ? "z-20" : "z-10"}
                ${index > 0 ? "scale-[0.96]" : ""}
                ${isHovered ? "-translate-y-2 shadow-lg" : ""}
                top-0 left-0
              `}
              style={{
                transform: `translateY(${
                  isHovered ? "-12px" : `${index * 8}px`
                }) scale(${1 - index * 0.02})`,
              }}
            />
          );
        })}

        {/* Số lượng video overlay */}
        <div className="absolute bottom-2 right-2 bg-black bg-opacity-70 text-xs px-2 py-1 rounded-md flex items-center gap-1 z-40">
          <Play size={14} />
          {playlist.videoCount} video
        </div>
      </div>

      {/* Tiêu đề và phụ đề */}
      <div className="mt-2">
        <h3 className="font-semibold">{playlist.title}</h3>
        <p className="text-sm text-gray-400">Xem toàn bộ danh sách</p>
      </div>
    </Link>
  );
}
