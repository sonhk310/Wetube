"use client";

import { useEffect, useRef } from "react";
import { usePathname, useSearchParams } from "next/navigation";
import { useLoadingBar } from "@/contexts/loading-bar-context";
import { isNewPage } from "@/hooks/use-progress-router";

/**
 * Thanh đỏ trên cùng: chạy ngay khi bắt đầu chuyển trang (bấm link, Back / Forward; `router.push`
 * đi qua `useProgressRouter`) và xong khi URL mới đã hiện. Trang chờ server (trang xem, kênh…)
 * vì vậy có thanh chạy trong lúc chờ, thay vì chỉ chạy sau khi trang đã đổi.
 */
export function NavigationEvents() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { start, finish } = useLoadingBar();

  // URL đang hiện; Back / Forward chỉ đổi #hash thì không chạy thanh
  const shownRef = useRef("");

  useEffect(() => {
    shownRef.current = window.location.pathname + window.location.search;
    finish();
  }, [pathname, searchParams, finish]);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      // Không xét defaultPrevented: chính <Link> của Next gọi preventDefault() để tự chuyển trang.
      // Nút nằm trong link mà không muốn chuyển trang (vd. tắt tiếng ở thẻ video) đều stopPropagation(),
      // nên click của chúng không tới document.
      if (e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
      const a = (e.target as Element | null)?.closest?.("a");
      if (!a || !a.href || a.hasAttribute("download")) return;
      if (a.target && a.target !== "_self") return;
      if (isNewPage(a.href)) start();
    };
    const onPopState = () => {
      if (window.location.pathname + window.location.search !== shownRef.current) start();
    };
    document.addEventListener("click", onClick);
    window.addEventListener("popstate", onPopState);
    return () => {
      document.removeEventListener("click", onClick);
      window.removeEventListener("popstate", onPopState);
    };
  }, [start]);

  return null;
}
