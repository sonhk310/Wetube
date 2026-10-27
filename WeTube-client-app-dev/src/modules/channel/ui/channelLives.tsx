"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { useGetChannelLivesQuery } from "@/app/api/liveApi";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import type { LiveCard } from "@/modules/live/types";
import LiveCardItem from "@/modules/live/ui/live-card";

const PAGE_SIZE = 12;

/** Tab Trực tiếp của kênh: buổi đang phát (nếu có) ở đầu, rồi các buổi đã phát, mới nhất trước. */
export default function ChannelLives() {
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  const [page, setPage] = useState(0);
  const [past, setPast] = useState<LiveCard[]>([]);
  const { data, isLoading } = useGetChannelLivesQuery(
    { channelId: channel?.id ?? "", page, size: PAGE_SIZE },
    { skip: !channel?.id }
  );

  useEffect(() => {
    if (!data) return;
    setPast((prev) =>
      page === 0 ? data.past : [...prev, ...data.past.filter((item) => !prev.some((p) => p.id === item.id))]
    );
  }, [data, page]);

  if (isLoading || !channel) {
    return (
      <div className="grid grid-cols-1 gap-4 pt-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="aspect-video w-full rounded-xl" />
        ))}
      </div>
    );
  }

  const current = data?.current;
  if (!current && past.length === 0) {
    return <p className="py-16 text-center text-gray-500">Kênh này chưa có buổi phát trực tiếp nào.</p>;
  }

  return (
    <div className="space-y-8 pt-4">
      {current ? (
        <section>
          <h2 className="mb-3 text-lg font-semibold">Đang phát trực tiếp</h2>
          <div className="max-w-md">
            <LiveCardItem live={current} />
          </div>
        </section>
      ) : null}
      {past.length > 0 ? (
        <section>
          <h2 className="mb-3 text-lg font-semibold">Các buổi đã phát</h2>
          <div className="grid grid-cols-1 gap-x-4 gap-y-8 sm:grid-cols-2 lg:grid-cols-3">
            {past.map((live) => (
              <LiveCardItem key={live.id} live={live} />
            ))}
          </div>
          {data?.hasMore ? (
            <div className="mt-6 flex justify-center">
              <Button variant="secondary" className="rounded-full" onClick={() => setPage((p) => p + 1)}>
                Xem thêm
              </Button>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
