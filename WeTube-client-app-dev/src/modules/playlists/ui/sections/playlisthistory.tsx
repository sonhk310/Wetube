"use client";

import { useAuth } from "@/contexts/auth-context";
import HistoryList from "./history-list";

export default function PlaylistHistory() {
  const { user } = useAuth();

  if (!user?.sub) return <p>Vui lòng đăng nhập để xem lịch sử</p>;

  return <HistoryList userId={user.sub} />;
}
