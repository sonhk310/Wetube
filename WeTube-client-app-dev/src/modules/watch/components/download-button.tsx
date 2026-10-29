"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowDownTrayIcon } from "@heroicons/react/24/outline";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useAuth } from "@/contexts/auth-context";
import { API_PREFIX } from "@/constants/appConstant";

/**
 * Nút "Tải xuống" (quyền lợi Premium). Premium: mở link tải của backend (302 tới Cloudinary kèm flag tải về) ở thẻ mới,
 * trình duyệt tải tệp rồi tự đóng thẻ; lỗi (hết lượt, video không xem được) hiện ngay ở thẻ đó chứ không làm mất trang xem.
 * Chưa Premium: hộp thoại giới thiệu. Khách: mời đăng nhập.
 */
export default function DownloadButton({ videoId }: { videoId: string }) {
  const { user, isPremium } = useAuth();
  const [upsellOpen, setUpsellOpen] = useState(false);

  const handleClick = () => {
    if (!user?.sub) {
      toast.error("Hãy đăng nhập để tải video");
      return;
    }
    if (!isPremium) {
      setUpsellOpen(true);
      return;
    }
    window.open(`${API_PREFIX}/videos/${videoId}/download`, "_blank", "noopener");
  };

  return (
    <>
      <button
        onClick={handleClick}
        className="flex items-center gap-2 bg-gray-100 hover:bg-gray-200 rounded-full cursor-pointer py-2 px-3 text-sm font-bold"
      >
        <ArrowDownTrayIcon className="size-5" />
        Tải xuống
      </button>

      <Dialog open={upsellOpen} onOpenChange={setUpsellOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Tải xuống là quyền lợi Premium</DialogTitle>
            <DialogDescription>
              Với WeTube Premium bạn tải video về máy, xem không quảng cáo và nghe tiếp khi tắt màn hình điện thoại.
            </DialogDescription>
          </DialogHeader>
          <Link
            href="/otherservices/premium"
            className="rounded-full bg-black px-4 py-2 text-center text-sm font-semibold text-white hover:bg-black/85"
          >
            Xem gói Premium
          </Link>
        </DialogContent>
      </Dialog>
    </>
  );
}
