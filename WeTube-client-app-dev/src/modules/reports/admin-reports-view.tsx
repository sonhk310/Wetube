"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useAuth } from "@/contexts/auth-context";
import {
  REPORT_STATUS_LABELS,
  reportReasonLabel,
  ReportAction,
  ReportStatus,
  ReportView,
  reportsApi,
} from "./reports-api";
import { Skeleton } from "@/components/ui/skeleton";
import ChannelQuickView from "./channel-quick-view";
import { confirmDialog } from "@/lib/dialogs";

/** Khung chờ cùng hình với thẻ báo cáo: ảnh video bên trái, vài dòng chữ bên phải. */
function ReportListSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="flex gap-4 border rounded-lg p-3">
          <Skeleton className="w-40 aspect-video rounded shrink-0" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-5 w-2/3" />
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        </div>
      ))}
    </div>
  );
}

/**
 * Căn cứ để admin xét báo cáo: bao nhiêu người đã báo cáo cùng đối tượng (theo lý do), và người báo cáo này
 * trước đây báo cáo đúng / sai bao nhiêu lần.
 */
function ReportEvidence({ report }: { readonly report: ReportView }) {
  const info = report.admin!;
  const reasons = Object.entries(info.targetReasonCounts)
    .map(([reason, n]) => `${reportReasonLabel({ channelId: report.channelId, reason })} ${n}`)
    .join(" · ");
  const past = info.reporterConfirmed + info.reporterDismissed;
  return (
    <div className="rounded-md bg-gray-50 px-2 py-1.5 text-xs text-gray-700 space-y-0.5">
      <p>
        <span className="font-semibold">{info.targetReporters} người đã báo cáo</span>
        {reasons && <span className="text-gray-500"> ({reasons})</span>}
      </p>
      <p>
        Người báo cáo: <span className="font-medium">{info.reporterName ?? "(không rõ)"}</span>
        {" · "}
        {past === 0 ? (
          <span className="text-gray-500">chưa có báo cáo nào được xét trước đây</span>
        ) : (
          <>
            báo cáo trước: <span className="text-green-700">{info.reporterConfirmed} đúng</span>,{" "}
            <span className={info.reporterDismissed > info.reporterConfirmed ? "text-red-600 font-semibold" : "text-red-600"}>
              {info.reporterDismissed} sai
            </span>
          </>
        )}
      </p>
    </div>
  );
}

const FILTERS: { label: string; value?: ReportStatus }[] = [
  { label: "Đang chờ", value: "OPEN" },
  { label: "Đã xử lý", value: "RESOLVED" },
  { label: "Không vi phạm", value: "DISMISSED" },
  { label: "Tất cả" },
];

/** Hành động cần xác nhận trước khi thực hiện (DISMISS không cần). */
const CONFIRM_BEFORE: Partial<Record<ReportAction, Parameters<typeof confirmDialog>[0]>> = {
  HIDE_VIDEO: {
    title: "Ẩn video này?",
    message: "Video sẽ ẩn với mọi người (trừ chủ video).",
    confirmLabel: "Ẩn video",
    destructive: true,
  },
  DELETE_COMMENT: {
    title: "Xoá bình luận này?",
    message: "Các phản hồi của bình luận cũng bị xoá.",
    confirmLabel: "Xoá bình luận",
    destructive: true,
  },
  HIDE_CHANNEL: {
    title: "Ẩn kênh này?",
    message: "Kênh và mọi video đang xem được của kênh sẽ ẩn với mọi người (trừ chủ kênh).",
    confirmLabel: "Ẩn kênh",
    destructive: true,
  },
  UNHIDE_CHANNEL: {
    title: "Bỏ ẩn kênh này?",
    message: "Các video bị ẩn cùng kênh sẽ trở lại trạng thái trước khi bị ẩn.",
    confirmLabel: "Bỏ ẩn kênh",
  },
  UNHIDE_VIDEO: {
    title: "Bỏ ẩn video này?",
    confirmLabel: "Bỏ ẩn video",
  },
};

/**
 * Trang quản trị: admin xem báo cáo vi phạm (video, bình luận, kênh) và ẩn video / xoá bình luận / ẩn kênh /
 * bác bỏ; video hoặc kênh đang bị ẩn thì có nút bỏ ẩn.
 */
