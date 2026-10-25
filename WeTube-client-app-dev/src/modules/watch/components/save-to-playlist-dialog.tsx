"use client";

import { useCallback, useEffect, useState } from "react";
import Image from "next/image";
import { toast } from "sonner";
import { useDispatch } from "react-redux";
import { Bookmark, Ellipsis, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { useAuth } from "@/contexts/auth-context";
import { playlistApi } from "@/app/api/playlistApi";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { isPrivatePlaylist, playlistDisplayTitle } from "@/modules/playlists/types";
import { Spinner } from "@/components/ui/spinner";
import { cn } from "@/lib/utils";

interface PlaylistOption {
  playlistId: string;
  playlistTitle: string;
  playlistType: string;
  privacy?: string;
  thumbnailUrl?: string | null;
}

interface SaveToPlaylistDialogProps {
  readonly videoId: string;
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
}

/** Ảnh bìa nhỏ có 2 vạch xếp chồng như thẻ danh sách phát; danh sách trống thì ô xám có "...". */
function PlaylistThumb({ src, alt }: { src?: string | null; alt: string }) {
  return (
    <div className="w-[72px] shrink-0">
      <div className="mx-auto h-0.5 w-[80%] rounded-t-sm bg-gray-300" />
      <div className="mx-auto mt-px h-0.5 w-[90%] rounded-t-sm bg-gray-400" />
      <div className="relative mt-px aspect-video overflow-hidden rounded-md bg-gray-200">
        {src ? (
          <Image src={src} alt={alt} fill sizes="72px" className="object-cover" />
        ) : (
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="rounded bg-white/70 px-1.5 text-gray-500">
              <Ellipsis className="size-4" />
            </span>
          </div>
        )}
      </div>
    </div>
  );
}

export default function SaveToPlaylistDialog({
  videoId,
  open,
  onOpenChange,
}: SaveToPlaylistDialogProps) {
  const { user } = useAuth();
  const dispatch = useDispatch();
  const [playlists, setPlaylists] = useState<PlaylistOption[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [busyId, setBusyId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [view, setView] = useState<"list" | "create">("list");
  const [newTitle, setNewTitle] = useState("");
  const [newPrivacy, setNewPrivacy] = useState<"private" | "public">("private");
  const [isCreating, setIsCreating] = useState(false);

  const load = useCallback(async () => {
    if (!user?.sub) return;
    setIsLoading(true);
    try {
      const [watchLater, created, containing] = await Promise.all([
        // Tài khoản chưa có danh sách Xem sau: API trả 404, chỉ bỏ hàng đó
        playlistService.getByUserAndType(user.sub, "WATCH_LATER").catch(() => []),
        playlistService.getCreated(user.sub),
        playlistService.getContaining(videoId),
      ]);
      setPlaylists([...watchLater, ...created]);
      setSelected(new Set<string>(containing));
    } catch (err) {
      console.error(err);
      toast.error("Không tải được danh sách phát của bạn");
    } finally {
      setIsLoading(false);
    }
  }, [user?.sub, videoId]);

  useEffect(() => {
    if (!open) return;
    setView("list");
    setNewTitle("");
    setNewPrivacy("private");
    void load();
  }, [open, load]);

  const refreshCaches = () => dispatch(playlistApi.util.invalidateTags(["Playlist"]));

  const toggle = async (p: PlaylistOption) => {
    if (busyId) return;
    const add = !selected.has(p.playlistId);
    const name = playlistDisplayTitle(p.playlistType, p.playlistTitle);
    setBusyId(p.playlistId);
    try {
      if (add) await playlistService.addVideo(p.playlistId, videoId);
      else await playlistService.removeVideo(p.playlistId, videoId);
      setSelected((prev) => {
        const next = new Set(prev);
        if (add) next.add(p.playlistId);
        else next.delete(p.playlistId);
        return next;
      });
      toast.success(add ? `Đã lưu vào ${name}` : `Đã xoá khỏi ${name}`);
      refreshCaches();
    } catch (err) {
      console.error(err);
      toast.error(add ? "Không lưu được vào danh sách phát" : "Không gỡ được khỏi danh sách phát");
    } finally {
      setBusyId(null);
    }
  };

  const handleCreate = async () => {
    const title = newTitle.trim();
    if (!user?.sub || !title || isCreating) return;
    setIsCreating(true);
    try {
      const res = await playlistService.createUserPlaylist(user.sub, title, newPrivacy);
      await playlistService.addVideo(res.playlistId, videoId);
      toast.success(`Đã lưu vào ${title}`);
      refreshCaches();
      onOpenChange(false);
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Không tạo được danh sách phát");
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !isCreating && onOpenChange(next)}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] sm:max-w-sm rounded-2xl p-0 gap-0">
        {view === "list" ? (
          <>
            <DialogHeader className="px-6 pt-5 pb-3">
              <DialogTitle>Lưu vào...</DialogTitle>
            </DialogHeader>

            {!user ? (
              <p className="px-6 pb-6 text-sm text-gray-600">Hãy đăng nhập để lưu video vào danh sách phát.</p>
            ) : (
              <>
                <div className="max-h-80 overflow-y-auto px-2">
                  {isLoading && (
                    <div className="flex justify-center py-6">
                      <Spinner />
                    </div>
                  )}
                  {!isLoading &&
                    playlists.map((p) => {
                      const name = playlistDisplayTitle(p.playlistType, p.playlistTitle);
                      const saved = selected.has(p.playlistId);
                      return (
                        <button
                          key={p.playlistId}
                          type="button"
                          onClick={() => toggle(p)}
                          disabled={busyId === p.playlistId}
                          aria-pressed={saved}
                          className="flex w-full items-center gap-3 rounded-lg px-4 py-2 text-left hover:bg-gray-100 disabled:opacity-60 cursor-pointer"
                        >
                          <PlaylistThumb src={p.thumbnailUrl} alt={name} />
                          <span className="min-w-0 flex-1">
                            <span className="block truncate text-sm font-medium">{name}</span>
                            <span className="block text-xs text-gray-500">
                              {isPrivatePlaylist(p.privacy) ? "Riêng tư" : "Công khai"}
                            </span>
                          </span>
                          <Bookmark className={cn("size-6 shrink-0", saved && "fill-current")} />
                        </button>
                      );
                    })}
                </div>

                <div className="border-t p-3">
                  <Button
                    type="button"
                    variant="secondary"
                    className="w-full rounded-full"
                    onClick={() => setView("create")}
                  >
                    <Plus className="size-5" /> Danh sách phát mới
                  </Button>
                </div>
              </>
            )}
          </>
        ) : (
          <div className="flex flex-col gap-4 p-6">
            <DialogHeader>
              <DialogTitle>Danh sách phát mới</DialogTitle>
            </DialogHeader>
            <Input
              value={newTitle}
              maxLength={150}
              autoFocus
              placeholder="Chọn một tiêu đề"
              aria-label="Tiêu đề"
              onChange={(e) => setNewTitle(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") void handleCreate();
              }}
              className="h-14 rounded-xl px-4"
            />
            <Select value={newPrivacy} onValueChange={(v) => v && setNewPrivacy(v as "private" | "public")}>
              <SelectTrigger className="w-full h-14 data-[size=default]:h-14 rounded-xl px-4">
                <span className="flex flex-col items-start">
                  <span className="text-xs text-gray-500">Chế độ hiển thị</span>
                  <SelectValue />
                </span>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="private">Riêng tư</SelectItem>
                <SelectItem value="public">Công khai</SelectItem>
              </SelectContent>
            </Select>
            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                className="rounded-full"
                onClick={() => setView("list")}
                disabled={isCreating}
              >
                Hủy
              </Button>
              <Button
                type="button"
                className="rounded-full"
                onClick={handleCreate}
                disabled={!newTitle.trim() || isCreating}
              >
                {isCreating ? "Đang tạo..." : "Tạo"}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
