"use client";

import { useEffect, useState } from "react";
import { Check } from "lucide-react";
import { useChangeHandleMutation, useGetChannelInfoQuery, useLazyCheckHandleQuery } from "@/app/api/channelApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/contexts/auth-context";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";

/** Dừng gõ chừng này mới hỏi server handle có dùng được không. */
const CHECK_DELAY_MS = 400;

type Check = { state: "idle" } | { state: "checking" } | { state: "ok" } | { state: "bad"; reason: string };

/** Khung "Handle" ở Studio → Tuỳ chỉnh: xem và đổi @handle của kênh. */
export function ChannelHandleForm() {
  const { user } = useAuth();
  const channelId = user?.channelId;
  const { data: channel, isLoading } = useGetChannelInfoQuery(channelId ?? "", { skip: !channelId });
  const [triggerCheck] = useLazyCheckHandleQuery();
  const [changeHandle, { isLoading: isSaving }] = useChangeHandleMutation();
  const [value, setValue] = useState<string | null>(null);
  const [check, setCheck] = useState<Check>({ state: "idle" });

  const current = channel?.handle ?? "";
  const typed = (value ?? current).trim().replace(/^@/, "").toLowerCase();
  const unchanged = typed === current;

  useEffect(() => {
    if (value === null || unchanged || !typed) {
      setCheck({ state: "idle" });
      return;
    }
    setCheck({ state: "checking" });
    let cancelled = false;
    const timer = window.setTimeout(async () => {
      try {
        const res = await triggerCheck(typed).unwrap();
        if (cancelled) return;
        setCheck(res.available ? { state: "ok" } : { state: "bad", reason: res.reason ?? "Handle không dùng được" });
      } catch (err) {
        if (cancelled) return;
        setCheck({ state: "bad", reason: apiErrorMessage(err, "Không kiểm tra được handle, vui lòng thử lại.") });
      }
    }, CHECK_DELAY_MS);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [typed, unchanged, value, triggerCheck]);

  if (isLoading || !channel) {
    return <Skeleton className="h-24 w-full" />;
  }

  const save = async () => {
    if (!channelId) return;
    try {
      await changeHandle({ channelId, handle: typed }).unwrap();
      setValue(null);
      setCheck({ state: "idle" });
      toastEmitter.success("Đã đổi handle");
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không đổi được handle, vui lòng thử lại."));
    }
  };

  return (
    <div className="space-y-2">
      <Label htmlFor="channel-handle">Handle</Label>
      <p className="text-sm text-muted-foreground">
        Tên riêng không trùng của kênh, dùng để người khác nhắc đến bạn trong bình luận. Chỉ đổi được 1 lần mỗi 14 ngày.
      </p>
      <div className="flex max-w-md items-center gap-2">
        <div className="relative flex-1">
          <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">@</span>
          <Input
            id="channel-handle"
            value={value ?? current}
            onChange={(e) => setValue(e.target.value)}
            maxLength={31}
            autoComplete="off"
            spellCheck={false}
            aria-invalid={check.state === "bad"}
            className="pl-7"
          />
        </div>
        <Button onClick={save} disabled={unchanged || check.state !== "ok" || isSaving}>
          {isSaving ? "Đang lưu..." : "Lưu"}
        </Button>
      </div>
      <p className="min-h-5 text-sm">
        {check.state === "checking" && <span className="text-muted-foreground">Đang kiểm tra...</span>}
        {check.state === "ok" && (
          <span className="inline-flex items-center gap-1 text-green-600">
            <Check className="size-4" /> Dùng được
          </span>
        )}
        {check.state === "bad" && <span className="text-destructive">{check.reason}</span>}
      </p>
      {typed && (
        <p className="break-all text-xs text-muted-foreground">
          Link kênh: {typeof window !== "undefined" ? window.location.origin : ""}/@{typed}
        </p>
      )}
    </div>
  );
}
