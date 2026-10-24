"use client";

import React, { useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/contexts/auth-context";
import {
  REPORT_REASONS,
  REPORT_STATUS_LABELS,
  ReportView,
  reportsApi,
} from "@/modules/reports/reports-api";

const STATUS_COLORS: Record<string, string> = {
  OPEN: "text-amber-600",
  RESOLVED: "text-green-600",
  DISMISSED: "text-gray-500",
};

const ReportStatus: React.FC = () => {
  const { user, isLoading } = useAuth();
  const [reports, setReports] = useState<ReportView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user?.sub) return;
    reportsApi
      .mine()
      .then(setReports)
      .catch((err) => setError(err.message));
  }, [user?.sub]);

  return (
    <div className="bg-white text-black min-h-screen px-6 py-10 space-y-8 text-sm text-left">
      <div>
        <h1 className="text-2xl font-bold mb-2">Nhật ký báo cáo</h1>
        <p className="max-w-3xl">
          Bất kỳ thành viên nào của cộng đồng WeTube cũng có thể báo cáo nội dung mà họ cho là vi phạm
          nguyên tắc cộng đồng. Nội dung bị báo cáo không tự động bị gỡ: quản trị viên sẽ xem xét và ẩn
          video nếu vi phạm. Bạn theo dõi kết quả các báo cáo của mình tại đây.
        </p>
      </div>

      {!isLoading && !user && <p>Vui lòng đăng nhập để xem nhật ký báo cáo.</p>}
      {error && <p className="text-red-500">Không tải được báo cáo: {error}</p>}
      {user && !error && reports === null && <p>Đang tải...</p>}
      {reports && reports.length === 0 && <p className="text-gray-500">Bạn chưa báo cáo video nào.</p>}

      {reports && reports.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left border-t border-gray-300">
            <thead className="uppercase">
              <tr>
                <th className="py-3 px-4">Video</th>
                <th className="py-3 px-4">Lý do</th>
                <th className="py-3 px-4">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id} className="border-t border-gray-200 align-top">
                  <td className="py-3 px-4">
                    <Link href={`/watch/${r.videoId}`} className="text-blue-600 hover:underline">
                      {r.videoTitle}
                    </Link>
                    {r.commentId && (
                      <div className="text-xs text-gray-600 mt-1 break-words">
                        Bình luận: {r.commentContent ?? "(đã bị xoá)"}
                      </div>
                    )}
                  </td>
                  <td className="py-3 px-4">
                    {REPORT_REASONS[r.reason] ?? r.reason}
                    {r.details && <div className="text-xs text-gray-600 mt-1">{r.details}</div>}
                    <div className="text-xs text-gray-500 mt-1">
                      {new Date(r.createdAt).toLocaleDateString("vi-VN")}
                    </div>
                  </td>
                  <td className={`py-3 px-4 ${STATUS_COLORS[r.status]}`}>
                    {REPORT_STATUS_LABELS[r.status]}
                    {r.resolutionNote && (
                      <div className="text-xs text-gray-600 mt-1">{r.resolutionNote}</div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default ReportStatus;
