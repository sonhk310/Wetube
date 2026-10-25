"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { useDispatch } from "react-redux";
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
import { Spinner } from "@/components/ui/spinner";

interface PlaylistOption {
  playlistId: string;
  playlistTitle: string;
  privacy?: string;
}

interface SaveToPlaylistDialogProps {
  readonly videoId: string;
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
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
  const [newTitle, setNewTitle] = useState("");
  const [newPrivacy, setNewPrivacy] = useState<"private" | "public">("private");
  const [isCreating, setIsCreating] = useState(false);

  const load = useCallback(async () => {
    if (!user?.sub) return;
    setIsLoading(true);
    try {
      const [created, containing] = await Promise.all([
        playlistService.getCreated(user.sub),
        playlistService.getContaining(videoId),
      ]);
      setPlaylists(created);
      setSelected(new Set<string>(containing));
    } catch (err) {
      console.error(err);
      toast.error("Không tải được danh sách phát của bạn");
    } finally {
      setIsLoading(false);
    }
  }, [user?.sub, videoId]);

  useEffect(() => {
    if (open) void load();
  }, [open, load]);

  const refreshCaches = () => dispatch(playlistApi.util.invalidateTags(["Playlist"]));

  const toggle = async (playlistId: string, checked: boolean) => {
    setBusyId(playlistId);
    try {
      if (checked) await playlistService.addVideo(playlistId, videoId);
      else await playlistService.removeVideo(playlistId, videoId);
      setSelected((prev) => {
        const next = new Set(prev);
        if (checked) next.add(playlistId);
        else next.delete(playlistId);
        return next;
      });
      refreshCaches();
    } catch (err) {
      console.error(err);
      toast.error(checked ? "Không lưu được vào danh sách phát" : "Không gỡ được khỏi danh sách phát");
    } finally {
      setBusyId(null);
    }
  };

  const handleCreate = async () => {
    const title = newTitle.trim();
    if (!user?.sub || !title) return;
    setIsCreating(true);
    try {
      const res = await playlistService.createUserPlaylist(user.sub, title, newPrivacy);
      await playlistService.addVideo(res.playlistId, videoId);
      setNewTitle("");
      toast.success(`Đã lưu vào "${title}"`);
      refreshCaches();
      await load();
    } catch (err) {
      console.error(err);
      toast.error(err instanceof Error ? err.message : "Không tạo được danh sách phát");
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Lưu video vào...</DialogTitle>
        </DialogHeader>

        {!user ? (
          <p className="text-sm text-gray-600">Hãy đăng nhập để lưu video vào danh sách phát.</p>
        ) : (
          <>
            <div className="max-h-60 overflow-y-auto flex flex-col gap-1">
              {isLoading && (
                <div className="flex justify-center py-3">
                  <Spinner />
                </div>
              )}
              {!isLoading && playlists.length === 0 && (
                <p className="text-sm text-gray-500">Bạn chưa có danh sách phát nào.</p>
              )}
              {playlists.map((p) => (
                <label
                  key={p.playlistId}
                  className="flex items-center gap-3 py-1.5 px-1 rounded hover:bg-gray-100 cursor-pointer text-sm"
                >
                  <input
                    type="checkbox"
                    checked={selected.has(p.playlistId)}
                    disabled={busyId === p.playlistId}
                    onChange={(e) => toggle(p.playlistId, e.target.checked)}
                  />
                  <span className="flex-1 line-clamp-1">{p.playlistTitle}</span>
                  <span className="text-xs text-gray-500">
                    {p.privacy?.toLowerCase() === "private" ? "Riêng tư" : "Công khai"}
                  </span>
                </label>
              ))}
            </div>

            <div className="border-t pt-3 flex flex-col gap-2">
              <p className="text-sm font-semibold">Tạo danh sách phát mới</p>
              <Input
                value={newTitle}
                maxLength={100}
                placeholder="Tên danh sách phát"
                onChange={(e) => setNewTitle(e.target.value)}
              />
              <Select value={newPrivacy} onValueChange={(v) => v && setNewPrivacy(v as "private" | "public")}>
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="private">Riêng tư</SelectItem>
                  <SelectItem value="public">Công khai</SelectItem>
                </SelectContent>
              </Select>
              <Button
                type="button"
                onClick={handleCreate}
                disabled={!newTitle.trim() || isCreating}
              >
                {isCreating ? "Đang tạo..." : "Tạo và lưu video"}
              </Button>
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
