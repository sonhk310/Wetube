"use client";

import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Spinner } from "@/components/ui/spinner";

/** Hộp thoại xác nhận kết thúc buổi phát (chủ kênh, hoặc quản trị viên khi `admin`). */
export default function LiveEndDialog({
  open,
  onOpenChange,
  onConfirm,
  loading,
  admin = false,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  loading: boolean;
  admin?: boolean;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{admin ? "Kết thúc buổi phát này?" : "Kết thúc buổi phát?"}</DialogTitle>
          <DialogDescription>
            Mọi người đang xem sẽ bị ngắt khỏi buổi phát và chat sẽ bị xoá. Việc này không hoàn tác được.
          </DialogDescription>
        </DialogHeader>
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>
            Huỷ
          </Button>
          <Button variant="destructive" onClick={onConfirm} disabled={loading}>
            {loading ? <Spinner className="size-4" /> : "Kết thúc"}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
