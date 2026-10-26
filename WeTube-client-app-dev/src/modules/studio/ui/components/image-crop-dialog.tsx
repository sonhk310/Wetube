"use client";

import { SyntheticEvent, useEffect, useRef, useState } from "react";
import ReactCrop, { Crop, centerCrop, makeAspectCrop } from "react-image-crop";
import "react-image-crop/dist/ReactCrop.css";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { apiErrorMessage } from "@/lib/apiError";
import { cropImageToFile } from "@/lib/cropImage";
import { toastEmitter } from "@/lib/toastEmitter";

export type CropKind = "avatar" | "banner";

// Ảnh đại diện vuông 1:1 (khung tròn); ảnh bìa 6:1 là tỉ lệ hiển thị trên máy tính (điện thoại hiện phần giữa)
const ASPECT: Record<CropKind, number> = { avatar: 1, banner: 6 };
// Cắt xong thu về cỡ này (backend cũng chặn ảnh quá lớn), nên ảnh gốc to vẫn tải lên được
const MAX_OUTPUT_WIDTH: Record<CropKind, number> = { avatar: 512, banner: 2048 };

/**
 * Hộp thoại chọn vùng ảnh trước khi tải ảnh đại diện / ảnh bìa lên: ảnh đứng yên, chủ kênh kéo khung để dời,
 * kéo góc khung để đổi cỡ (tỉ lệ khung được giữ cố định).
 */
export default function ImageCropDialog({
  file,
  kind,
  onCancel,
  onConfirm,
}: {
  file: File | null;
  kind: CropKind;
  onCancel: () => void;
  onConfirm: (cropped: File) => void;
}) {
  const [url, setUrl] = useState<string | null>(null);
  // Khung tính theo % của ảnh để không lệch khi cửa sổ đổi cỡ
  const [crop, setCrop] = useState<Crop>();
  const [working, setWorking] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);
  const aspect = ASPECT[kind];

  // Mỗi lần chọn ảnh mới: tạo link xem tạm, bỏ khung cũ
  useEffect(() => {
    setCrop(undefined);
    if (!file) {
      setUrl(null);
      return;
    }
    const objectUrl = URL.createObjectURL(file);
    setUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  // Ảnh tải xong: đặt sẵn khung lớn nhất có thể (chiếm 90% cạnh vừa khít), nằm giữa ảnh
  const onImageLoad = (e: SyntheticEvent<HTMLImageElement>) => {
    const { width, height } = e.currentTarget;
    const fitByHeight = width / height > aspect;
    const initial = makeAspectCrop(
      fitByHeight ? { unit: "%", height: 90 } : { unit: "%", width: 90 },
      aspect,
      width,
      height
    );
    setCrop(centerCrop(initial, width, height));
  };

  const confirm = async () => {
    const img = imgRef.current;
    if (!file || !url || !img || !crop || crop.unit !== "%") return;
    setWorking(true);
    try {
      // Đổi % của khung sang điểm ảnh của ảnh gốc
      const area = {
        x: (crop.x / 100) * img.naturalWidth,
        y: (crop.y / 100) * img.naturalHeight,
        width: (crop.width / 100) * img.naturalWidth,
        height: (crop.height / 100) * img.naturalHeight,
      };
      onConfirm(await cropImageToFile(url, area, MAX_OUTPUT_WIDTH[kind], file.name));
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không cắt được ảnh, vui lòng thử lại."));
    } finally {
      setWorking(false);
    }
  };

  const title = kind === "avatar" ? "Cắt ảnh đại diện" : "Cắt ảnh bìa";

  return (
    <Dialog open={!!file} onOpenChange={(open) => !open && !working && onCancel()}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] sm:max-w-xl rounded-2xl">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>
            Kéo khung để chọn vùng muốn giữ, kéo các góc để đổi kích thước khung.
            {kind === "banner" && " Trên điện thoại ảnh bìa hẹp hơn nên chỉ hiện phần giữa của vùng này."}
          </DialogDescription>
        </DialogHeader>
        <div className="flex max-h-[60vh] justify-center overflow-auto rounded-lg bg-gray-100">
          {url && (
            <ReactCrop
              crop={crop}
              onChange={(_, percentCrop) => setCrop(percentCrop)}
              aspect={aspect}
              circularCrop={kind === "avatar"}
              keepSelection
              minWidth={32}
            >
              {/* eslint-disable-next-line @next/next/no-img-element -- ảnh xem tạm từ blob, không qua tối ưu của Next */}
              <img
                ref={imgRef}
                src={url}
                alt="Ảnh đang cắt"
                onLoad={onImageLoad}
                className="max-h-[60vh] max-w-full select-none"
              />
            </ReactCrop>
          )}
        </div>
        <DialogFooter>
          <Button type="button" variant="ghost" className="rounded-full" disabled={working} onClick={onCancel}>
            Huỷ
          </Button>
          <Button type="button" className="rounded-full" disabled={!crop || working} onClick={confirm}>
            {working ? "Đang cắt..." : "Cắt và tải lên"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
