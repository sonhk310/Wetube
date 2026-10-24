"use client";

import React, { useRef } from "react";
import { Input } from "@/components/ui/input";
import { Pencil, Loader2 } from "lucide-react";
import Image from "next/image";

interface ThumbnailSelectorProps {
  thumbnailPreview: string;
  onThumbnailChange: (file: File) => void;
  isGenerating?: boolean;
  disabled?: boolean;
  /** Shorts: khung dọc 9:16 thay cho 16:9. */
  vertical?: boolean;
}

export const ThumbnailSelector: React.FC<ThumbnailSelectorProps> = ({
  thumbnailPreview,
  onThumbnailChange,
  isGenerating = false,
  disabled = false,
  vertical = false,
}) => {
  const aspect = vertical ? "aspect-[9/16]" : "aspect-video";
  const inputRef = useRef<HTMLInputElement>(null);

  const handleButtonClick = () => {
    if (!isGenerating && !disabled) {
      inputRef.current?.click();
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0];
      onThumbnailChange(file);
    }
  };

  const isDisabled = isGenerating || disabled;

  return (
    <div className="flex flex-col space-y-2">
      <label
        htmlFor="thumbnail"
        className="font-semibold mb-2 block text-md"
      >
        Ảnh thu nhỏ
        {thumbnailPreview && !isGenerating && !disabled && (
          <span className="text-sm text-gray-500 block font-normal">
            (Auto-generated - Click to change)
          </span>
        )}
        {isGenerating && (
          <span className="text-xs text-gray-500 block font-normal">
            (Generating...)
          </span>
        )}
        {disabled && !isGenerating && (
          <span className="text-xs text-gray-500 block font-normal">
            (Saving...)
          </span>
        )}
      </label>

      <div className={vertical ? "w-36" : "flex-1"}>
        <button
          type="button"
          onClick={handleButtonClick}
          disabled={isGenerating}
          className={`relative w-full p-0 border-none bg-transparent ${
            isDisabled ? "cursor-wait" : "cursor-pointer"
          } group`}
        >
          {thumbnailPreview ? (
            <Image
              src={thumbnailPreview}
              alt="Ảnh thu nhỏ"
              width={1280}
              height={720}
              className={`w-full h-auto ${aspect} object-cover rounded-lg border transition-all pointer-events-none`}
            />
          ) : (
            <div className={`w-full ${aspect} bg-gray-200 rounded-lg border flex items-center justify-center`}>
              {isGenerating ? (
                <div className="flex items-center gap-2 text-gray-500">
                  <Loader2 className="w-5 h-5 animate-spin" />
                  <span>Đang tạo...</span>
                </div>
              ) : (
                <span className="text-gray-500">Chưa có ảnh thu nhỏ</span>
              )}
            </div>
          )}

          {!isDisabled && thumbnailPreview && (
            <div className="absolute inset-0 bg-white/20 backdrop-blur-sm group-hover:backdrop-blur-md flex items-center justify-center rounded-lg opacity-0 group-hover:opacity-100 transition-all duration-300 pointer-events-none">
              <Pencil className="w-6 h-6 text-white drop-shadow-lg" />
            </div>
          )}

          {isGenerating && (
            <div className="absolute inset-0 bg-black bg-opacity-20 flex items-center justify-center rounded-lg">
              <Loader2 className="w-8 h-8 text-white animate-spin" />
            </div>
          )}
        </button>

        <Input
          ref={inputRef}
          id="thumbnail"
          type="file"
          accept="image/png, image/jpeg"
          onChange={handleFileChange}
          className="hidden"
          disabled={isDisabled}
        />
      </div>
    </div>
  );
};
