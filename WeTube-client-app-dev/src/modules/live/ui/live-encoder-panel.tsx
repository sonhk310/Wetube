"use client";

import { useState } from "react";
import { Copy, Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCopyToClipboard } from "@/lib/clipboard";
import { toastEmitter } from "@/lib/toastEmitter";
import type { LiveView } from "../types";

function Field({ label, value, secret }: { label: string; value: string; secret?: boolean }) {
  const [, copy] = useCopyToClipboard();
  const [shown, setShown] = useState(!secret);
  return (
    <div className="space-y-1">
      <Label>{label}</Label>
      <div className="flex gap-2">
        <Input readOnly value={value} type={shown ? "text" : "password"} className="font-mono text-sm" />
        {secret ? (
          <Button type="button" variant="outline" size="icon" onClick={() => setShown((s) => !s)} aria-label="Hiện / ẩn">
            {shown ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
          </Button>
        ) : null}
        <Button
          type="button"
          variant="outline"
          size="icon"
          aria-label="Chép"
          onClick={async () => {
            if (await copy(value)) toastEmitter.success("Đã chép");
          }}
        >
          <Copy className="size-4" />
        </Button>
      </div>
    </div>
  );
}

/** Địa chỉ máy chủ và khoá luồng cho phần mềm phát (OBS), kèm hướng dẫn theo giao thức. */
export default function LiveEncoderPanel({ live }: { live: LiveView }) {
  if (!live.ingress) {
    return <p className="text-sm text-gray-500">Không có thông tin kết nối cho buổi phát này.</p>;
  }
  const whip = live.protocol !== "RTMP";
  return (
    <div className="space-y-4 rounded-xl border border-gray-200 p-4">
      <h2 className="font-semibold">Kết nối phần mềm phát</h2>
      <Field label={whip ? "Server (WHIP)" : "Server (RTMP)"} value={live.ingress.url} />
      <Field label={whip ? "Bearer Token" : "Khoá luồng (Stream key)"} value={live.ingress.streamKey} secret />
      <ol className="list-decimal space-y-1 pl-5 text-sm text-gray-600">
        <li>
          Trong OBS mở <b>Cài đặt → Luồng (Stream)</b>.
        </li>
        <li>
          Chọn Dịch vụ <b>{whip ? "WHIP" : "Tuỳ chỉnh (Custom)"}</b>.
        </li>
        <li>Dán Server và {whip ? "Bearer Token" : "Khoá luồng"} ở trên, rồi bấm Áp dụng.</li>
        <li>
          Ở ô <b>Nguồn (Sources)</b> của OBS, bấm dấu <b>+</b> và thêm nguồn hình (vd. <b>Chụp màn hình</b>). Chưa có
          nguồn thì OBS phát màn hình đen.
        </li>
        <li>
          Bấm <b>Bắt đầu phát (Start Streaming)</b>. Khi có hình, buổi phát tự chuyển sang trực tiếp.
        </li>
      </ol>
      <p className="text-xs text-gray-500">
        {whip ? "WHIP cần OBS phiên bản 30 trở lên. " : null}
        Chưa có OBS?{" "}
        <a href="https://obsproject.com/download" target="_blank" rel="noopener noreferrer" className="text-blue-600 hover:underline">
          Tải OBS miễn phí
        </a>
        .
      </p>
    </div>
  );
}
