"use client";

import { Check, Copy } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import { roomUrl } from "../lib/createRoom";

export default function InviteDialog({
  code,
  open,
  onOpenChange,
}: {
  code: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [, copy] = useCopyToClipboard();
  const [copied, setCopied] = useState(false);
  const link = typeof window === "undefined" ? "" : roomUrl(code);

  const handleCopy = async () => {
    if (await copy(link)) {
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } else {
      toastEmitter.error("Không sao chép được, hãy chép thủ công");
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Mời bạn bè</DialogTitle>
          <DialogDescription>
            Gửi liên kết này cho bạn bè. Họ cần đăng nhập, rồi bấm vào liên kết là vào phòng ngay (tối đa 20 người).
          </DialogDescription>
        </DialogHeader>
        <div className="flex items-center gap-2">
          <Input readOnly value={link} onFocus={(e) => e.currentTarget.select()} aria-label="Liên kết mời" />
          <Button type="button" onClick={handleCopy} className="shrink-0">
            {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
            {copied ? "Đã chép" : "Sao chép"}
          </Button>
        </div>
        <p className="text-sm text-gray-600">
          Mã phòng: <span className="font-mono font-semibold text-black">{code}</span>
        </p>
      </DialogContent>
    </Dialog>
  );
}
