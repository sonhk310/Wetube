"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { toastEmitter } from "@/lib/toastEmitter";
import { playlistService } from "../list/playlist-API";
import { isPrivatePlaylist } from "../../types";

type Privacy = "PUBLIC" | "PRIVATE";

interface EditPlaylistDialogProps {
  playlist: { playlistId: string; playlistTitle: string; privacy: string };
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSaved: () => void;
}

/** Đổi tên và chế độ hiển thị của danh sách phát (chỉ danh sách tự tạo). */
export function EditPlaylistDialog({ playlist, open, onOpenChange, onSaved }: EditPlaylistDialogProps) {
  const [title, setTitle] = useState(playlist.playlistTitle);
  const [privacy, setPrivacy] = useState<Privacy>(isPrivatePlaylist(playlist.privacy) ? "PRIVATE" : "PUBLIC");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setTitle(playlist.playlistTitle);
    setPrivacy(isPrivatePlaylist(playlist.privacy) ? "PRIVATE" : "PUBLIC");
    setError(null);
  }, [open, playlist.playlistTitle, playlist.privacy]);

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      await playlistService.update(playlist.playlistId, { title: title.trim(), privacy });
      toastEmitter.success("Đã lưu thay đổi");
      onOpenChange(false);
      onSaved();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !saving && onOpenChange(next)}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] sm:max-w-md rounded-2xl">
        <DialogHeader>
          <DialogTitle>Chỉnh sửa danh sách phát</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div className="space-y-1">
            <label htmlFor="playlist-title" className="text-sm font-medium">
              Tên
            </label>
            <Input
              id="playlist-title"
              value={title}
              maxLength={150}
              onChange={(e) => setTitle(e.target.value)}
              aria-invalid={!!error}
            />
            {error && <p className="text-sm text-red-600">{error}</p>}
          </div>
          <div className="space-y-1">
            <span className="text-sm font-medium">Chế độ hiển thị</span>
            <Select value={privacy} onValueChange={(v) => v && setPrivacy(v as Privacy)}>
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="PUBLIC">Công khai</SelectItem>
                <SelectItem value="PRIVATE">Riêng tư</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="ghost" className="rounded-full" onClick={() => onOpenChange(false)} disabled={saving}>
            Huỷ
          </Button>
          <Button className="rounded-full" onClick={save} disabled={!title.trim() || saving}>
            Lưu
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
