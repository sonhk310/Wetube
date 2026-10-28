"use client";

import Link from "next/link";
import { useGetLiveListQuery } from "@/app/api/liveApi";
import LiveCardItem from "./live-card";

/** Hỏi lại danh sách mỗi chừng này: buổi mới lên sóng / vừa kết thúc tự cập nhật. */
const REFRESH_MS = 30_000;
/** Cùng cỡ thẻ với lưới video trang chủ (3 thẻ một hàng); nhiều hơn thì có "Xem tất cả". */
const MAX_CARDS = 3;

/** Hàng "Đang phát trực tiếp" đầu trang chủ. Không có buổi nào đang phát thì không hiện gì. */
export default function LiveNowRow() {
  const { data } = useGetLiveListQuery(undefined, { pollingInterval: REFRESH_MS });
  if (!data || data.length === 0) return null;

  return (
    <section className="px-4 pt-4">
      <div className="mb-3 flex items-baseline justify-between">
        <h2 className="text-lg font-semibold">Đang phát trực tiếp</h2>
        {data.length > MAX_CARDS ? (
          <Link href="/live" className="text-sm font-medium text-blue-700 hover:underline">
            Xem tất cả ({data.length})
          </Link>
        ) : null}
      </div>
      <div className="flex flex-wrap gap-4">
        {data.slice(0, MAX_CARDS).map((live) => (
          <div key={live.id} className="w-full sm:w-[calc(33.333%-1rem)]">
            <LiveCardItem live={live} />
          </div>
        ))}
      </div>
    </section>
  );
}
