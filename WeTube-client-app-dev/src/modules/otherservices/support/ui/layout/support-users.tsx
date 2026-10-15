"use client";

import React, { useState } from "react";
import { MessageCircleQuestion } from "lucide-react";

const SupportPanel: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      {/* Icon Trợ Giúp */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed bottom-4 right-4 z-50 bg-white text-black rounded-full p-2 shadow-lg transition-all duration-300 
                   hover:bg-white hover:text-black opacity-70 scale-90 hover:opacity-100 hover:scale-100"
      >
        <MessageCircleQuestion size={24} />
      </button>

      {/* Panel Trợ Giúp - nằm bên phải icon và căn trái sát icon */}
      {isOpen && (
        <div
          className="fixed bottom-16 right-4 w-80 max-h-[80vh] bg-white shadow-xl rounded-xl overflow-y-auto z-40 text-black p-4"
        >
          <div className="flex justify-between items-center mb-3">
            <h2 className="text-lg font-semibold">Trợ giúp</h2>
            <button
              onClick={() => setIsOpen(false)}
              className="text-gray-500 hover:text-black text-xl"
            >
              &times;
            </button>
          </div>
          <ul className="space-y-2 text-sm text-left">
            <li> Thay đổi địa chỉ email YouTube</li>
            <li> Nguyên tắc cộng đồng của YouTube</li>
            <li> Chọn chế độ cài đặt bình luận</li>
            <li> Thay đổi cỡ chữ</li>
            <li> Mẹo phát trực tiếp</li>
          </ul>
          <input
            type="text"
            placeholder="Tìm kiếm trong trợ giúp"
            className="mt-4 w-full border rounded-md px-3 py-2 text-sm"
          />
        </div>
      )}
    </>
  );
};

export default SupportPanel;
