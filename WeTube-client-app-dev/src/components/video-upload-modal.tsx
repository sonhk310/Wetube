"use client";

import React, { useState, useCallback, useEffect, useRef } from "react";
import { useDropzone } from "react-dropzone";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DialogHeader, DialogTitle, DialogClose } from "@/components/ui/dialog";
import Lottie from "lottie-react";
import uploadAnimation from "@/../public/icons/upload.json";
import { UploadWizard } from "@/components/upload/upload-wizard";
import { MAX_UPLOAD_BYTES } from "@/lib/uploadDraft";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn } from "@/lib/utils";

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

/** Thời gian chờ tối đa cho animation trước khi sang 4 bước (phòng khi Lottie không báo xong). */
const ANIMATION_FALLBACK_MS = 1200;

export const VideoUploadModal: React.FC<UploadModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  // Tệp đã chọn: icon chạy animation một lượt, xong mới sang 4 bước
  const [pickedFile, setPickedFile] = useState<File | null>(null);
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const fallbackTimer = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (!isOpen) {
      setTimeout(() => {
        setIsDragOver(false);
        setPickedFile(null);
        setVideoFile(null);
      }, 300);
    }
  }, [isOpen]);

  useEffect(() => () => window.clearTimeout(fallbackTimer.current), []);

  const showWizard = useCallback((file: File) => {
    window.clearTimeout(fallbackTimer.current);
    setVideoFile((current) => current ?? file);
  }, []);

  const onFileSelect = useCallback(
    (files: FileList | File[]) => {
      if (!files || files.length === 0) return;
      const file = files[0];
      if (!file.type.startsWith("video/")) {
        toastEmitter.error("Vui lòng chọn tệp video.");
        return;
      }
      if (file.size > MAX_UPLOAD_BYTES) {
        toastEmitter.error("Tệp quá lớn, tối đa 100MB.");
        return;
      }
      setIsDragOver(false);
      setPickedFile(file);
      fallbackTimer.current = window.setTimeout(() => showWizard(file), ANIMATION_FALLBACK_MS);
    },
    [showWizard]
  );

  const onDrop = useCallback((acceptedFiles: File[]) => {
    onFileSelect(acceptedFiles);
  }, [onFileSelect]);

  const {
    getRootProps,
    getInputProps,
    open: openFileDialog,
  } = useDropzone({
    onDrop,
    onDragEnter: () => setIsDragOver(true),
    onDragLeave: () => setIsDragOver(false),
    accept: { "video/*": [] },
    maxFiles: 1,
    noClick: true,
  });

  const handleChooseFiles = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    openFileDialog();
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      onFileSelect(e.target.files);
    }
  };

  if (videoFile) {
    return <UploadWizard file={videoFile} onClose={onClose} />;
  }

  return (
    <div className="flex flex-col h-full">
      <DialogHeader className="p-4 border-b border-gray-200">
        <DialogTitle className="text-black text-2xl font-medium">
          Tải video lên
        </DialogTitle>
        <DialogClose asChild>
          <Button
            variant="ghost"
            className="absolute right-4 top-4 rounded-sm opacity-70 ring-offset-background transition-opacity hover:opacity-200 disabled:pointer-events-none data-[state=open]:bg-accent data-[state=open]:text-muted-foreground"
          >
            <X className="h-7 w-7" />
            <span className="sr-only">Đóng</span>
          </Button>
        </DialogClose>
      </DialogHeader>
      <div className="p-8 flex-grow flex flex-col justify-center">
        <div
          {...getRootProps()}
          className={`rounded-lg p-16 text-center transition-colors ${
            isDragOver ? "bg-gray-50" : "bg-slate-50"
          }`}
        >
          <input
            {...getInputProps()}
            id="hidden-file-input"
            className="hidden"
            onChange={handleFileInputChange}
          />
          <button
            onClick={openFileDialog}
            disabled={!!pickedFile}
            className={cn(
              "w-30 h-30 mx-auto mb-6 bg-gray-200 rounded-full flex items-center justify-center cursor-pointer border-none p-0 appearance-none transition-transform",
              pickedFile && "scale-110 bg-blue-100"
            )}
          >
            {pickedFile ? (
              // Đã có tệp: chạy animation một lượt rồi sang 4 bước
              <Lottie
                key="picked"
                animationData={uploadAnimation}
                loop={false}
                autoplay
                onComplete={() => showWizard(pickedFile)}
                className="w-28 h-28"
              />
            ) : isDragOver ? (
              <Lottie
                animationData={uploadAnimation}
                loop={true}
                className="w-28 h-28"
              />
            ) : (
              <Lottie
                animationData={uploadAnimation}
                loop={false}
                size={90}
                className="text-black"
              />
            )}
          </button>
          <h3 className="text-gray-900 text-xl mb-2 font-medium">
            {pickedFile
              ? `Đang chuẩn bị "${pickedFile.name}"…`
              : isDragOver
                ? "Thả tệp video vào đây"
                : "Kéo và thả tệp video để tải lên"}
          </h3>
          <p className="text-gray-600 mb-8 text-sm">
            Video sẽ được lưu dưới dạng bản nháp cho đến khi bạn đăng.
          </p>
          <Button
            onClick={handleChooseFiles}
            disabled={!!pickedFile}
            className="bg-gray-200 hover:bg-gray-300 text-black px-6 py-2 rounded-sm"
            type="button"
          >
            Chọn tệp
          </Button>
        </div>
        <div className="mt-8 text-center text-md text-zinc-400 leading-relaxed">
          <p>
            {
              "Khi tải video lên WeTube, bạn xác nhận đồng ý với "
            }
            <a
              href="/terms"
              className="text-blue-400 hover:text-blue-300"
            >
              Điều khoản dịch vụ
            </a>
            {" và "}
            <a
              href="/community-guidelines"
              className="text-blue-400 hover:text-blue-300"
            >
              Nguyên tắc cộng đồng
            </a>
            {"."}
          </p>
        </div>
      </div>
    </div>
  );
};
