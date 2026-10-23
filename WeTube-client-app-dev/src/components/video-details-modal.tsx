"use client";

import React, { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose,
  DialogContent,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import toast from "react-hot-toast";
import { Clock, Film, Loader2, X } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { ThumbnailSelector } from "./ThumbnailSelector";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useUploadVideoMutation, videoApi } from "@/app/api/videoApi";
import { useDispatch } from "react-redux";
import { playlistApi } from "@/app/api/playlistApi";
import { formatDuration } from "@/lib/utils";
import axios from "axios";
import { API_PREFIX } from "@/constants/appConstant";

const MAX_VIDEO_SIZE_MB = 100;
const MAX_VIDEO_SIZE_BYTES = MAX_VIDEO_SIZE_MB * 1024 * 1024;
const MAX_THUMBNAIL_SIZE_MB = 5;
const MAX_THUMBNAIL_SIZE_BYTES = MAX_THUMBNAIL_SIZE_MB * 1024 * 1024;

const videoUploadSchema = z.object({
  title: z
    .string()
    .trim()
    .min(1, { message: "Vui lòng nhập tiêu đề." })
    .max(100, { message: "Tiêu đề tối đa 100 ký tự." }),
  description: z
    .string()
    .max(5000, { message: "Mô tả tối đa 5000 ký tự." })
    .optional(),
  tags: z
    .string()
    .max(200, { message: "Thẻ tối đa 200 ký tự." })
    .refine((value) => value === "" || /^(#\w+(\s+#\w+)*)$/.test(value), {
      message: 'Thẻ phải có dạng "#the1 #the2"',
    })
    .optional(),
  videoFile: z
    .instanceof(File, { message: "Vui lòng chọn tệp video." })
    .refine(
      (file) => file.size <= MAX_VIDEO_SIZE_BYTES,
      `Tệp video tối đa ${MAX_VIDEO_SIZE_MB}MB.`
    ),
  thumbnailFile: z
    .instanceof(File, { message: "Vui lòng chọn hoặc tạo ảnh thu nhỏ." })
    .refine(
      (file) => file.size <= MAX_THUMBNAIL_SIZE_BYTES,
      `Ảnh thu nhỏ tối đa ${MAX_THUMBNAIL_SIZE_MB}MB.`
    ),
});

type VideoFormData = z.infer<typeof videoUploadSchema>;

interface VideoDetailsModalProps {
  file: File;
  onClose: () => void;
  onUploadComplete: () => void;
}

