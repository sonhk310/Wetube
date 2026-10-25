"use client";

import { Flag, ListPlus, MessageSquareText, MoreVertical, Share2, ThumbsDown, ThumbsUp } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn, formatCompact } from "@/lib/utils";

interface ShortsActionsProps {
  /** "side": cột bên phải video (máy tính); "overlay": nằm trên video (điện thoại). */
  variant: "side" | "overlay";
  className?: string;
  likeCount: number;
  liked: boolean;
  disliked: boolean;
  commentCount?: number;
  onLike: () => void;
  onDislike: () => void;
  onComment: () => void;
  onShare: () => void;
  onSave: () => void;
  onReport: () => void;
}

const circleClass = (variant: ShortsActionsProps["variant"]) =>
  cn(
    "flex items-center justify-center rounded-full transition-colors",
    variant === "side" ? "size-12 bg-gray-100 hover:bg-gray-200 text-black" : "size-11 bg-black/30 text-white"
  );

function Action({
  variant,
  icon: Icon,
  text,
  active,
  onClick,
  ariaLabel,
}: {
  variant: ShortsActionsProps["variant"];
  icon: LucideIcon;
  text: string;
  active?: boolean;
  onClick: () => void;
  ariaLabel: string;
}) {
  return (
    <div className="flex flex-col items-center">
      <button type="button" aria-label={ariaLabel} aria-pressed={active} onClick={onClick} className={circleClass(variant)}>
        <Icon className={cn("size-6", active && "fill-current")} />
      </button>
      <span className={cn("mt-1 text-xs font-medium", variant === "overlay" && "text-white drop-shadow")}>{text}</span>
    </div>
  );
}

/** Cột nút của Short như YouTube: Thích, Không thích, Bình luận, Chia sẻ, ⋮. */
export function ShortsActions({
  variant,
  className,
  likeCount,
  liked,
  disliked,
  commentCount,
  onLike,
  onDislike,
  onComment,
  onShare,
  onSave,
  onReport,
}: ShortsActionsProps) {
  return (
    <div className={cn("flex flex-col items-center gap-4", className)}>
      <Action variant={variant} icon={ThumbsUp} text={formatCompact(likeCount)} active={liked} onClick={onLike} ariaLabel="Thích" />
      <Action variant={variant} icon={ThumbsDown} text="Không thích" active={disliked} onClick={onDislike} ariaLabel="Không thích" />
      <Action
        variant={variant}
        icon={MessageSquareText}
        text={commentCount === undefined ? "Bình luận" : formatCompact(commentCount)}
        onClick={onComment}
        ariaLabel="Bình luận"
      />
      <Action variant={variant} icon={Share2} text="Chia sẻ" onClick={onShare} ariaLabel="Chia sẻ" />
      <DropdownMenu>
        <DropdownMenuTrigger aria-label="Tác vụ khác" className={circleClass(variant)}>
          <MoreVertical className="size-6" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" side={variant === "side" ? "left" : "top"}>
          <DropdownMenuItem onSelect={onSave} className="cursor-pointer gap-3 py-2.5">
            <ListPlus className="size-5" /> Lưu vào danh sách phát
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={onReport} className="cursor-pointer gap-3 py-2.5">
            <Flag className="size-5" /> Báo cáo
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}
