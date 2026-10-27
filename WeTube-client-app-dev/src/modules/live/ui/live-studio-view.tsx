"use client";

import { useGetMyLiveQuery } from "@/app/api/liveApi";
import { Skeleton } from "@/components/ui/skeleton";
import LiveOnAirPanel from "./live-onair-panel";
import LiveSetupForm from "./live-setup-form";

/** Hỏi lại buổi của mình mỗi chừng này (STOMP báo trước, đây là dự phòng, nhất là lúc chờ tín hiệu OBS). */
const REFRESH_MS = 10_000;

/** Trang phát của chủ kênh: chưa có buổi nào thì form chuẩn bị, có buổi chưa kết thúc thì bảng đang phát. */
export default function LiveStudioView() {
  const { data: mine, isLoading, refetch } = useGetMyLiveQuery(undefined, { pollingInterval: REFRESH_MS });

  if (isLoading) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="aspect-video w-full max-w-3xl rounded-xl" />
      </div>
    );
  }
  if (mine) {
    // key theo id: mỗi buổi phát mới dựng lại phần webcam / STOMP từ đầu
    return <LiveOnAirPanel key={mine.id} live={mine} />;
  }
  return (
    <div>
      <h1 className="px-6 pt-6 text-2xl font-bold tracking-tight">Phát trực tiếp</h1>
      <LiveSetupForm onConflict={() => void refetch()} />
    </div>
  );
}