export const VideoDetailsModal: React.FC<VideoDetailsModalProps> = ({
  file,
  onClose,
  onUploadComplete,
}) => {
  const { user } = useAuth();
  const dispatch = useDispatch();

  const [uploadVideo, { isLoading: isUploading }] = useUploadVideoMutation();

  const [uploadProgress, setUploadProgress] = useState(0);
  const [videoSrc, setVideoSrc] = useState("");
  const [duration, setDuration] = useState(0);
  const [resolution, setResolution] = useState("");

  const [thumbnailFile, setThumbnailFile] = useState<File | null>(null);
  const [thumbnailPreview, setThumbnailPreview] = useState<string>("");
  const [isGeneratingThumbnail, setIsGeneratingThumbnail] = useState(false);

  const [isGeneratingDescription, setIsGeneratingDescription] = useState(false);

  // Modal error state
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Dropdown chọn ngôn ngữ
  const [targetLang, setTargetLang] = useState(""); // mặc định English
  const [visibility, setVisibility] = useState<"PUBLIC" | "UNLISTED" | "PRIVATE">("PUBLIC");

  const titleWithoutExt = file.name.replace(/\.[^/.]+$/, "");
  const defaultTitle = decodeURIComponent(titleWithoutExt);

  const {
    register,
    handleSubmit,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<VideoFormData>({
    resolver: zodResolver(videoUploadSchema),
    mode: "onBlur",
    defaultValues: {
      title: defaultTitle,
      description: "",
      tags: "",
      videoFile: file,
      thumbnailFile: undefined,
    },
  });

  // Generate mô tả bằng AI
  const handleGenerateDescription = async () => {
    const values = getValues();
    const title = values.title?.trim();
    if (!title) {
      toast.error("Vui lòng nhập tiêu đề trước.");
      return;
    }
    try {
      setIsGeneratingDescription(true);
      const res = await axios.post(
        `${API_PREFIX}/ai/description`,
        { title },
        { headers: { "Content-Type": "application/json" } }
      );
      if (res.data) {
        if (res.data.description)
          setValue("description", res.data.description, {
            shouldValidate: true,
          });
        if (res.data.tags)
          setValue("tags", res.data.tags, { shouldValidate: true });
        toast.success("AI đã tạo mô tả và thẻ!");
      }
    } catch (err) {
      toast.error("Không tạo được mô tả và thẻ");
    } finally {
      setIsGeneratingDescription(false);
    }
  };

  // Generate thumbnail tự động
  const generateThumbnail = useCallback(
    (video: HTMLVideoElement): Promise<File> => {
      return new Promise((resolve, reject) => {
        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          reject(new Error("Không khởi tạo được vùng vẽ"));
          return;
        }
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(
                new File([blob], "thumbnail.jpg", { type: "image/jpeg" })
              );
            } else reject(new Error("Không tạo được ảnh thu nhỏ"));
          },
          "image/jpeg",
          0.8
        );
      });
    },
    []
  );

  useEffect(() => {
    const url = URL.createObjectURL(file);
    setVideoSrc(url);
    setIsGeneratingThumbnail(true);

    const videoElement = document.createElement("video");
    videoElement.src = url;
    videoElement.crossOrigin = "anonymous";
    videoElement.muted = true;

    videoElement.addEventListener("loadeddata", () => {
      if (isFinite(videoElement.duration) && videoElement.duration > 0) {
        setDuration(videoElement.duration);
        setResolution(
          `${videoElement.videoWidth} x ${videoElement.videoHeight}`
        );
        videoElement.currentTime = Math.min(videoElement.duration / 2, 1);
      }
    });

    videoElement.addEventListener("seeked", async () => {
      try {
        if (!thumbnailFile) {
          const autoThumb = await generateThumbnail(videoElement);
          setThumbnailFile(autoThumb);
          const previewUrl = URL.createObjectURL(autoThumb);
          setThumbnailPreview(previewUrl);
          setValue("thumbnailFile", autoThumb, { shouldValidate: true });
        }
      } finally {
        setIsGeneratingThumbnail(false);
      }
    });

    return () => {
      URL.revokeObjectURL(url);
    };
  }, [file, generateThumbnail, setValue, thumbnailFile]);

  useEffect(() => {
    return () => {
      if (thumbnailPreview) URL.revokeObjectURL(thumbnailPreview);
    };
  }, [thumbnailPreview]);

  const handleThumbnailChange = (selectedFile: File) => {
    if (thumbnailPreview) URL.revokeObjectURL(thumbnailPreview);
    setThumbnailFile(selectedFile);
    const previewUrl = URL.createObjectURL(selectedFile);
    setThumbnailPreview(previewUrl);
    setValue("thumbnailFile", selectedFile, { shouldValidate: true });
  };

  // Submit upload
  const onSubmit = async (data: VideoFormData) => {
    if (!user) {
      toast.error("Bạn cần đăng nhập để tiếp tục.");
      return;
    }

    const formData = new FormData();
    formData.append("videoFile", data.videoFile);
    formData.append("thumbnailFile", data.thumbnailFile);
    formData.append("title", data.title);
    formData.append("description", data.description || "");
    formData.append("tags", data.tags || "");
    formData.append("usersId", user.sub);
    formData.append("duration", duration.toString());
    formData.append("isShort", String(duration <= 60));
    formData.append("visibility", visibility);
    if (targetLang && targetLang !== "") {
      formData.append("targetLang", targetLang);
    }

    setUploadProgress(0);

    const interval = setInterval(() => {
      setUploadProgress((prev) => (prev >= 90 ? prev : prev + 5));
    }, 200);

    try {
      for (const [key, value] of formData.entries()) {
        console.log(key, value);
      }

      await uploadVideo(formData).unwrap();
      clearInterval(interval);
      setUploadProgress(100);

      toast.success("Tải video lên thành công!", { duration: 3000 });

      // if (user) {
      //   dispatch(
      //     playlistApi.util.invalidateTags([
      //       { type: "Playlist", id: `USER_${user.sub}` },
      //     ])
      //   );
      // }
      dispatch(playlistApi.util.invalidateTags(["Playlist"]));
      dispatch(videoApi.util.invalidateTags(["VideoList"]));
      onUploadComplete();
    } catch (err: any) {
      clearInterval(interval);
      setUploadProgress(0);
      const msg =
        err?.data?.message ||
        err?.message ||
        "Upload failed. Please try again.";
      setErrorMessage(msg);
    }
  };

  const renderButtonContent = () =>
    isUploading ? (
      <>
        <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Uploading...
      </>
    ) : isGeneratingThumbnail ? (
      <>
        <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Generating
        thumbnail...
      </>
    ) : (
      "Upload"
    );

  return (
    <div className="flex flex-col h-full max-h-[90vh]">
      <DialogHeader className="p-4 border-b flex-shrink-0">
        <DialogTitle className="text-2xl font-medium">
          Chi tiết video
        </DialogTitle>
        <DialogClose asChild>
          <Button
            variant="ghost"
            className="absolute right-4 top-4 rounded-sm opacity-70"
            disabled={isUploading}
          >
            <X className="h-7 w-7" />
            <span className="sr-only">Đóng</span>
          </Button>
        </DialogClose>
      </DialogHeader>

      {/* Body */}
      <div className="flex-1 p-6 overflow-hidden">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 h-full">
          <div className="lg:col-span-2 space-y-6 overflow-y-auto pr-2">
            {/* Title */}
            <div>
              <label
                htmlFor="title"
                className="font-semibold mb-2 block"
              >
                Tiêu đề
              </label>
              <Textarea
                id="title"
                {...register("title")}
                disabled={isUploading}
              />
              {errors.title && (
                <p className="text-sm text-red-500">{errors.title.message}</p>
              )}
            </div>

            {/* Description */}
            <div>
              <label
                htmlFor="description"
                className="font-semibold mb-2 block"
              >
                Mô tả
              </label>
              <Textarea
                id="description"
                rows={6}
                {...register("description")}
                disabled={isUploading}
              />
              {errors.description && (
                <p className="text-sm text-red-500">
                  {errors.description.message}
                </p>
              )}
              <Button
                type="button"
                onClick={handleGenerateDescription}
                disabled={isGeneratingDescription}
                className="mt-2 bg-purple-600 text-white"
              >
                {isGeneratingDescription ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />{" "}
                    Đang tạo...
                  </>
                ) : (
                  "AI Generate Description"
                )}
              </Button>
            </div>

            {/* Chế độ hiển thị */}
            <div>
              <label htmlFor="visibility" className="font-semibold mb-2 block">
                Chế độ hiển thị
              </label>
              <select
                id="visibility"
                value={visibility}
                onChange={(e) =>
                  setVisibility(e.target.value as "PUBLIC" | "UNLISTED" | "PRIVATE")
                }
                className="border rounded px-3 py-2 w-full"
                disabled={isUploading}
              >
                <option value="PUBLIC">Công khai — ai cũng có thể tìm và xem</option>
                <option value="UNLISTED">Không công khai — chỉ người có liên kết xem được</option>
                <option value="PRIVATE">Riêng tư — chỉ mình bạn xem được</option>
              </select>
            </div>

            {/* Dropdown chọn ngôn ngữ */}
            <div className="mt-4">
              <label className="font-semibold mb-2 block">Phụ đề AI (dịch sang)</label>
              <select
                value={targetLang}
                onChange={(e) => setTargetLang(e.target.value)}
                className="border rounded px-3 py-2 w-full"
                disabled={isUploading}
              >
                <option value="">Không tạo phụ đề</option>
                <option value="en">English</option>
                <option value="vi">Tiếng Việt</option>
                <option value="ko">한국어</option>
                <option value="ja">日本語</option>
                <option value="zh">中文</option>
              </select>
            </div>

            {/* Thumbnail + Tags */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <ThumbnailSelector
                thumbnailPreview={thumbnailPreview}
                onThumbnailChange={handleThumbnailChange}
                isGenerating={isGeneratingThumbnail}
                disabled={isUploading}
              />
              <div className="flex-1 pt-1">
                {errors.thumbnailFile && (
                  <p className="text-sm text-red-500">
                    {errors.thumbnailFile.message}
                  </p>
                )}
              </div>
            </div>

            <div>
              <label
                htmlFor="tags"
                className="font-semibold mb-2 block"
              >
                Thẻ
              </label>
              <Input
                id="tags"
                placeholder="Nhập các thẻ, bắt đầu bằng #"
                disabled={isUploading}
                className={errors?.tags ? "border-red-500" : ""}
                {...register("tags")}
              />
              {errors.tags && (
                <p className="text-sm text-red-500 mt-1">
                  {errors.tags.message}
                </p>
              )}
              <p className="text-xs text-muted-foreground mt-1">
                Thẻ giúp người khác dễ tìm thấy video của bạn.
              </p>
            </div>
          </div>

          {/* Preview */}
          <div className="lg:col-span-1 flex flex-col">
            {/* Video Player */}
            {videoSrc && (
              <video
                src={videoSrc}
                controls
                className="w-full rounded-lg bg-black aspect-video"
              />
            )}

            {/* Khung chứa thông tin chi tiết dưới video */}
            <div className="mt-4 border rounded-md p-3 text-sm space-y-3 bg-gray-50">
              <p className="font-semibold line-clamp-2 break-words">
                {getValues("title")}
              </p>

              {duration > 0 && (
                <div className="flex items-center gap-6 text-xs text-gray-600">
                  <div className="flex items-center gap-1.5">
                    <Clock className="w-4 h-4" />
                    <span>{formatDuration(duration)}</span>
                  </div>
                  {resolution && (
                    <div className="flex items-center gap-1.5">
                      <Film className="w-4 h-4" />
                      <span>{resolution}</span>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Thanh tiến trình upload */}
            {isUploading && (
              <div className="mt-4">
                <p className="text-sm mb-1">Uploading: {uploadProgress}%</p>
                <div className="w-full bg-gray-200 h-2 rounded-full overflow-hidden">
                  <div
                    className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Footer */}
      <DialogFooter className="p-4 border-t flex-shrink-0">
        <Button
          variant="ghost"
          onClick={onClose}
          disabled={isUploading}
        >
          Quay lại
        </Button>
        <Button
          onClick={handleSubmit(onSubmit)}
          disabled={isUploading || isGeneratingThumbnail}
          className="bg-blue-600 text-white"
        >
          {renderButtonContent()}
        </Button>
      </DialogFooter>

      {/* Error Modal */}
      {errorMessage && (
        <Dialog
          open={!!errorMessage}
          onOpenChange={() => setErrorMessage(null)}
        >
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Lỗi Upload</DialogTitle>
            </DialogHeader>
            <p>{errorMessage}</p>
            <DialogFooter>
              <Button onClick={() => setErrorMessage(null)}>OK</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
};
