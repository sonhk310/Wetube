"use client";

import { useEffect, useRef } from "react";
import { usePathname, useSearchParams } from "next/navigation";
import { useLoadingBar } from "@/contexts/loading-bar-context";

export function NavigationEvents() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { start, finish } = useLoadingBar();

  const isFirstRender = useRef(true);

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    start();

    const timer = setTimeout(() => {
      finish();
    }, 1000);

    return () => clearTimeout(timer);
  }, [pathname, searchParams, start, finish]);

  return null;
}