export default function AdminReportsView() {
  const { user, isLoading } = useAuth();
  const isAdmin = !!user?.roles?.includes("ROLE_ADMIN");
  const [filter, setFilter] = useState<ReportStatus | undefined>("OPEN");
  const [reports, setReports] = useState<ReportView[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [quickViewIds, setQuickViewIds] = useState<Set<string>>(new Set());

  const toggleQuickView = (id: string) =>
    setQuickViewIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const load = useCallback(() => {
    setReports(null);
    setError(null);
    reportsApi.list(filter).then(setReports).catch((err) => setError(err.message));
  }, [filter]);

  useEffect(() => {
    if (isAdmin) load();
  }, [isAdmin, load]);

  const resolve = async (id: string, action: ReportAction) => {
    const question = CONFIRM_BEFORE[action];
    if (question && !(await confirmDialog(question))) return;
    setBusyId(id);
    try {
      await reportsApi.resolve(id, action);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Thao tác thất bại");
    } finally {
      setBusyId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="p-6">
        <ReportListSkeleton />
      </div>
    );
  }
  if (!isAdmin) return <p className="p-6">Trang này chỉ dành cho quản trị viên.</p>;

  return (
    <div className="p-6 space-y-4 text-sm">
      <h1 className="text-2xl font-bold">Quản lý báo cáo vi phạm</h1>

      <div className="flex gap-2 flex-wrap">
        {FILTERS.map((f) => (
          <button
            key={f.label}
            onClick={() => setFilter(f.value)}
            className={`px-3 py-1 rounded-full border ${
              filter === f.value ? "bg-black text-white" : "bg-white hover:bg-gray-100"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <p className="text-red-500">{error}</p>}
      {reports === null && !error && <ReportListSkeleton />}
      {reports && reports.length === 0 && <p className="text-gray-500">Không có báo cáo nào.</p>}

      <div className="space-y-3">
        {reports?.map((r) => (
          <div key={r.id} className="flex gap-4 border rounded-lg p-3 items-start">
            {r.channelId ? (
              <div className="w-40 flex justify-center shrink-0">
                <div className="relative size-20 bg-gray-200 rounded-full overflow-hidden">
                  {r.channelPicture && (
                    <Image src={r.channelPicture} alt={r.channelName ?? ""} fill sizes="80px" className="object-cover" />
                  )}
                </div>
              </div>
            ) : (
              <div className="relative w-40 aspect-video bg-gray-200 rounded overflow-hidden shrink-0">
                {r.thumbnailUrl && (
                  <Image src={r.thumbnailUrl} alt={r.videoTitle ?? ""} fill sizes="160px" className="object-cover" />
                )}
              </div>
            )}
            <div className="flex-1 space-y-1">
              {r.channelId ? (
                <p>
                  <span className="text-xs font-semibold">Kênh: </span>
                  <Link href={`/channel/${r.channelId}`} className="font-semibold text-blue-600 hover:underline">
                    {r.channelName ?? "(kênh không còn)"}
                  </Link>
                  {r.channelHandle && <span className="text-gray-500"> @{r.channelHandle}</span>}
                </p>
              ) : (
                <Link href={`/watch/${r.videoId}`} className="font-semibold text-blue-600 hover:underline">
                  {r.videoTitle}
                </Link>
              )}
              {r.commentId && (
                <p className="border-l-2 border-gray-300 pl-2 text-gray-700">
                  {r.commentContent !== null ? (
                    <>
                      <span className="text-xs font-semibold">Bình luận của {r.commentAuthor ?? "Ẩn danh"}: </span>
                      <span className="whitespace-pre-wrap break-words">{r.commentContent}</span>
                    </>
                  ) : (
                    <span className="text-xs italic">(bình luận đã bị xoá)</span>
                  )}
                </p>
              )}
              <p>
                <span className="font-medium">{reportReasonLabel(r)}</span>
                {r.details && <span className="text-gray-600"> · {r.details}</span>}
              </p>
              <p className="text-xs text-gray-500">
                {new Date(r.createdAt).toLocaleString("vi-VN")} · {REPORT_STATUS_LABELS[r.status]}
                {r.videoStatus === "INACTIVE" && " · video đang bị ẩn"}
                {r.channelStatus === "INACTIVE" && " · kênh đang bị ẩn"}
                {r.resolutionNote && ` · ${r.resolutionNote}`}
              </p>
              {r.admin && <ReportEvidence report={r} />}
              {r.channelId && (
                <button
                  onClick={() => toggleQuickView(r.id)}
                  className="text-xs font-medium text-blue-600 hover:underline cursor-pointer"
                >
                  {quickViewIds.has(r.id) ? "Ẩn xem nhanh" : "Xem nhanh kênh"}
                </button>
              )}
              {r.channelId && quickViewIds.has(r.id) && <ChannelQuickView channelId={r.channelId} />}
            </div>
            {r.status === "OPEN" && (
              <div className="flex flex-col gap-2 shrink-0">
                {r.channelId ? (
                  <button
                    onClick={() => resolve(r.id, "HIDE_CHANNEL")}
                    disabled={busyId === r.id}
                    className="px-3 py-1 rounded-md bg-red-600 text-white disabled:opacity-60"
                  >
                    Ẩn kênh
                  </button>
                ) : r.commentId ? (
                  <button
                    onClick={() => resolve(r.id, "DELETE_COMMENT")}
                    disabled={busyId === r.id}
                    className="px-3 py-1 rounded-md bg-red-600 text-white disabled:opacity-60"
                  >
                    Xoá bình luận
                  </button>
                ) : (
                  <button
                    onClick={() => resolve(r.id, "HIDE_VIDEO")}
                    disabled={busyId === r.id}
                    className="px-3 py-1 rounded-md bg-red-600 text-white disabled:opacity-60"
                  >
                    Ẩn video
                  </button>
                )}
                <button
                  onClick={() => resolve(r.id, "DISMISS")}
                  disabled={busyId === r.id}
                  className="px-3 py-1 rounded-md border disabled:opacity-60"
                >
                  Không vi phạm
                </button>
              </div>
            )}
            {(r.channelId ? r.channelStatus === "INACTIVE" : !r.commentId && r.videoStatus === "INACTIVE") && (
              <button
                onClick={() => resolve(r.id, r.channelId ? "UNHIDE_CHANNEL" : "UNHIDE_VIDEO")}
                disabled={busyId === r.id}
                className="px-3 py-1 rounded-md border shrink-0 disabled:opacity-60"
              >
                {r.channelId ? "Bỏ ẩn kênh" : "Bỏ ẩn video"}
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
