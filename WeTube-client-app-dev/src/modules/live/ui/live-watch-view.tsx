"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Share2 } from "lucide-react";
import { toast } from "sonner";
import { useEndLiveMutation, useGetLiveQuery, useVotePollMutation } from "@/app/api/liveApi";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/contexts/auth-context";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";
import { formatCompact, timeAgo } from "@/lib/utils";
import SubscribeControl from "@/modules/channel/ui/subscribeControl";
import JoinMembershipButton from "@/modules/membership/join-membership-button";
import LinkifiedText from "@/modules/watch/components/linkified-text";
import ShareDialog from "@/modules/watch/components/share-dialog";
import { useLiveChannel } from "../lib/useLiveChannel";
import { formatLiveDuration, liveUrl } from "../lib/format";
import type { RecordingStatus } from "../types";
import { LiveBadge, ViewerCount } from "./live-badge";
import LiveEndDialog from "./live-end-dialog";
import LiveChat from "./live-chat";
import LivePlayer from "./live-player";
import LivePollCard from "./live-poll-card";

const FALLBACK_AVATAR = "https://placehold.co/160x160?text=W";
/**
 * Khi buổi chưa lên sóng (STOMP cũng báo, đây là dự phòng) hoặc bản ghi chưa xong, hỏi lại thông tin mỗi chừng này.
 */
const CREATED_POLL_MS = 30_000;

function WatchSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-x-6 px-6 py-6 md:grid-cols-6 xxl:grid-cols-8 xxl:px-20">
      <div className="col-span-1 md:col-span-4 xxl:col-span-6">
        <Skeleton className="aspect-video w-full rounded-xl" />
        <Skeleton className="mt-4 h-7 w-3/4" />
        <Skeleton className="mt-4 h-10 w-1/2" />
      </div>
      <Skeleton className="col-span-1 mt-6 h-96 rounded-xl md:col-span-2 md:mt-0" />
    </div>
  );
}

/**
 * Màn hình thay cho trình phát khi buổi đã kết thúc: ngày phát, thời lượng, số người xem cao nhất. Có bản ghi thì nút
 * Xem lại; đang xử lý thì báo; chủ kênh còn thấy lý do khi không có video xem lại.
 */
function EndedScreen({
  endedAt,
  startedAt,
  peakViewers,
  replayVideoId,
  recordingStatus,
  recordingMessage,
}: {
  endedAt: string | null;
  startedAt: string | null;
  peakViewers: number;
  replayVideoId: string | null;
  recordingStatus: RecordingStatus;
  recordingMessage: string | null;
}) {
  const duration = formatLiveDuration(startedAt, endedAt);
  return (
    <div className="flex aspect-video w-full flex-col items-center justify-center gap-2 rounded-xl bg-neutral-900 px-6 text-center text-white">
      <p className="text-xl font-bold">Buổi phát trực tiếp đã kết thúc</p>
      <p className="text-sm text-neutral-300">
        {endedAt ? `Kết thúc lúc ${new Date(endedAt).toLocaleString("vi-VN")}` : null}
        {duration ? ` · Thời lượng ${duration}` : null}
        {` · Cao nhất ${peakViewers.toLocaleString("vi-VN")} người xem`}
      </p>
      {replayVideoId ? (
        <Link
          href={`/watch/${replayVideoId}`}
          className="mt-2 rounded-full bg-white px-5 py-2 text-sm font-semibold text-black hover:bg-neutral-200"
        >
          Xem lại
        </Link>
      ) : recordingStatus === "STOPPING" ? (
        <p className="mt-1 text-sm text-neutral-300">Đang xử lý bản ghi…</p>
      ) : recordingMessage ? (
        <p className="mt-1 max-w-md text-sm text-amber-300">{recordingMessage}</p>
      ) : null}
    </div>
  );
}

