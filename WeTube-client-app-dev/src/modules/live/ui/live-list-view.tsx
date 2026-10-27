"use client";

import { RadioTower } from "lucide-react";
import { useGetLiveListQuery } from "@/app/api/liveApi";
import { Skeleton } from "@/components/ui/skeleton";
import LiveCardItem from "./live-card";

/** Hỏi lại danh sách mỗi chừng này: buổi mới lên sóng / vừa kết thúc tự cập nhật. */
const REFRESH_MS = 30_000;

/** Lưới các buổi đang phát, nhiều người xem trước. `embedded`: nằm trong trang chủ (chip Trực tiếp), không có tiêu đề. */
export default function LiveListView({ embedded = false }: { embedded?: boolean }) {
  const { data, isLoading } = useGetLiveListQuery(undefined, { pollingInterval: REFRESH_MS });

  return (
    <section className="px-6 pb-8 pt-4">
      {embedded ? null : <h1 className="mb-4 text-2xl font-bold tracking-tight">Đang phát trực tiếp</h1>}
      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="aspect-video w-full rounded-xl" />
          ))}
        </div>
      ) : !data || data.length === 0 ? (
        <div className="flex flex-col items-center gap-2 py-20 text-gray-500">
          <RadioTower className="size-10" />
          <p>Hiện chưa có ai phát trực tiếp.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-x-4 gap-y-8 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {data.map((live) => (
            <LiveCardItem key={live.id} live={live} />
          ))}
        </div>
      )}
    </section>
  );
}
