"use client";

import { CalendarDays, Flag, Globe, Link2, Share2, TrendingUp } from "lucide-react";
import Link from "next/link";
import { useState, type ReactNode } from "react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAuth } from "@/contexts/auth-context";
import ReportDialog from "@/modules/reports/report-dialog";
import LinkifiedText from "@/modules/watch/components/linkified-text";
import ShareDialog from "@/modules/watch/components/share-dialog";
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

/**
 * Hộp thoại "Giới thiệu" của kênh: mô tả đầy đủ, thông tin thêm, nút Chia sẻ kênh và Báo cáo người dùng
 * (như YouTube; mở từ "...xem thêm" ở đầu trang kênh).
 */
export default function ChannelAboutDialog({
  channel,
  isOwner,
  open,
  onOpenChange,
}: {
  channel: ChannelInfo;
  isOwner: boolean;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const { user, login } = useAuth();
  const [shareOpen, setShareOpen] = useState(false);
  const [reportOpen, setReportOpen] = useState(false);
  const country = countryName(channel.countryCode);
  const joined = new Date(channel.createdAt).toLocaleDateString("vi-VN", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
  const path = channel.handle ? `/@${channel.handle}` : `/channel/${channel.id}`;
  const link = typeof window !== "undefined" ? `${window.location.origin}${path}` : path;

  // Đóng hộp Giới thiệu rồi mới mở hộp khác, để không chồng hai hộp thoại
  const openShare = () => {
    onOpenChange(false);
    setShareOpen(true);
  };
  const openReport = () => {
    if (!user?.sub) {
      toast("Hãy đăng nhập để báo cáo", { action: { label: "Đăng nhập", onClick: login } });
      return;
    }
    onOpenChange(false);
    setReportOpen(true);
  };

  return (
    <>
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
                <Row icon={<Link2 className="size-5" />}>
                  <Link
                    href={path}
                    onClick={() => onOpenChange(false)}
                    className="break-all text-blue-600 hover:underline"
                  >
                    {link}
                  </Link>
                </Row>
                <Row icon={<CalendarDays className="size-5" />}>Đã tham gia {joined}</Row>
                {country && <Row icon={<Globe className="size-5" />}>{country}</Row>}
                <Row icon={<TrendingUp className="size-5" />}>{channel.totalViews.toLocaleString("vi-VN")} lượt xem</Row>
              </ul>
            </section>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={openShare}
                className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-full bg-gray-100 px-4 text-sm font-medium hover:bg-gray-200"
              >
                <Share2 className="size-4" />
                Chia sẻ kênh
              </button>
              {!isOwner && (
                <button
                  type="button"
                  onClick={openReport}
                  className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-full bg-gray-100 px-4 text-sm font-medium hover:bg-gray-200"
                >
                  <Flag className="size-4" />
                  Báo cáo người dùng
                </button>
              )}
            </div>
          </div>
        </DialogContent>
      </Dialog>
      <ShareDialog link={link} title={channel.name} open={shareOpen} onOpenChange={setShareOpen} />
      {!isOwner && (
        <ReportDialog
          open={reportOpen}
          onOpenChange={setReportOpen}
          target={{ channelId: channel.id }}
          title="Báo cáo người dùng"
        />
      )}
    </>
  );
}
