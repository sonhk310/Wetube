"use client";

import { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { useDeleteLivesMutation } from "@/app/api/liveApi";
import { playlistApi } from "@/app/api/playlistApi";
import { videoApi } from "@/app/api/videoApi";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";
import type { MyPastLive } from "@/modules/live/types";

/**
 * Hỏi lại trước khi xoá các buổi đã phát, và cho chọn xoá cả video xem lại (mặc định không: video vẫn nằm ở Nội dung).
 * Mở khi `lives` khác null; xoá xong gọi `onDeleted` rồi `onClose`.
 */
export function DeleteLivesDialog({
  lives,
  onClose,
  onDeleted,
}: {
  lives: MyPastLive[] | null;
  onClose: () => void;
  onDeleted: () => void;
}) {
  const dispatch = useDispatch();
  const [deleteLives, { isLoading: deleting }] = useDeleteLivesMutation();
  const [confirmed, setConfirmed] = useState(false);
  const [withVideos, setWithVideos] = useState(false);
  const open = !!lives;
  useEffect(() => {
    if (open) {
      setConfirmed(false);
      setWithVideos(false);
    }
  }, [open]);

  const count = lives?.length ?? 0;
  const videoCount = lives?.filter((l) => l.replayVideoId).length ?? 0;

  const onConfirm = async () => {
    if (!lives) return;
    try {
      const res = await deleteLives({ ids: lives.map((l) => l.id), deleteVideos: withVideos }).unwrap();
      toastEmitter.success(
        res.videosDeleted > 0
          ? `Đã xoá ${res.deleted} buổi phát và ${res.videosDeleted} video xem lại`
          : `Đã xoá ${res.deleted} buổi phát`
      );
      if (res.videosDeleted > 0) {
        dispatch(playlistApi.util.invalidateTags(["Playlist"]));
        dispatch(videoApi.util.invalidateTags(["VideoList", "Playlist"]));
      }
      onDeleted();
      onClose();
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không xoá được buổi phát, vui lòng thử lại."));
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !next && !deleting && onClose()}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] rounded-2xl sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="text-xl">
            {count === 1 ? "Xoá buổi phát này?" : `Xoá ${count} buổi phát đã chọn?`}
          </DialogTitle>
        </DialogHeader>
        <ul className="max-h-40 space-y-1 overflow-y-auto rounded-lg bg-gray-100 p-3 text-sm">
          {lives?.map((live) => (
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
          <Button variant="ghost" className="rounded-full" onClick={onClose} disabled={deleting}>
            Huỷ
          </Button>
          <Button
            variant="destructive"
            className="rounded-full"
            onClick={() => void onConfirm()}
            disabled={!confirmed || deleting}
          >
            {deleting ? "Đang xoá…" : "Xoá vĩnh viễn"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
