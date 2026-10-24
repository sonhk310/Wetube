"use client";

import { useEffect, useState } from "react";
import { ChevronDown, Plus } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/auth-context";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { toastEmitter } from "@/lib/toastEmitter";

interface PlaylistOption {
  playlistId: string;
  playlistTitle: string;
}

interface PlaylistPickerProps {
  value: string[];
  onChange: (ids: string[]) => void;
}

/** Chọn nhiều danh sách phát do người dùng tạo; tạo được danh sách mới (riêng tư) ngay tại đây. */
export function PlaylistPicker({ value, onChange }: PlaylistPickerProps) {
  const { user } = useAuth();
  const [playlists, setPlaylists] = useState<PlaylistOption[]>([]);
  const [newTitle, setNewTitle] = useState("");
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    if (!user?.sub) return;
    playlistService
      .getCreated(user.sub)
      .then(setPlaylists)
      .catch(() => toastEmitter.error("Không tải được danh sách phát của bạn"));
  }, [user?.sub]);

  const toggle = (id: string, checked: boolean) =>
    onChange(checked ? [...value, id] : value.filter((v) => v !== id));

  const handleCreate = async () => {
    const title = newTitle.trim();
    if (!user?.sub || !title) return;
    setCreating(true);
    try {
      const { playlistId } = await playlistService.createUserPlaylist(user.sub, title, "private");
      setPlaylists((prev) => [...prev, { playlistId, playlistTitle: title }]);
      onChange([...value, playlistId]);
      setNewTitle("");
    } catch (err) {
      toastEmitter.error((err as Error).message);
    } finally {
      setCreating(false);
    }
  };

  const selectedNames = playlists.filter((p) => value.includes(p.playlistId)).map((p) => p.playlistTitle);

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex w-full items-center justify-between rounded-md border border-gray-300 px-3 py-2 text-left text-sm hover:border-gray-400"
        >
          <span className="truncate">{selectedNames.length ? selectedNames.join(", ") : "Chọn"}</span>
          <ChevronDown className="h-4 w-4 shrink-0 text-gray-500" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-[var(--radix-dropdown-menu-trigger-width)] min-w-64">
        {playlists.length === 0 && <p className="px-2 py-1.5 text-sm text-gray-500">Bạn chưa có danh sách phát nào</p>}
        {playlists.map((p) => (
          <DropdownMenuCheckboxItem
            key={p.playlistId}
            checked={value.includes(p.playlistId)}
            onCheckedChange={(checked) => toggle(p.playlistId, checked === true)}
            onSelect={(e) => e.preventDefault()}
          >
            {p.playlistTitle}
          </DropdownMenuCheckboxItem>
        ))}
        <DropdownMenuSeparator />
        <div className="flex items-center gap-2 p-2" onKeyDown={(e) => e.stopPropagation()}>
          <Input
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder="Tạo danh sách phát mới"
            className="h-8 text-sm"
          />
          <Button
            type="button"
            size="sm"
            variant="secondary"
            disabled={!newTitle.trim() || creating}
            onClick={handleCreate}
          >
            <Plus className="h-4 w-4" />
          </Button>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
