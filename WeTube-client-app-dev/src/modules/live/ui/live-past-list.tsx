"use client";

import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useDispatch } from "react-redux";
import { useDeleteLivesMutation, useGetMyPastLivesQuery } from "@/app/api/liveApi";
import { videoApi } from "@/app/api/videoApi";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";
import { useLiveRecordingStore } from "@/store/zustand/useLiveRecordingStore";
import { formatLiveDuration } from "../lib/format";
import type { MyPastLive } from "../types";

const PAGE_SIZE = 20;

/** Còn chờ trình duyệt tải bản ghi lên: xoá bây giờ thì bản ghi mất, nên chưa cho chọn. */
const waitingForUpload = (live: MyPastLive) => live.recordingStatus === "STOPPING";

function ReplayStatus({ live }: { live: MyPastLive }) {
  if (live.replayVideoId) {
    return (
      <Link href={`/watch/${live.replayVideoId}`} className="text-sm font-medium text-blue-700 hover:underline">
        Có video xem lại
      </Link>
    );
  }
  if (waitingForUpload(live)) {
    return <span className="text-sm text-amber-700">Đang chờ tải bản ghi lên…</span>;
  }
  return (
    <span className="text-sm text-gray-500" title={live.recordingMessage ?? undefined}>
      {live.recordingEnabled ? "Không có video xem lại" : "Không ghi hình"}
    </span>
  );
}

