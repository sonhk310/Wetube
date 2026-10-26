"use client";

import { Crown, LogOut, UserPlus, XCircle } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useGetVideoDetailQuery } from "@/app/api/videoApi";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Spinner } from "@/components/ui/spinner";
import { useAuth } from "@/contexts/auth-context";
import { useProgressRouter } from "@/hooks/use-progress-router";
import { cn } from "@/lib/utils";
import ActiveVideo from "@/modules/watch/components/video";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { useVideoStore } from "@/store/zustand/videoStore";
import { inviteFlagKey } from "../lib/createRoom";
import { sendRoom } from "../lib/roomSocket";
import { useRoomConnection } from "../lib/useRoomConnection";
import InviteDialog from "./invite-dialog";
import RoomChat from "./room-chat";
import RoomMembers from "./room-members";
import RoomQueue from "./room-queue";
import { ReactionBar, ReactionOverlay } from "./room-reactions";

/** Khung thông báo giữa trang (không vào được phòng, phòng đã đóng, chưa đăng nhập...). */
function Notice({ title, text, children }: { title: string; text?: string; children?: React.ReactNode }) {
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-md flex-col items-center justify-center gap-3 px-6 text-center">
      <h1 className="text-2xl font-bold">{title}</h1>
      {text && <p className="text-gray-600">{text}</p>}
      <div className="mt-2 flex gap-2">{children}</div>
    </div>
  );
}

const HomeButton = () => (
  <Button asChild>
    <Link href="/">Về trang chủ</Link>
  </Button>
);

export default function RoomView({ code }: { code: string }) {
  const { user, isLoading, login } = useAuth();
  useRoomConnection(code);
  const status = useRoomStore((s) => s.status);
  const errorCode = useRoomStore((s) => s.errorCode);
  const errorText = useRoomStore((s) => s.errorText);

  if (isLoading) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <Spinner className="size-8" />
      </div>
    );
  }

  if (!user) {
    return (
      <Notice title="Đăng nhập để vào phòng xem chung" text="Bạn cần đăng nhập để cùng xem và trò chuyện với bạn bè.">
        <Button
          onClick={() => {
            try {
              window.sessionStorage.setItem("wetube_after_login", `/rooms/${code}`);
            } catch {
              // sessionStorage bị chặn: đăng nhập xong thì mở lại liên kết mời
            }
            login();
          }}
        >
          Đăng nhập
        </Button>
      </Notice>
    );
  }

  if (status === "closed") {
    return <Notice title="Chủ phòng đã đóng phòng" text="Phòng xem chung này không còn nữa."><HomeButton /></Notice>;
  }
  if (status === "kicked") {
    return <Notice title="Bạn đã bị mời ra khỏi phòng" text="Chủ phòng đã mời bạn ra, nên bạn không vào lại được phòng này."><HomeButton /></Notice>;
  }
  if (status === "error") {
    const title =
      errorCode === "FULL" ? "Phòng đã đủ người" : errorCode === "KICKED" ? "Bạn không vào được phòng này" : "Phòng không tồn tại hoặc đã đóng";
    return <Notice title={title} text={errorText ?? undefined}><HomeButton /></Notice>;
  }
  if (status !== "joined") {
    return (
      <div className="grid gap-4 px-6 py-6 lg:grid-cols-[minmax(0,1fr)_380px]">
        <Skeleton className="aspect-video w-full rounded-xl" />
        <Skeleton className="h-96 w-full rounded-xl" />
      </div>
    );
  }

  return <JoinedRoom code={code} />;
}

