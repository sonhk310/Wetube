"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import { useLoadingBar } from "@/contexts/loading-bar-context";

/** Link / URL này có dẫn sang trang khác trong site (khác đường dẫn hoặc query) không. */
export function isNewPage(href: string): boolean {
  const next = new URL(href, window.location.href);
  if (next.origin !== window.location.origin) return false;
  return next.pathname + next.search !== window.location.pathname + window.location.search;
}

/** `useRouter` của Next, nhưng `push` / `replace` sang trang khác thì chạy thanh đỏ trên cùng. */
export function useProgressRouter() {
  const router = useRouter();
  const { start } = useLoadingBar();

  return useMemo(
    () => ({
      ...router,
      push: (href: string, options?: Parameters<typeof router.push>[1]) => {
        if (isNewPage(href)) start();
        router.push(href, options);
      },
      replace: (href: string, options?: Parameters<typeof router.replace>[1]) => {
        if (isNewPage(href)) start();
        router.replace(href, options);
      },
    }),
    [router, start]
  );
}
