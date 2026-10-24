import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";

export type ReportStatus = "OPEN" | "RESOLVED" | "DISMISSED";

export interface ReportView {
  id: string;
  videoId: string;
  videoTitle: string;
  thumbnailUrl: string | null;
  videoStatus: string | null;
  /** Có giá trị khi là báo cáo bình luận. */
  commentId: string | null;
  /** null nếu bình luận đã bị xoá. */
  commentContent: string | null;
  commentAuthor: string | null;
  reason: string;
  details: string | null;
  status: ReportStatus;
  resolutionNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export type ReportAction = "HIDE_VIDEO" | "DELETE_COMMENT" | "DISMISS";

export const REPORT_REASONS: Record<string, string> = {
  SPAM: "Spam hoặc gây hiểu lầm",
  VIOLENCE: "Bạo lực hoặc nguy hiểm",
  SEXUAL: "Nội dung khiêu dâm",
  HATE: "Thù ghét hoặc quấy rối",
  COPYRIGHT: "Vi phạm bản quyền",
  MISINFORMATION: "Thông tin sai lệch",
  OTHER: "Lý do khác",
};

export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  OPEN: "Đang chờ xử lý",
  RESOLVED: "Đã xử lý",
  DISMISSED: "Không vi phạm",
};

async function readError(res: Response) {
  try {
    const body = await res.json();
    return body.message || `HTTP ${res.status}`;
  } catch {
    return `HTTP ${res.status}`;
  }
}

export const reportsApi = {
  /** Báo cáo video ({ videoId }) hoặc bình luận ({ commentId }). */
  create: async (
    target: { videoId: string } | { commentId: string },
    reason: string,
    details?: string,
  ): Promise<ReportView> => {
    const res = await apiFetch(`${API_PREFIX}/reports`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...target, reason, details }),
    });
    if (!res.ok) throw new Error(await readError(res));
    return res.json();
  },

  mine: async (): Promise<ReportView[]> => {
    const res = await apiFetch(`${API_PREFIX}/reports/mine`);
    if (!res.ok) throw new Error(await readError(res));
    return res.json();
  },

  list: async (status?: ReportStatus): Promise<ReportView[]> => {
    const query = status ? `?status=${status}` : "";
    const res = await apiFetch(`${API_PREFIX}/reports${query}`);
    if (!res.ok) throw new Error(await readError(res));
    return res.json();
  },

  resolve: async (reportId: string, action: ReportAction): Promise<ReportView> => {
    const res = await apiFetch(`${API_PREFIX}/reports/${reportId}/resolve?action=${action}`, {
      method: "POST",
    });
    if (!res.ok) throw new Error(await readError(res));
    return res.json();
  },
};