function JoinedRoom({ code }: { code: string }) {
  const router = useProgressRouter();
  const { user } = useAuth();
  const videoId = useRoomStore((s) => s.playback?.videoId);
  const roomVideo = useRoomStore((s) => s.video);
  const iAmHost = useRoomStore((s) => s.hostUserId === user?.sub);
  const setVideoDetail = useVideoStore((s) => s.setVideoDetail);
  const clearVideoDetail = useVideoStore((s) => s.clearVideoDetail);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [tab, setTab] = useState<"chat" | "queue">("chat");

  // Trình phát đọc chi tiết video (đường dẫn, phụ đề, chương) từ store dùng chung với trang xem
  const { data } = useGetVideoDetailQuery({ videoId: videoId ?? "" }, { skip: !videoId });
  useEffect(() => {
    if (data) setVideoDetail(data);
  }, [data, setVideoDetail]);
  useEffect(() => () => clearVideoDetail(), [clearVideoDetail]);

  // Người vừa tạo phòng thấy hộp thoại mời ngay
  useEffect(() => {
    try {
      if (window.sessionStorage.getItem(inviteFlagKey(code))) {
        window.sessionStorage.removeItem(inviteFlagKey(code));
        setInviteOpen(true);
      }
    } catch {
      // sessionStorage bị chặn: bấm nút Mời khi cần
    }
  }, [code]);

  const leave = () => {
    sendRoom("leave", { immediate: true });
    router.push(videoId ? `/watch/${videoId}` : "/");
  };

  const closeRoom = () => {
    if (window.confirm("Đóng phòng? Mọi người đang xem sẽ bị đưa ra khỏi phòng.")) sendRoom("close");
  };

  const panel = "flex min-h-0 flex-col overflow-hidden rounded-xl border bg-white";

  return (
    <section className="grid gap-4 px-4 py-4 sm:px-6 lg:grid-cols-[minmax(0,1fr)_380px]">
      <div className="min-w-0">
        <div className="relative">
          {videoId ? (
            <ActiveVideo key={videoId} videoId={videoId} roomMode />
          ) : (
            <div className="flex aspect-video w-full items-center justify-center rounded-xl bg-black px-6 text-center text-white/80">
              Phòng chưa có video. Tìm và thêm video ở khung Hàng chờ để cả phòng cùng xem.
            </div>
          )}
          <ReactionOverlay />
        </div>

        {roomVideo && (
          <div className="mt-3">
            <h1 className="line-clamp-2 text-xl font-semibold">{roomVideo.title}</h1>
            {roomVideo.author && <p className="mt-1 text-sm text-gray-600">{roomVideo.author}</p>}
          </div>
        )}

        <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
          <ReactionBar />
          <div className="flex flex-wrap items-center gap-2">
            <Button variant="outline" size="sm" onClick={() => setInviteOpen(true)}>
              <UserPlus className="size-4" /> Mời
            </Button>
            <Button variant="outline" size="sm" onClick={leave}>
              <LogOut className="size-4" /> Rời phòng
            </Button>
            {iAmHost && (
              <Button variant="destructive" size="sm" onClick={closeRoom}>
                <XCircle className="size-4" /> Đóng phòng
              </Button>
            )}
          </div>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2">
          <RoomMembers />
          {iAmHost && (
            <span className="flex items-center gap-1 text-xs text-gray-500">
              <Crown className="size-3.5" /> Bạn là chủ phòng
            </span>
          )}
        </div>
      </div>

      <aside className="flex min-w-0 flex-col gap-3 lg:h-[calc(100vh-6rem)]">
        <div className="flex gap-2 lg:hidden" role="tablist">
          {(["chat", "queue"] as const).map((t) => (
            <button
              key={t}
              type="button"
              role="tab"
              aria-selected={tab === t}
              onClick={() => setTab(t)}
              className={cn(
                "rounded-lg px-3 py-1.5 text-sm font-medium",
                tab === t ? "bg-black text-white" : "bg-gray-100 hover:bg-gray-200"
              )}
            >
              {t === "chat" ? "Trò chuyện" : "Hàng chờ"}
            </button>
          ))}
        </div>
        <RoomChat className={cn(panel, "h-[26rem] lg:h-auto lg:flex-[3]", tab !== "chat" && "hidden lg:flex")} />
        <RoomQueue className={cn(panel, "h-[26rem] lg:h-auto lg:flex-[2]", tab !== "queue" && "hidden lg:flex")} />
      </aside>

      <InviteDialog code={code} open={inviteOpen} onOpenChange={setInviteOpen} />
    </section>
  );
}
