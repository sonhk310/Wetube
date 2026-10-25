"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { ListVideo, MoreVertical, Pencil, Play, Trash2 } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { isEditablePlaylist, isPrivatePlaylist, playlistDisplayTitle } from "../../types";
import { EditPlaylistDialog } from "./edit-playlist-dialog";
import { DeletePlaylistDialog } from "./delete-playlist-dialog";

export interface PlaylistTileData {
  playlistId: string;
  playlistTitle: string;
  playlistType: string;
  privacy: string;
  totalVideos: number;
  thumbnailUrl: string | null;
  ownerName: string | null;
}

const FALLBACK_THUMBNAIL = "/images/thumbnail.png";

function pageHref(playlist: PlaylistTileData) {
  if (playlist.playlistType === "WATCH_LATER") return "/playlists/watchlater";
  if (playlist.playlistType === "LIKED") return "/playlists/liked";
  return `/playlists/${playlist.playlistId}`;
}

/** Thẻ danh sách phát kiểu YouTube: ảnh xếp chồng, "N video", rê chuột để "Phát tất cả". */
export function PlaylistTile({
  playlist,
  isOwner,
  onChanged,
}: {
  playlist: PlaylistTileData;
  isOwner: boolean;
  onChanged: () => void;
}) {
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const title = playlistDisplayTitle(playlist.playlistType, playlist.playlistTitle);
  const href = pageHref(playlist);
  const canEdit = isOwner && isEditablePlaylist(playlist.playlistType);

  return (
    <div className="group/tile">
      {/* Hai vạch phía trên ảnh: hiệu ứng nhiều video xếp chồng */}
      <div className="mx-auto h-1 w-[84%] rounded-t-md bg-gray-300" />
      <div className="mx-auto mt-px h-1 w-[92%] rounded-t-md bg-gray-400" />
      <div className="relative mt-px aspect-video overflow-hidden rounded-xl bg-gray-200">
        <Link href={href} aria-label={title} className="absolute inset-0">
          <Image
            src={playlist.thumbnailUrl || FALLBACK_THUMBNAIL}
            alt={title}
            fill
            sizes="(max-width: 768px) 100vw, 25vw"
            className="object-cover"
          />
        </Link>
        {playlist.totalVideos > 0 && (
          <Link
            href={`/playlists/${playlist.playlistId}?play=1`}
            className="absolute inset-0 flex items-center justify-center gap-2 bg-black/60 text-sm font-semibold text-white opacity-0 transition-opacity group-hover/tile:opacity-100"
          >
            <Play className="size-5" fill="currentColor" /> Phát tất cả
          </Link>
        )}
        <span className="pointer-events-none absolute bottom-2 right-2 flex items-center gap-1 rounded bg-black/80 px-1.5 py-0.5 text-xs font-medium text-white">
          <ListVideo className="size-3.5" /> {playlist.totalVideos} video
        </span>
      </div>

      <div className="mt-2 flex gap-1">
        <div className="min-w-0 flex-1">
          <Link href={href} className="line-clamp-2 font-semibold text-gray-900">
            {title}
          </Link>
          <p className="text-sm text-gray-600">
            {isPrivatePlaylist(playlist.privacy) ? "Riêng tư" : playlist.ownerName ?? ""} · Danh sách phát
          </p>
          <Link href={href} className="text-sm font-medium text-gray-700 hover:text-gray-900">
            Xem toàn bộ danh sách
          </Link>
        </div>
        {canEdit && (
          <DropdownMenu>
            <DropdownMenuTrigger
              className="h-8 w-8 shrink-0 rounded-full p-1.5 hover:bg-gray-100"
              aria-label="Tác vụ khác"
            >
              <MoreVertical className="size-5" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => setDeleteOpen(true)} className="cursor-pointer gap-3 py-2.5">
                <Trash2 className="size-5" /> Xoá
              </DropdownMenuItem>
              <DropdownMenuItem onSelect={() => setEditOpen(true)} className="cursor-pointer gap-3 py-2.5">
                <Pencil className="size-5" /> Chỉnh sửa
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>

      {canEdit && (
        <>
          <EditPlaylistDialog playlist={playlist} open={editOpen} onOpenChange={setEditOpen} onSaved={onChanged} />
          <DeletePlaylistDialog
            playlist={playlist}
            open={deleteOpen}
            onOpenChange={setDeleteOpen}
            onDeleted={onChanged}
          />
        </>
      )}
    </div>
  );
}