/** Hộp thoại xoá: hỏi lại, và cho chọn xoá cả video xem lại (mặc định không, video vẫn nằm ở Nội dung). */
function DeleteLivesDialog({
  selected,
  deleting,
  onCancel,
  onConfirm,
}: {
  selected: MyPastLive[] | null;
  deleting: boolean;
  onCancel: () => void;
  onConfirm: (deleteVideos: boolean) => void;
}) {
  const [confirmed, setConfirmed] = useState(false);
  const [withVideos, setWithVideos] = useState(false);
  const open = !!selected;
  useEffect(() => {
    if (open) {
      setConfirmed(false);
      setWithVideos(false);
    }
  }, [open]);

  const count = selected?.length ?? 0;
  const videoCount = selected?.filter((l) => l.replayVideoId).length ?? 0;

  return (
    <Dialog open={open} onOpenChange={(next) => !next && !deleting && onCancel()}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] rounded-2xl sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="text-xl">
            {count === 1 ? "Xoá buổi phát này?" : `Xoá ${count} buổi phát đã chọn?`}
          </DialogTitle>
        </DialogHeader>
        <ul className="max-h-40 space-y-1 overflow-y-auto rounded-lg bg-gray-100 p-3 text-sm">
          {selected?.map((live) => (
            <li key={live.id} className="truncate">
              {live.title}
            </li>
          ))}
        </ul>
        <p className="text-sm text-gray-600">
          Buổi phát sẽ biến mất khỏi tab Trực tiếp của kênh, kèm các bình chọn và thông báo của buổi đó.
        </p>
        {videoCount > 0 ? (
          <label className="flex cursor-pointer items-start gap-3 text-sm">
            <input
              type="checkbox"
              checked={withVideos}
              onChange={(e) => setWithVideos(e.target.checked)}
              className="mt-0.5 h-4 w-4 accent-red-600"
            />
            <span>
              Xoá cả {videoCount === 1 ? "video xem lại" : `${videoCount} video xem lại`} (kèm tệp trên máy chủ).{" "}
              <span className="text-gray-500">Không tick thì video vẫn nằm trong Nội dung của kênh.</span>
            </span>
          </label>
        ) : null}
        <label className="flex cursor-pointer items-start gap-3 text-sm">
          <input
            type="checkbox"
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
            className="mt-0.5 h-4 w-4 accent-red-600"
          />
          <span>Tôi hiểu rằng việc xoá này có hiệu lực vĩnh viễn và không khôi phục được.</span>
        </label>
        <DialogFooter className="flex-row justify-end gap-2">
          <Button variant="ghost" className="rounded-full" onClick={onCancel} disabled={deleting}>
            Huỷ
          </Button>
          <Button
            variant="destructive"
            className="rounded-full"
            onClick={() => onConfirm(withVideos)}
            disabled={!confirmed || deleting}
          >
            {deleting ? "Đang xoá…" : "Xoá vĩnh viễn"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Studio: các buổi đã phát của kênh (kể cả buổi không có video xem lại, không hiện ở tab công khai), tick chọn nhiều
 * rồi xoá. Danh sách làm mới khi bản ghi vừa tải lên xong.
 */
export default function LivePastList() {
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<MyPastLive[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [confirming, setConfirming] = useState<MyPastLive[] | null>(null);
  const { data, isLoading, isFetching, refetch } = useGetMyPastLivesQuery({ page, size: PAGE_SIZE });
  const [deleteLives, { isLoading: deleting }] = useDeleteLivesMutation();
  const dispatch = useDispatch();
  const recordingPhase = useLiveRecordingStore((s) => s.phase);

  useEffect(() => {
    if (!data) return;
    setItems((prev) =>
      page === 0 ? data.items : [...prev, ...data.items.filter((item) => !prev.some((p) => p.id === item.id))]
    );
  }, [data, page]);

  // Bản ghi vừa tải lên xong (hoặc bỏ): buổi đổi trạng thái, lấy lại danh sách
  useEffect(() => {
    if (recordingPhase === "done" || recordingPhase === "idle" || recordingPhase === "failed") void refetch();
  }, [recordingPhase, refetch]);

  // Buổi đã biến mất (xoá ở tab khác) thì bỏ khỏi phần đã chọn
  useEffect(() => {
    setSelectedIds((prev) => {
      const next = new Set([...prev].filter((id) => items.some((l) => l.id === id && !waitingForUpload(l))));
      return next.size === prev.size ? prev : next;
    });
  }, [items]);

  const selectable = useMemo(() => items.filter((l) => !waitingForUpload(l)), [items]);
  const allSelected = selectable.length > 0 && selectable.every((l) => selectedIds.has(l.id));

  const toggle = (id: string) =>
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (!next.delete(id)) next.add(id);
      return next;
    });

  const onConfirm = async (deleteVideos: boolean) => {
    if (!confirming) return;
    try {
      const res = await deleteLives({ ids: confirming.map((l) => l.id), deleteVideos }).unwrap();
      toastEmitter.success(
        res.videosDeleted > 0
          ? `Đã xoá ${res.deleted} buổi phát và ${res.videosDeleted} video xem lại`
          : `Đã xoá ${res.deleted} buổi phát`
      );
      if (res.videosDeleted > 0) dispatch(videoApi.util.invalidateTags(["VideoList", "Playlist"]));
      setConfirming(null);
      setSelectedIds(new Set());
      setItems([]);
      setPage(0);
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không xoá được buổi phát, vui lòng thử lại."));
    }
  };

  if (isLoading && items.length === 0) {
    return (
      <div className="space-y-3 px-6 pb-8">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-20 w-full rounded-xl" />
      </div>
    );
  }
  if (items.length === 0) return null;

  return (
    <section className="space-y-3 px-6 pb-10">
      <div className="flex flex-wrap items-center gap-3">
        <h2 className="text-lg font-semibold">Các buổi đã phát{data ? ` (${data.total})` : ""}</h2>
        <label className="ml-auto flex cursor-pointer items-center gap-2 text-sm">
          <input
            type="checkbox"
            className="h-4 w-4"
            checked={allSelected}
            onChange={() => setSelectedIds(allSelected ? new Set() : new Set(selectable.map((l) => l.id)))}
          />
          Chọn tất cả
        </label>
        <Button
          variant="destructive"
          className="rounded-full"
          disabled={selectedIds.size === 0}
          onClick={() => setConfirming(items.filter((l) => selectedIds.has(l.id)))}
        >
          Xoá đã chọn{selectedIds.size > 0 ? ` (${selectedIds.size})` : ""}
        </Button>
      </div>

      <ul className="divide-y rounded-xl border">
        {items.map((live) => {
          const duration = formatLiveDuration(live.startedAt, live.endedAt);
          return (
            <li key={live.id} className="flex items-center gap-4 p-3">
              <input
                type="checkbox"
                className="h-4 w-4 shrink-0"
                aria-label={`Chọn ${live.title}`}
                checked={selectedIds.has(live.id)}
                disabled={waitingForUpload(live)}
                onChange={() => toggle(live.id)}
              />
              <div className="relative aspect-video w-32 shrink-0 overflow-hidden rounded-md bg-neutral-900">
                {live.thumbnailUrl ? (
                  <Image src={live.thumbnailUrl} alt="" fill sizes="128px" className="object-cover" />
                ) : null}
                {duration ? (
                  <span className="absolute bottom-1 right-1 rounded bg-black/75 px-1 text-xs font-semibold text-white">
                    {duration}
                  </span>
                ) : null}
              </div>
              <div className="min-w-0 flex-1">
                <Link href={`/live/${live.id}`} className="line-clamp-2 font-medium hover:text-blue-700">
                  {live.title}
                </Link>
                <p className="text-xs text-gray-500">
                  {live.endedAt ? new Date(live.endedAt).toLocaleString("vi-VN") : "Đã phát"} · cao nhất{" "}
                  {live.peakViewers.toLocaleString("vi-VN")} người xem
                </p>
                <div className="mt-0.5">
                  <ReplayStatus live={live} />
                </div>
              </div>
            </li>
          );
        })}
      </ul>

      {data?.hasMore ? (
        <div className="flex justify-center">
          <Button
            variant="secondary"
            className="rounded-full"
            disabled={isFetching}
            onClick={() => setPage((p) => p + 1)}
          >
            Xem thêm
          </Button>
        </div>
      ) : null}

      <DeleteLivesDialog
        selected={confirming}
        deleting={deleting}
        onCancel={() => setConfirming(null)}
        onConfirm={(withVideos) => void onConfirm(withVideos)}
      />
    </section>
  );
}
