"use client";

import { useState } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import type { MyPastLive } from "@/modules/live/types";
import { usePastLives } from "../../hooks/use-past-lives";
import { ContentRowShell } from "../components/content-row";
import { DeleteLivesDialog } from "../components/delete-lives-dialog";
import { LiveContentCard } from "../components/live-content-card";

/** Số thẻ tối đa trong hàng (mới nhất trước); còn lại xem ở "Xem tất cả". */
const ROW_MAX = 12;

/** Hàng "Trực tiếp" của trang Nội dung: các buổi đã phát của kênh, cùng dáng với hàng Video và Shorts. */
export default function LiveSection() {
  const { lives, isLoading, error, reload } = usePastLives();
  const [toDelete, setToDelete] = useState<MyPastLive[] | null>(null);

  if (isLoading) {
    return (
      <div className="px-6 py-4">
        <Skeleton className="mb-3 h-6 w-24" />
        <div className="flex gap-4 overflow-hidden">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="w-56 shrink-0">
              <Skeleton className="aspect-video w-full rounded-xl" />
              <Skeleton className="mt-2 h-4 w-3/4" />
            </div>
          ))}
        </div>
      </div>
    );
  }
  if (error) {
    return <div className="p-6 text-center text-red-500">Không tải được các buổi phát trực tiếp. Vui lòng thử lại sau.</div>;
  }

  return (
    <>
      <ContentRowShell
        title="Trực tiếp"
        viewAllHref="/studio/content?tab=live"
        count={Math.min(lives.length, ROW_MAX)}
        emptyText="Chưa có buổi phát trực tiếp nào đã kết thúc."
      >
        {lives.slice(0, ROW_MAX).map((live) => (
          <LiveContentCard key={live.id} live={live} onDelete={(l) => setToDelete([l])} />
        ))}
      </ContentRowShell>
      <DeleteLivesDialog lives={toDelete} onClose={() => setToDelete(null)} onDeleted={reload} />
    </>
  );
}
