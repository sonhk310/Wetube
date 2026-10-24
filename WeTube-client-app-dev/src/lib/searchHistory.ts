"use client";

import { useCallback, useEffect, useState } from "react";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";

const LOCAL_KEY = "wetube_search_history";
const LOCAL_MAX = 20;
/** Số mục hiện trong ô gợi ý khi ô tìm kiếm còn trống / đang gõ. */
const EMPTY_LIMIT = 10;
const TYPING_LIMIT = 3;

const normalize = (q: string) => q.trim().replace(/\s+/g, " ").slice(0, 200);

function readLocal(): string[] {
  try {
    const raw = window.localStorage.getItem(LOCAL_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((x) => typeof x === "string") : [];
  } catch {
    return [];
  }
}

function writeLocal(items: string[]) {
  try {
    window.localStorage.setItem(LOCAL_KEY, JSON.stringify(items.slice(0, LOCAL_MAX)));
  } catch {
    // localStorage bị chặn: bỏ qua
  }
}

const sameQuery = (a: string, b: string) => a.toLocaleLowerCase("vi") === b.toLocaleLowerCase("vi");

/**
 * Lịch sử tìm kiếm cho ô gợi ý: đã đăng nhập thì lưu trên server (theo tài khoản), khách thì lưu
 * trong trình duyệt. `prefix` rỗng: các từ khoá gần nhất; đang gõ: vài từ khoá bắt đầu bằng prefix.
 */
export function useSearchHistory(prefix: string, loggedIn: boolean, enabled: boolean) {
  const [items, setItems] = useState<string[]>([]);
  const p = normalize(prefix);
  const limit = p ? TYPING_LIMIT : EMPTY_LIMIT;

  useEffect(() => {
    if (!enabled) return;
    if (!loggedIn) {
      const lower = p.toLocaleLowerCase("vi");
      setItems(readLocal().filter((q) => q.toLocaleLowerCase("vi").startsWith(lower)).slice(0, limit));
      return;
    }
    const controller = new AbortController();
    // Chờ người dùng ngừng gõ một chút rồi mới hỏi server
    const timer = window.setTimeout(() => {
      apiFetch(`${API_PREFIX}/search-history?prefix=${encodeURIComponent(p)}&limit=${limit}`, {
        signal: controller.signal,
      })
        .then((res) => (res.ok ? res.json() : []))
        .then((data) => setItems(Array.isArray(data) ? data : []))
        .catch(() => {});
    }, p ? 150 : 0);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [p, limit, loggedIn, enabled]);

  const remove = useCallback(
    (query: string) => {
      setItems((prev) => prev.filter((q) => !sameQuery(q, query)));
      if (!loggedIn) {
        writeLocal(readLocal().filter((q) => !sameQuery(q, query)));
        return;
      }
      apiFetch(`${API_PREFIX}/search-history?query=${encodeURIComponent(query)}`, { method: "DELETE" }).catch(
        () => {}
      );
    },
    [loggedIn]
  );

  return { items, remove };
}

/** Ghi một lần tìm kiếm vào lịch sử (không chờ, lỗi thì bỏ qua). */
export function recordSearch(rawQuery: string, loggedIn: boolean) {
  const query = normalize(rawQuery);
  if (!query) return;
  if (!loggedIn) {
    writeLocal([query, ...readLocal().filter((q) => !sameQuery(q, query))]);
    return;
  }
  apiFetch(`${API_PREFIX}/search-history`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query }),
  }).catch(() => {});
}
