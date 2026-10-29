"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAppDialogStore } from "@/store/zustand/useAppDialogStore";

/** Hiện hộp thoại xác nhận / thông báo do lib/dialogs (confirmDialog, noticeDialog) yêu cầu. Gắn một lần ở layout gốc. */
export default function AppDialogHost() {
  const current = useAppDialogStore((s) => s.queue[0]);
  const settle = useAppDialogStore((s) => s.settle);

  return (
    <Dialog
      open={!!current}
      // Đóng bằng Esc, bấm ra ngoài hoặc nút X: xác nhận coi là Huỷ, thông báo coi là đã đọc
      onOpenChange={(open) => {
        if (!open) settle(current?.kind === "notice");
      }}
    >
      {current && (
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{current.title}</DialogTitle>
            {current.message ? (
              <DialogDescription className="whitespace-pre-line">{current.message}</DialogDescription>
            ) : null}
          </DialogHeader>
          <DialogFooter>
            {current.kind === "confirm" && (
              <Button variant="outline" onClick={() => settle(false)}>
                {current.cancelLabel}
              </Button>
            )}
            <Button variant={current.destructive ? "destructive" : "default"} autoFocus onClick={() => settle(true)}>
              {current.confirmLabel}
            </Button>
          </DialogFooter>
        </DialogContent>
      )}
    </Dialog>
  );
}
