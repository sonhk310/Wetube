"use client";

import React, { useState, useCallback, useEffect } from "react";
import { useDropzone } from "react-dropzone";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DialogHeader, DialogTitle, DialogClose } from "@/components/ui/dialog";
import Lottie from "lottie-react";
import uploadAnimation from "@/../public/icons/upload.json";
import { VideoDetailsModal } from "@/components/video-details-modal";

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const VideoUploadModal: React.FC<UploadModalProps> = ({
  isOpen,
  onClose,
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [videoFile, setVideoFile] = useState<File | null>(null);

  useEffect(() => {
    if (!isOpen) {
      setTimeout(() => {
        setIsDragOver(false);
        setVideoFile(null);
      }, 300);
    }
  }, [isOpen]);

  const onFileSelect = (files: FileList | File[]) => {
    if (files && files.length > 0) {
      const file = files[0];
      if (file.type.startsWith("video/")) {
        console.log("Selected file:", file);
        setVideoFile(file);
      } else {
        alert("Vui lòng chọn tệp video hợp lệ.");
      }
    }
  };

  const onDrop = useCallback((acceptedFiles: File[]) => {
    onFileSelect(acceptedFiles);
  }, []);

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
    return (
      <VideoDetailsModal
        file={videoFile}
        onClose={() => {
          setVideoFile(null);
        }}
        onUploadComplete={() => {
          onClose();
        }}
      />
    );
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
      {/* <div className="p-8"> */}
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
            className="w-30 h-30 mx-auto mb-6 bg-gray-200 rounded-full flex items-center justify-center
                            justify-center cursor-pointer border-none p-0 appearance-none"
          >
            {/* <Upload size={90} className="text-black"/>
                        <span className="sr-only">Chọn tệp video</span> */}
            {isDragOver ? (
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
            {isDragOver
              ? "Thả tệp video vào đây"
              : "Kéo và thả tệp video để tải lên"}
          </h3>
          <p className="text-gray-600 mb-8 text-sm">
            Video của bạn sẽ ở chế độ riêng tư cho đến khi bạn công khai.
          </p>
          <Button
            onClick={handleChooseFiles}
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
