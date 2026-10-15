"use client";
import React from "react";

interface FeedbackModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const FeedbackModal: React.FC<FeedbackModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black bg-opacity-50 flex justify-end">
      <div className="w-full max-w-md h-full bg-white dark:bg-zinc-900 p-6 overflow-y-auto shadow-xl relative">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-3 right-4 text-2xl text-gray-400 hover:text-white"
        >
          &times;
        </button>

        <h2 className="text-xl font-semibold mb-4 text-white">Gửi phản hồi đến YouTube</h2>

        {/* Form content */}
        <div className="space-y-4 text-sm text-white">
          <div>
            <label className="block font-medium mb-1">
              Mô tả ý kiến phản hồi của bạn (bắt buộc)
            </label>
            <textarea
              className="w-full h-28 p-2 border border-gray-600 bg-transparent rounded resize-none"
              placeholder="Hãy cho chúng tôi biết điều gì khiến bạn đưa ra ý kiến phản hồi này..."
            />
            <p className="text-xs text-gray-400 mt-1">
              Vui lòng không gửi thông tin nhạy cảm
            </p>
          </div>

          <div>
            <label className="block font-medium mb-2">Ảnh chụp màn hình</label>
            <button className="w-full py-2 border border-gray-600 rounded flex items-center justify-center gap-2 hover:bg-gray-800 transition">
              <span>🖼️</span> Chụp ảnh màn hình
            </button>
          </div>

          <div className="flex items-start gap-2">
            <input type="checkbox" className="mt-1" />
            <label className="text-xs text-gray-300">
              Chúng tôi có thể gửi email cho bạn để hỏi thêm thông tin hoặc để cập nhật thông tin cho bạn
            </label>
          </div>

          <p className="text-xs text-gray-400">
            Một số{" "}
            <a href="#" className="underline text-blue-400">thông tin về tài khoản và hệ thống</a> có thể được gửi đến Google...
            <br />
            <a href="#" className="underline text-blue-400">Chính sách quyền riêng tư</a> và{" "}
            <a href="#" className="underline text-blue-400">Điều khoản dịch vụ</a>.
          </p>

          <button className="w-full mt-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded">
            Gửi
          </button>
        </div>
      </div>
    </div>
  );
};

export default FeedbackModal;
