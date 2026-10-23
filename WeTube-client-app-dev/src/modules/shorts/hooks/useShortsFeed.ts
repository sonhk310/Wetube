// modules/shorts/hooks/useShortsFeed.ts
"use client";

import { useCallback, useEffect, useState } from "react";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";

export interface ShortVideo {
  id: string;
  videoUrl: string;
  title: string;
  totalView: number;
  tagsAsString?: string;
  user?: { id: string; name: string; picture: string };
}

const PAGE_SIZE = 10;

/** Shorts thật từ backend (GET /api/videos/shorts), tải thêm khi cuộn tới cuối. */
export function useShortsFeed() {
  const [shorts, setShorts] = useState<ShortVideo[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  const loadMore = useCallback(async () => {
    if (loading || !hasMore) return;
    setLoading(true);
    try {
      const res = await apiFetch(`${API_PREFIX}/videos/shorts?page=${page}&size=${PAGE_SIZE}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data: { content: ShortVideo[]; last: boolean } = await res.json();
      setShorts((prev) => {
        const seen = new Set(prev.map((s) => s.id));
        return [...prev, ...data.content.filter((s) => !seen.has(s.id))];
      });
      setHasMore(!data.last);
      setPage((p) => p + 1);
    } catch (err) {
      console.error("Load shorts failed:", err);
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [loading, hasMore, page]);

  useEffect(() => {
    loadMore();
    // chỉ tải trang đầu khi mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { shorts, loadMore, loading, hasMore, error };
}