export default function LiveWatchView({ id }: { id: string }) {
  const { user, login } = useAuth();
  const [pollMs, setPollMs] = useState(0);
  const { data: live, isLoading, isError, refetch } = useGetLiveQuery(id, { pollingInterval: pollMs });
  const { data: channel } = useGetChannelInfoQuery(live?.channelId ?? "", { skip: !live?.channelId });
  const channelState = useLiveChannel(live ? id : undefined, !!user?.sub);
  const [endLive, { isLoading: ending }] = useEndLiveMutation();
  const [votePoll, { isLoading: voting }] = useVotePollMutation();
  const [shareOpen, setShareOpen] = useState(false);
  const [endOpen, setEndOpen] = useState(false);
  const [expanded, setExpanded] = useState(false);

  const status = channelState.status ?? live?.status;
  // Buổi đã kết thúc mà bản ghi chưa xong: hỏi lại cho tới khi có nút Xem lại (hoặc lý do không có)
  const waitingForReplay = status === "ENDED" && live?.recordingStatus === "STOPPING";
  useEffect(() => {
    setPollMs(status === "CREATED" || waitingForReplay ? CREATED_POLL_MS : 0);
  }, [status, waitingForReplay]);

  // STOMP báo buổi đã kết thúc nhưng `live` còn là bản lúc đang phát (chưa có giờ kết thúc, người xem cao nhất,
  // kết quả Poll): tải lại để màn hình "đã kết thúc" đủ thông tin
  const endedByStomp = channelState.status === "ENDED" && live?.status !== "ENDED";
  useEffect(() => {
    if (endedByStomp) void refetch();
  }, [endedByStomp, refetch]);

  if (isLoading) return <WatchSkeleton />;
  if (isError || !live || !status) {
    return (
      <div className="px-6 py-20 text-center">
        <p className="text-lg font-semibold">Không tìm thấy buổi phát</p>
        <p className="mt-1 text-sm text-gray-500">Buổi phát không tồn tại hoặc đường dẫn đã sai.</p>
        <Link href="/live" className="mt-4 inline-block rounded-full bg-gray-100 px-4 py-2 text-sm font-semibold hover:bg-gray-200">
          Xem các buổi đang phát
        </Link>
      </div>
    );
  }

  const isOwner = user?.sub === live.ownerId;
  const isAdmin = !!user?.roles?.includes("ROLE_ADMIN");
  const viewers = channelState.viewers ?? live.currentViewers;
  const unfinished = status === "CREATED" || status === "LIVE";
  const channelHref = live.channelHandle ? `/@${live.channelHandle}` : `/channel/${live.channelId}`;
  const description = live.description?.trim();
  const chatEnabled = channelState.chatEnabled ?? live.chatEnabled;
  // undefined: chưa nhận tin Poll nào qua STOMP nên dùng Poll trong GET /api/live/{id}
  const poll = channelState.poll === undefined ? live.poll : channelState.poll;

  const onVote = async (option: number) => {
    if (!poll) return;
    if (!user?.sub) {
      toast("Hãy đăng nhập để bình chọn", { action: { label: "Đăng nhập", onClick: login } });
      return;
    }
    try {
      channelState.setMyVote(await votePoll({ id, pollId: poll.id, option }).unwrap());
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không bình chọn được, vui lòng thử lại."));
    }
  };

  const onEnd = async () => {
    try {
      await endLive(id).unwrap();
      toastEmitter.success("Đã kết thúc buổi phát");
      setEndOpen(false);
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không kết thúc được buổi phát, vui lòng thử lại."));
    }
  };

  return (
    <section className="grid grid-cols-1 gap-x-6 px-6 py-6 md:grid-cols-6 xxl:grid-cols-8 xxl:px-20">
      <div className="col-span-1 md:col-span-4 xxl:col-span-6">
        {unfinished ? (
          <LivePlayer id={id} status={status} thumbnailUrl={channelState.thumbnailUrl ?? live.thumbnailUrl} />
        ) : (
          <EndedScreen
            endedAt={live.endedAt}
            startedAt={live.startedAt}
            peakViewers={live.peakViewers}
            replayVideoId={live.replayVideoId}
            recordingStatus={live.recordingStatus}
            recordingMessage={live.recordingMessage}
          />
        )}

        <div className="mt-3 flex flex-wrap items-center gap-2">
          {status === "LIVE" ? <LiveBadge /> : null}
          {status === "LIVE" ? <ViewerCount count={viewers} /> : null}
          {live.startedAt && status === "LIVE" ? (
            <span className="text-sm text-gray-500">Bắt đầu {timeAgo(live.startedAt)}</span>
          ) : null}
        </div>
        <h1 className="mt-1">
          <LinkifiedText text={live.title} seekable={false} className="text-2xl font-bold tracking-tight" />
        </h1>

        <div className="mt-3 flex flex-wrap items-center justify-between gap-x-4 gap-y-3">
          <div className="flex min-w-0 items-center gap-3">
            <Link href={channelHref} className="shrink-0">
              <Image
                src={live.channelPicture || FALLBACK_AVATAR}
                alt={live.channelName ?? "Kênh"}
                width={40}
                height={40}
                className="size-10 rounded-full object-cover"
              />
            </Link>
            <div className="min-w-0">
              <Link href={channelHref} className="block max-w-[14rem] truncate font-bold" title={live.channelName ?? ""}>
                {live.channelName}
              </Link>
              {channel ? (
                <p className="whitespace-nowrap text-xs font-semibold text-gray-500">
                  {formatCompact(channel.totalSubscribers)} người đăng ký
                </p>
              ) : null}
            </div>
            {isOwner ? (
              unfinished ? (
                <Link
                  href="/studio/live"
                  className="shrink-0 whitespace-nowrap rounded-full bg-gray-100 px-3 py-2 text-sm font-bold hover:bg-gray-200"
                >
                  Quản lý buổi phát
                </Link>
              ) : null
            ) : (
              <div className="flex shrink-0 items-center gap-2">
                <SubscribeControl
                  key={live.channelId}
                  channelId={live.channelId}
                  subscribed={channel?.subscribed ?? false}
                  notificationMode={channel?.notificationMode}
                />
                <JoinMembershipButton key={`join-${live.channelId}`} channelId={live.channelId} />
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            <Button variant="secondary" className="rounded-full" onClick={() => setShareOpen(true)}>
              <Share2 className="size-4" />
              Chia sẻ
            </Button>
            {isAdmin && !isOwner && unfinished ? (
              <Button variant="destructive" className="rounded-full" onClick={() => setEndOpen(true)}>
                Kết thúc buổi phát (quản trị)
              </Button>
            ) : null}
          </div>
        </div>

        {!unfinished && live.polls && live.polls.length > 0 ? (
          <div className="mt-4 space-y-3">
            <h2 className="font-semibold">Kết quả Poll</h2>
            {live.polls.map((p) => (
              <LivePollCard key={p.id} poll={p} />
            ))}
          </div>
        ) : null}

        {description ? (
          <div className="mt-4 rounded-xl bg-gray-100 p-3 text-sm">
            <div className={expanded ? "" : "line-clamp-3"}>
              <LinkifiedText text={description} seekable={false} />
            </div>
            <button className="mt-1 font-semibold" onClick={() => setExpanded((e) => !e)}>
              {expanded ? "Ẩn bớt" : "Xem thêm"}
            </button>
          </div>
        ) : null}
      </div>

      {unfinished ? (
        <aside className="col-span-1 mt-6 flex flex-col gap-3 md:col-span-2 md:mt-0">
          {poll ? <LivePollCard poll={poll} onVote={(option) => void onVote(option)} busy={voting} /> : null}
          <LiveChat
            channel={channelState}
            status={status}
            chatEnabled={chatEnabled}
            canModerate={isOwner}
            className="h-[32rem] md:h-[calc(100vh-9rem)]"
          />
        </aside>
      ) : null}

      <ShareDialog link={liveUrl(id)} title={live.title} open={shareOpen} onOpenChange={setShareOpen} />
      <LiveEndDialog open={endOpen} onOpenChange={setEndOpen} onConfirm={() => void onEnd()} loading={ending} admin />
    </section>
  );
}
