"use client";

import { useEffect, useRef } from "react";
import { Check, Link2, Repeat, Timer } from "lucide-react";

/** Kích thước ước lượng để đặt menu không tràn khỏi khung video. */
export const CONTEXT_MENU_SIZE = { width: 288, height: 148 };

interface PlayerContextMenuProps {
  /** Toạ độ trong khung trình phát. */
  x: number;
  y: number;
  loop: boolean;
  /** "1:23" hiện cạnh "Sao chép URL video tại thời điểm hiện tại". */
  timeLabel: string;
  onToggleLoop: () => void;
  onCopyUrl: () => void;
  onCopyUrlAtTime: () => void;
  /** Đóng menu; target là phần tử vừa được bấm (nếu đóng do bấm ra ngoài). */
  onClose: (target?: EventTarget | null) => void;
}

/** Menu chuột phải của trình phát, giống YouTube: lặp lại, sao chép URL (tại thời điểm hiện tại). */
export default function PlayerContextMenu({
  x,
  y,
  loop,
  timeLabel,
  onToggleLoop,
  onCopyUrl,
  onCopyUrlAtTime,
  onClose,
}: PlayerContextMenuProps) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onPointerDown = (e: PointerEvent) => {
      if (ref.current?.contains(e.target as Node)) return;
      // Chuột phải lần nữa: để trình phát tự xử lý (mở menu mới / menu của trình duyệt)
      if (e.button === 2) return;
      onClose(e.target);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    const onBlur = () => onClose();
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKey);
    window.addEventListener("blur", onBlur);
    window.addEventListener("scroll", onBlur, true);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKey);
      window.removeEventListener("blur", onBlur);
      window.removeEventListener("scroll", onBlur, true);
    };
  }, [onClose]);

  const row = "flex w-full items-center gap-3 px-4 py-2.5 text-left hover:bg-white/10";
  const choose = (action: () => void) => () => {
    action();
    onClose();
  };

  return (
    <div
      ref={ref}
      role="menu"
      style={{ left: x, top: y, width: CONTEXT_MENU_SIZE.width }}
      className="absolute z-40 rounded-xl bg-neutral-900/80 py-2 text-sm text-white shadow-lg ring-1 ring-white/15 backdrop-blur-xl"
      onContextMenu={(e) => e.preventDefault()}
    >
      <button type="button" role="menuitemcheckbox" aria-checked={loop} className={row} onClick={choose(onToggleLoop)}>
        <Repeat size={18} />
        <span className="flex-1">Lặp lại</span>
        {loop && <Check size={18} />}
      </button>
      <button type="button" role="menuitem" className={row} onClick={choose(onCopyUrl)}>
        <Link2 size={18} />
        <span className="flex-1">Sao chép URL video</span>
      </button>
      <button type="button" role="menuitem" className={row} onClick={choose(onCopyUrlAtTime)}>
        <Timer size={18} />
        <span className="flex-1">Sao chép URL video tại {timeLabel}</span>
      </button>
    </div>
  );
}
