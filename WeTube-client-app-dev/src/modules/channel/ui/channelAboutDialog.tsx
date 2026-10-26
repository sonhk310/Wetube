"use client";

import { CalendarDays, Globe, Link2, TrendingUp } from "lucide-react";
import type { ReactNode } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import LinkifiedText from "@/modules/watch/components/linkified-text";
import type { ChannelInfo } from "@/types/channel";

/** Tên quốc gia tiếng Việt từ mã 2 chữ; null nếu không rõ (mã "UN" là chưa xác định). */
function countryName(code: string | null | undefined): string | null {
  if (!code || code === "UN") return null;
  try {
    return new Intl.DisplayNames(["vi"], { type: "region" }).of(code) ?? null;
  } catch {
    return null;
  }
}

function Row({ icon, children }: { icon: ReactNode; children: ReactNode }) {
  return (
    <li className="flex items-start gap-3 text-sm">
      <span className="mt-0.5 text-gray-500">{icon}</span>
      <span className="min-w-0 break-words">{children}</span>
    </li>
  );
}

/** Hộp thoại "Giới thiệu" của kênh: mô tả đầy đủ và thông tin thêm (mở từ "...xem thêm" ở đầu trang kênh). */
export default function ChannelAboutDialog({
  channel,
  open,
  onOpenChange,
}: {
  channel: ChannelInfo;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const country = countryName(channel.countryCode);
  const joined = new Date(channel.createdAt).toLocaleDateString("vi-VN", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
  const link = channel.handle && typeof window !== "undefined" ? `${window.location.origin}/@${channel.handle}` : null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] max-h-[85vh] overflow-y-auto sm:max-w-lg rounded-2xl">
        <DialogHeader>
          <DialogTitle className="break-words">{channel.name}</DialogTitle>
          <DialogDescription className="sr-only">Giới thiệu về kênh {channel.name}</DialogDescription>
        </DialogHeader>
        <div className="space-y-5">
          <section>
            <h3 className="mb-1 text-base font-semibold">Giới thiệu</h3>
            {channel.description ? (
              <LinkifiedText text={channel.description} seekable={false} className="text-sm" />
            ) : (
              <p className="text-sm text-gray-500">Kênh này chưa có mô tả.</p>
            )}
          </section>
          <section>
            <h3 className="mb-2 text-base font-semibold">Thông tin thêm</h3>
            <ul className="space-y-3">
              {link && (
                <Row icon={<Link2 className="size-5" />}>
                  <span className="break-all">{link}</span>
                </Row>
              )}
              <Row icon={<CalendarDays className="size-5" />}>Đã tham gia {joined}</Row>
              {country && <Row icon={<Globe className="size-5" />}>{country}</Row>}
              <Row icon={<TrendingUp className="size-5" />}>{channel.totalViews.toLocaleString("vi-VN")} lượt xem</Row>
            </ul>
          </section>
        </div>
      </DialogContent>
    </Dialog>
  );
}
