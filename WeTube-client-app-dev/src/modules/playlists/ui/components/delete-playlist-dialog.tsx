"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { toastEmitter } from "@/lib/toastEmitter";
import { playlistService } from "../list/playlist-API";

interface DeletePlaylistDialogProps {
  playlist: { playlistId: string; playlistTitle: string };
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onDeleted: () => void;
}

/** Hỏi lại trước khi xoá danh sách phát (video trong danh sách không bị xoá). */
export function DeletePlaylistDialog({ playlist, open, onOpenChange, onDeleted }: DeletePlaylistDialogProps) {
  const [deleting, setDeleting] = useState(false);

  const remove = async () => {
    setDeleting(true);
    try {
      await playlistService.removePlaylist(playlist.playlistId);
      toastEmitter.success("Đã xoá danh sách phát");
      onOpenChange(false);
      onDeleted();
    } catch (err) {
      toastEmitter.error((err as Error).message);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(next) => !deleting && onOpenChange(next)}>
      <DialogContent className="grid-cols-[minmax(0,1fr)] sm:max-w-md rounded-2xl">
        <DialogHeader>
          <DialogTitle className="break-words">Xoá {playlist.playlistTitle}?</DialogTitle>
          <DialogDescription>Danh sách phát sẽ bị xoá vĩnh viễn.</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="ghost" className="rounded-full" onClick={() => onOpenChange(false)} disabled={deleting}>
            Huỷ
          </Button>
          <Button variant="destructive" className="rounded-full" onClick={remove} disabled={deleting}>
            Xoá
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
