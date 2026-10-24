"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useAuth } from "@/contexts/auth-context";
import {
  REPORT_REASONS,
  REPORT_STATUS_LABELS,
  ReportAction,
  ReportStatus,
  ReportView,
  reportsApi,
} from "./reports-api";
import { Skeleton } from "@/components/ui/skeleton";

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

const FILTERS: { label: string; value?: ReportStatus }[] = [
  { label: "Đang chờ", value: "OPEN" },
  { label: "Đã xử lý", value: "RESOLVED" },
  { label: "Không vi phạm", value: "DISMISSED" },
  { label: "Tất cả" },
];

/** Trang quản trị: admin xem báo cáo vi phạm (video, bình luận) và ẩn video / xoá bình luận / bác bỏ. */
export default function AdminReportsView() {
  const { user, isLoading } = useAuth();
  const isAdmin = !!user?.roles?.includes("ROLE_ADMIN");
  const [filter, setFilter] = useState<ReportStatus | undefined>("OPEN");
  const [reports, setReports] = useState<ReportView[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(() => {
    setReports(null);
    setError(null);
    reportsApi.list(filter).then(setReports).catch((err) => setError(err.message));
  }, [filter]);

  useEffect(() => {
    if (isAdmin) load();
  }, [isAdmin, load]);

  const resolve = async (id: string, action: ReportAction) => {
    if (action === "HIDE_VIDEO" && !window.confirm("Ẩn video này với mọi người (trừ chủ video)?")) return;
    if (action === "DELETE_COMMENT" && !window.confirm("Xoá bình luận này (kèm các phản hồi)?")) return;
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
            <div className="relative w-40 aspect-video bg-gray-200 rounded overflow-hidden shrink-0">
              {r.thumbnailUrl && (
                <Image src={r.thumbnailUrl} alt={r.videoTitle} fill sizes="160px" className="object-cover" />
              )}
            </div>
            <div className="flex-1 space-y-1">
              <Link href={`/watch/${r.videoId}`} className="font-semibold text-blue-600 hover:underline">
                {r.videoTitle}
              </Link>
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
                <span className="font-medium">{REPORT_REASONS[r.reason] ?? r.reason}</span>
                {r.details && <span className="text-gray-600"> · {r.details}</span>}
              </p>
              <p className="text-xs text-gray-500">
                {new Date(r.createdAt).toLocaleString("vi-VN")} · {REPORT_STATUS_LABELS[r.status]}
                {r.videoStatus === "INACTIVE" && " · video đang bị ẩn"}
                {r.resolutionNote && ` · ${r.resolutionNote}`}
              </p>
            </div>
            {r.status === "OPEN" && (
              <div className="flex flex-col gap-2 shrink-0">
                {r.commentId ? (
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
          </div>
        ))}
      </div>
    </div>
  );
}
