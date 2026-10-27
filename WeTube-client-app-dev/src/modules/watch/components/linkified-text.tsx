"use client";

import Link from "next/link";
import { ReactNode } from "react";
import { requestSeek } from "@/lib/watchSeek";

// Link http(s) hoặc mốc thời gian dạng m:ss / h:mm:ss
// và @handle của kênh (không bắt "abc@gmail.com") và #thẻ (cùng quy tắc với backend utility/Hashtags:
// không đứng sau chữ / số / & / #, tối đa 64 ký tự, không chỉ toàn số)
const TOKEN =
  /(https?:\/\/[^\s<>"]+)|(?<![\w:])((?:\d{1,2}:)?[0-5]?\d:[0-5]\d)(?![\w:])|(?<![\p{L}\p{N}_.@-])@([a-z0-9][a-z0-9_.-]{1,28}[a-z0-9])(?![\p{L}\p{N}_@])|(?<![\p{L}\p{N}\p{M}_&#])#([\p{L}\p{N}\p{M}_]{1,64})(?![\p{L}\p{N}\p{M}_])/giu;
const ONLY_DIGITS = /^\p{N}+$/u;
// Dấu câu ở cuối thường không thuộc link ("xem tại https://a.b/c.")
const URL_TRAILING = /[.,;:!?)\]}'"]+$/;

/** Văn bản có nhắc @handle này không (cùng cách nhận diện với phần hiển thị bên dưới). */
export function mentionsHandle(text: string, handle: string): boolean {
  const escaped = handle.replace(/\./g, "\\.");
  return new RegExp(`(?<![\\p{L}\\p{N}_.@-])@${escaped}(?![\\p{L}\\p{N}_@])`, "iu").test(text);
}

/** "1:02:03" -> 3723 */
export function parseTimestamp(text: string): number {
  return text.split(":").reduce((total, part) => total * 60 + Number(part), 0);
}

/**
 * Hiện văn bản (mô tả video, bình luận) với link bấm được (mở tab mới), mốc thời gian bấm được để
 * tua trình phát, @handle bấm được để mở trang kênh và #thẻ bấm được để mở trang /hashtag. Giữ nguyên xuống dòng.
 * seekable = false: mốc thời gian như 3:20 để nguyên chữ (ngoài trang xem không có trình phát để tua).
 */
export default function LinkifiedText({
  text,
  className,
  seekable = true,
}: {
  text: string;
  className?: string;
  seekable?: boolean;
}) {
  const parts: ReactNode[] = [];
  let last = 0;
  for (const match of text.matchAll(TOKEN)) {
    const start = match.index ?? 0;
    if (match[2] && !seekable) continue;
    if (match[4] && ONLY_DIGITS.test(match[4])) continue;
    let token = match[0];
    let end = start + token.length;

    if (match[1]) {
      const trailing = token.match(URL_TRAILING)?.[0] ?? "";
      token = token.slice(0, token.length - trailing.length);
      end -= trailing.length;
      if (!token) continue;
    }

    if (start > last) parts.push(text.slice(last, start));
    if (match[1]) {
      parts.push(
        <a
          key={start}
          href={token}
          target="_blank"
          rel="noopener noreferrer nofollow ugc"
          className="text-blue-600 hover:underline break-all"
        >
          {token}
        </a>
      );
    } else if (match[3]) {
      parts.push(
        <Link
          key={start}
          href={`/@${match[3].toLowerCase()}`}
          prefetch={false}
          className="font-medium text-violet-600 hover:underline"
        >
          {token}
        </Link>
      );
    } else if (match[4]) {
      parts.push(
        <Link
          key={start}
          href={`/hashtag/${encodeURIComponent(match[4].normalize("NFC").toLowerCase())}`}
          prefetch={false}
          className="font-medium text-violet-600 hover:underline"
        >
          {token}
        </Link>
      );
    } else {
      const seconds = parseTimestamp(token);
      parts.push(
        <button
          key={start}
          type="button"
          onClick={() => requestSeek(seconds)}
          className="text-blue-600 hover:underline"
        >
          {token}
        </button>
      );
    }
    last = end;
  }
  if (last < text.length) parts.push(text.slice(last));

  return (
    <p className={`whitespace-pre-wrap break-words ${className ?? ""}`}>{parts}</p>
  );
}
