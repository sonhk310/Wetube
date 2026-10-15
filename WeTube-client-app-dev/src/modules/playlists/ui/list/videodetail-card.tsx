"use client";

import { useEffect, useRef, useState } from "react";
import ColorThief from "colorthief";
import Image from 'next/image';

interface VideoCardProps {
  thumbnailUrl: string;
  title: string;
}

export default function VideoCard({ thumbnailUrl, title }: VideoCardProps) {
  const imgRef = useRef<HTMLImageElement>(null);
  const [bgColor, setBgColor] = useState<string>("#ffffff");

  useEffect(() => {
    if (imgRef.current) {
      const colorThief = new ColorThief();
     
      imgRef.current.crossOrigin = "anonymous";

      imgRef.current.onload = () => {
        try {
          const color = colorThief.getColor(imgRef.current!);
          setBgColor(`rgb(${color[0]}, ${color[1]}, ${color[2]})`);
        } catch (error) {
          console.error("ColorThief error:", error);
        }
      };
    }
  }, [thumbnailUrl]);

  return (
    <div
      className="rounded-2xl shadow-md p-4 transition-all duration-300"
      style={{ backgroundColor: bgColor }}
    >
      <Image
        ref={imgRef}
        src={thumbnailUrl}
        alt={title}
        width={300}
        height={200}
        className="w-full h-40 object-cover rounded-xl"
      />
      <h3 className="mt-3 text-white font-semibold">{title}</h3>
    </div>
  );
}
