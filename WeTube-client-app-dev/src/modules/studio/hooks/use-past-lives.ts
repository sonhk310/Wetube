"use client";

import { useCallback, useEffect, useState } from "react";
import { useLazyGetMyPastLivesQuery } from "@/app/api/liveApi";
import type { MyPastLive } from "@/modules/live/types";
import { useLiveRecordingStore } from "@/store/zustand/useLiveRecordingStore";

const PAGE_SIZE = 50; // trần của máy chủ
const MAX_PAGES = 20;

/**
 * Mọi buổi đã phát của kênh mình (kể cả buổi không có video xem lại), mới nhất trước, lấy hết các trang như danh
 * sách video của Nội dung. Tự tải lại khi bản ghi của buổi vừa rồi đổi trạng thái (tải lên xong, bỏ); `reload` để tải
 * lại sau khi xoá.
 */
export function usePastLives() {
  const [load] = useLazyGetMyPastLivesQuery();
  const [state, setState] = useState<{ lives: MyPastLive[]; isLoading: boolean; error: boolean }>({
    lives: [],
    isLoading: true,
    error: false,
  });
  const [nonce, setNonce] = useState(0);
  const recordingPhase = useLiveRecordingStore((s) => s.phase);
  const reload = useCallback(() => setNonce((n) => n + 1), []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const all: MyPastLive[] = [];
        for (let page = 0; page < MAX_PAGES; page++) {
          const res = await load({ page, size: PAGE_SIZE }, false).unwrap();
          all.push(...res.items);
          if (!res.hasMore) break;
        }
        if (!cancelled) setState({ lives: all, isLoading: false, error: false });
      } catch {
        if (!cancelled) setState((prev) => ({ ...prev, isLoading: false, error: true }));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [load, nonce, recordingPhase]);

  return { ...state, reload };
}
