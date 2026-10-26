"use client";

import { Play, Plus, X } from "lucide-react";
import Image from "next/image";
import { useEffect, useState } from "react";
import { useSearchVideosFullQuery } from "@/app/api/searchApi";
import { formatClock } from "@/lib/roomSync";
import { toastEmitter } from "@/lib/toastEmitter";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { sendRoom } from "../lib/roomSocket";
import type { RoomVideo } from "../types";

const SEARCH_DEBOUNCE_MS = 300;
const WATCH_LINK = /\/watch\/([0-9a-fA-F-]{36})/;

function Thumb({ src, alt }: { src?: string | null; alt: string }) {
  return (
    <div className="relative aspect-video w-24 shrink-0 overflow-hidden rounded-md bg-gray-200">
      {src && <Image src={src} alt={alt} fill sizes="96px" className="object-cover" />}
    </div>
  );
}

/** Tìm video (hoặc dán liên kết /watch/...) để thêm vào hàng chờ; ai trong phòng cũng thêm, xoá, phát ngay được. */
export default function RoomQueue({ className }: { className?: string }) {
  const queue = useRoomStore((s) => s.queue);
  const playingId = useRoomStore((s) => s.playback?.videoId);
  const [input, setInput] = useState("");
  const [query, setQuery] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => setQuery(input.trim()), SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [input]);

  const pastedId = input.match(WATCH_LINK)?.[1];
  const { data, isFetching } = useSearchVideosFullQuery(
    { query, size: 6 },
    { skip: query.length < 2 || !!pastedId }
  );
  const results = (data?.content ?? []).filter((v) => !queue.some((q) => q.id === v.id) && v.id !== playingId);

  const add = (videoId: string) => {
    sendRoom("queue", { op: "ADD", videoId });
    toastEmitter.success("Đã thêm vào hàng chờ");
    setInput("");
  };

  const playNow = (video: RoomVideo) => {
    sendRoom("control", {
      type: "CHANGE",
      version: useRoomStore.getState().playback?.version ?? 0,
      videoId: video.id,
    });
  };

  return (
    <div className={className}>
      <h2 className="border-b px-4 py-3 text-base font-semibold">Hàng chờ ({queue.length})</h2>
      <div className="border-b p-3">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Tìm video hoặc dán liên kết để thêm"
          aria-label="Tìm video để thêm vào hàng chờ"
          className="w-full rounded-full border px-4 py-2 text-sm outline-none focus:border-blue-500"
        />
        {pastedId && (
          <button
            type="button"
            onClick={() => add(pastedId)}
            className="mt-2 flex w-full items-center justify-center gap-1 rounded-full bg-blue-600 px-3 py-1.5 text-sm text-white hover:bg-blue-700"
          >
            <Plus className="size-4" /> Thêm video từ liên kết
          </button>
        )}
        {query.length >= 2 && !pastedId && (
          <div className="mt-2 space-y-1">
            {isFetching && <p className="px-1 text-sm text-gray-500">Đang tìm...</p>}
            {!isFetching && results.length === 0 && <p className="px-1 text-sm text-gray-500">Không có kết quả</p>}
            {results.map((v) => (
              <button
                key={v.id}
                type="button"
                onClick={() => add(v.id)}
                className="flex w-full items-center gap-2 rounded-md p-1 text-left hover:bg-gray-100"
              >
                <Thumb src={v.thumbnailUrl} alt={v.title} />
                <span className="min-w-0 flex-1">
                  <span className="line-clamp-2 text-sm font-medium">{v.title}</span>
                  <span className="block truncate text-xs text-gray-500">{v.user?.name}</span>
                </span>
                <Plus className="size-5 shrink-0 text-gray-500" />
              </button>
            ))}
          </div>
        )}
      </div>
      <div className="min-h-0 flex-1 space-y-1 overflow-y-auto p-3">
        {queue.length === 0 && (
          <p className="text-sm text-gray-500">Hàng chờ trống. Thêm video để cả phòng xem tiếp sau video này.</p>
        )}
        {queue.map((v) => (
          <div key={v.id} className="group flex items-center gap-2 rounded-md p-1 hover:bg-gray-100">
            <Thumb src={v.thumbnailUrl} alt={v.title} />
            <div className="min-w-0 flex-1">
              <p className="line-clamp-2 text-sm font-medium">{v.title}</p>
              <p className="truncate text-xs text-gray-500">
                {v.author} {v.duration > 0 && `· ${formatClock(v.duration)}`}
              </p>
            </div>
            <button
              type="button"
              onClick={() => playNow(v)}
              aria-label="Phát ngay"
              title="Phát ngay"
              className="flex size-8 shrink-0 items-center justify-center rounded-full hover:bg-gray-200"
            >
              <Play className="size-4" />
            </button>
            <button
              type="button"
              onClick={() => sendRoom("queue", { op: "REMOVE", videoId: v.id })}
              aria-label="Xoá khỏi hàng chờ"
              title="Xoá khỏi hàng chờ"
              className="flex size-8 shrink-0 items-center justify-center rounded-full hover:bg-gray-200"
            >
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
