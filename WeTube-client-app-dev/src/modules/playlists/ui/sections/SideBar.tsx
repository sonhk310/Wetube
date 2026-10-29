"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  FileText,
  MessageCircle,
  MessageSquare,
  Pause,
  Play,
  Search,
  Settings,
  Trash2,
} from "lucide-react";
import { playlistService } from "../list/playlist-API";
import { useHistoryStore } from "@/store/zustand/useHistoryStore";
import { useWatchProgressStore } from "@/lib/watchProgress";
import { toastEmitter } from "@/lib/toastEmitter";
import { confirmDialog } from "@/lib/dialogs";

const Sidebar = () => {
  const query = useHistoryStore((s) => s.query);
  const setQuery = useHistoryStore((s) => s.setQuery);
  const markCleared = useHistoryStore((s) => s.markCleared);
  const [paused, setPaused] = useState<boolean | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    playlistService
      .getHistoryPaused()
      .then(setPaused)
      .catch((err) => console.error(err));
  }, []);

  const handleClear = async () => {
    const ok = await confirmDialog({
      title: "Xoá toàn bộ nhật ký xem?",
      message: "Thao tác này không hoàn tác được.",
      confirmLabel: "Xoá nhật ký",
      destructive: true,
    });
    if (!ok) return;
    setBusy(true);
    try {
      await playlistService.clearHistory();
      markCleared();
      useWatchProgressStore.getState().reset(); // bỏ thanh đỏ trên thumbnail
      toastEmitter.success("Đã xoá nhật ký xem");
    } catch (err) {
      toastEmitter.error((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const handleTogglePause = async () => {
    if (paused === null) return;
    setBusy(true);
    try {
      await playlistService.setHistoryPaused(!paused);
      setPaused(!paused);
      toastEmitter.success(paused ? "Đã bật lưu nhật ký xem" : "Đã tạm dừng lưu nhật ký xem");
    } catch (err) {
      toastEmitter.error((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className=" p-8  border-gray-200 bg-white z-40">

      <div className="relative mb-8">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500 h-4 w-4" />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Tìm kiếm trong nhật ký xem"
          className="h-9 rounded-none border-0 border-b border-gray-300 bg-transparent pl-10 shadow-none text-black placeholder-gray-500 focus-visible:border-black focus-visible:ring-0"
        />
      </div>

      <div className="space-y-4">
        <Button variant="ghost" disabled={busy} onClick={handleClear} className="w-full justify-start h-auto p-3 text-black hover:bg-gray-100 text-left">
          <Trash2 className="h-5 w-5 mr-3 flex-shrink-0" />
          <span>Xóa tất cả nhật ký xem</span>
        </Button>
        <Button variant="ghost" disabled={busy || paused === null} onClick={handleTogglePause} className="w-full justify-start h-auto p-3 text-black hover:bg-gray-100 text-left">
          {paused ? (
            <Play className="h-5 w-5 mr-3 flex-shrink-0" />
          ) : (
            <Pause className="h-5 w-5 mr-3 flex-shrink-0" />
          )}
          <span>{paused ? "Bật lưu nhật ký xem" : "Tạm dừng lưu nhật ký xem"}</span>
        </Button>
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-black hover:bg-gray-100 text-left">
          <Settings className="h-5 w-5 mr-3 flex-shrink-0" />
          <span>Quản lý toàn bộ nhật ký hoạt động</span>
        </Button>
      </div>

      <div className="mt-8 pt-6 border-t border-gray-200 space-y-2">
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-gray-600 hover:bg-gray-100 hover:text-black text-left">
          <MessageCircle className="h-4 w-4 mr-3 flex-shrink-0" />
          <span>Bình luận</span>
        </Button>
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-gray-600 hover:bg-gray-100 hover:text-black text-left">
          <FileText className="h-4 w-4 mr-3 flex-shrink-0" />
          <span>Bài đăng</span>
        </Button>
        <Button variant="ghost" className="w-full justify-start h-auto p-3 text-gray-600 hover:bg-gray-100 hover:text-black text-left">
          <MessageSquare className="h-4 w-4 mr-3 flex-shrink-0" />
          <span>Trò chuyện trực tiếp</span>
        </Button>
      </div>
    </div>
  );
};

export default Sidebar;
