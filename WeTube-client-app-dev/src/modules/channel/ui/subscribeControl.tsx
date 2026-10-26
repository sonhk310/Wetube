"use client";

import { useState } from "react";
import { Bell, BellOff, BellRing, Check, ChevronDown, UserMinus } from "lucide-react";
import { toast } from "sonner";
import {
  useSetNotificationModeMutation,
  useSubscribeMutation,
  useUnsubscribeMutation,
} from "@/app/api/subscriptionsApi";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/contexts/auth-context";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";
import type { NotificationMode } from "@/types/channel";

const MODES: { mode: NotificationMode; label: string; Icon: typeof Bell }[] = [
  { mode: "ALL", label: "Tất cả", Icon: BellRing },
  { mode: "PERSONALIZE", label: "Cá nhân hoá", Icon: Bell },
  { mode: "NONE_NOTIFICATION", label: "Không có thông báo", Icon: BellOff },
];

/**
 * Nút Đăng ký của trang kênh và trang xem. Đã đăng ký thì thành "Đã đăng ký ▾" kèm chuông: chọn chế độ thông báo
 * hoặc huỷ đăng ký (như YouTube). Khách bấm thì mời đăng nhập.
 */
export default function SubscribeControl({
  channelId,
  subscribed: serverSubscribed,
  notificationMode,
  onChanged,
}: {
  channelId: string;
  /** Trạng thái lúc tải trang; bấm xong thì nút tự theo kết quả mới. */
  subscribed: boolean;
  notificationMode?: NotificationMode | null;
  /** Gọi sau khi đăng ký / huỷ thành công (để chỗ dùng cập nhật số người đăng ký). */
  onChanged?: (subscribed: boolean) => void;
}) {
  const { user, login } = useAuth();
  const [subscribe, { isLoading: subscribing }] = useSubscribeMutation();
  const [unsubscribe, { isLoading: unsubscribing }] = useUnsubscribeMutation();
  const [setMode] = useSetNotificationModeMutation();
  // Trạng thái sau khi bấm; chưa bấm thì lấy từ server
  const [override, setOverride] = useState<{ subscribed: boolean; mode: NotificationMode | null } | null>(null);
  const subscribed = override?.subscribed ?? serverSubscribed;
  const mode = override ? override.mode : (notificationMode ?? null);

  const onSubscribe = async () => {
    if (!user?.sub) {
      toast("Hãy đăng nhập để đăng ký kênh", { action: { label: "Đăng nhập", onClick: login } });
      return;
    }
    try {
      await subscribe({ subscriberId: user.sub, channelId }).unwrap();
      // Backend đặt chế độ mặc định là "Cá nhân hoá" khi mới đăng ký
      setOverride({ subscribed: true, mode: "PERSONALIZE" });
      toast.success("Đã đăng ký kênh");
      onChanged?.(true);
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không đăng ký được kênh, vui lòng thử lại."));
    }
  };

  const onUnsubscribe = async () => {
    if (!user?.sub) return;
    try {
      await unsubscribe({ subscriberId: user.sub, channelId }).unwrap();
      setOverride({ subscribed: false, mode: null });
      toast.success("Đã huỷ đăng ký");
      onChanged?.(false);
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không huỷ đăng ký được, vui lòng thử lại."));
    }
  };

  const onPickMode = async (next: NotificationMode) => {
    if (next === mode) return;
    try {
      await setMode({ channelId, mode: next }).unwrap();
      setOverride({ subscribed: true, mode: next });
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không đổi được chế độ thông báo, vui lòng thử lại."));
    }
  };

  if (!subscribed) {
    return (
      <button
        type="button"
        onClick={onSubscribe}
        disabled={subscribing}
        className="h-9 rounded-full bg-black px-4 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-60"
      >
        Đăng ký
      </button>
    );
  }

  const Current = MODES.find((m) => m.mode === mode)?.Icon ?? Bell;
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          disabled={unsubscribing}
          className="inline-flex h-9 items-center gap-2 rounded-full bg-gray-100 px-4 text-sm font-medium hover:bg-gray-200 disabled:opacity-60"
        >
          <Current className="size-5" />
          Đã đăng ký
          <ChevronDown className="size-4" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-56">
        {MODES.map(({ mode: m, label, Icon }) => (
          <DropdownMenuItem key={m} onClick={() => onPickMode(m)}>
            <Icon className="size-4" />
            <span className="flex-1">{label}</span>
            {m === mode && <Check className="size-4" />}
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={onUnsubscribe}>
          <UserMinus className="size-4" />
          Huỷ đăng ký
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
