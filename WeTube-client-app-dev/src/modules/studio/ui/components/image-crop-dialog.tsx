"use client";

import { useEffect, useState } from "react";
import Cropper from "react-easy-crop";
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
import { CropArea, cropImageToFile } from "@/lib/cropImage";
import { toastEmitter } from "@/lib/toastEmitter";

export type CropKind = "avatar" | "banner";

// Ảnh đại diện vuông 1:1 (khung tròn); ảnh bìa 6:1 là tỉ lệ hiển thị trên máy tính (điện thoại hiện phần giữa)
const ASPECT: Record<CropKind, number> = { avatar: 1, banner: 6 };
// Cắt xong thu về cỡ này (backend cũng chặn ảnh quá lớn), nên ảnh gốc to vẫn tải lên được
const MAX_OUTPUT_WIDTH: Record<CropKind, number> = { avatar: 512, banner: 2048 };

/** Hộp thoại chọn vùng ảnh (kéo, phóng to / thu nhỏ) trước khi tải ảnh đại diện / ảnh bìa lên. */
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
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [area, setArea] = useState<CropArea | null>(null);
  const [working, setWorking] = useState(false);

  // Mỗi lần chọn ảnh mới: tạo link xem tạm, bắt đầu lại từ giữa ảnh
  useEffect(() => {
    if (!file) {
      setUrl(null);
      return;
    }
    const objectUrl = URL.createObjectURL(file);
    setUrl(objectUrl);
    setCrop({ x: 0, y: 0 });
    setZoom(1);
    setArea(null);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  const confirm = async () => {
    if (!file || !url || !area) return;
    setWorking(true);
    try {
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
            Kéo ảnh để chọn vùng muốn giữ, dùng thanh trượt (hoặc hai ngón tay) để phóng to.
            {kind === "banner" && " Trên điện thoại ảnh bìa hẹp hơn nên chỉ hiện phần giữa của vùng này."}
          </DialogDescription>
        </DialogHeader>
        <div className="relative h-72 w-full overflow-hidden rounded-lg bg-black">
          {url && (
            <Cropper
              image={url}
              crop={crop}
              zoom={zoom}
              aspect={ASPECT[kind]}
              cropShape={kind === "avatar" ? "round" : "rect"}
              showGrid={false}
              onCropChange={setCrop}
              onZoomChange={setZoom}
              onCropComplete={(_, pixels) => setArea(pixels)}
            />
          )}
        </div>
        <label className="flex items-center gap-3 text-sm">
          <span className="shrink-0">Thu phóng</span>
          <input
            type="range"
            min={1}
            max={3}
            step={0.01}
            value={zoom}
            onChange={(e) => setZoom(Number(e.target.value))}
            aria-label="Thu phóng"
            className="w-full"
          />
        </label>
        <DialogFooter>
          <Button type="button" variant="ghost" className="rounded-full" disabled={working} onClick={onCancel}>
            Huỷ
          </Button>
          <Button type="button" className="rounded-full" disabled={!area || working} onClick={confirm}>
            {working ? "Đang cắt..." : "Cắt và tải lên"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
