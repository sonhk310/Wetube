import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";

export type ReportStatus = "OPEN" | "RESOLVED" | "DISMISSED";

export interface ReportView {
  id: string;
  /** null khi là báo cáo kênh. */
  videoId: string | null;
  videoTitle: string | null;
  thumbnailUrl: string | null;
  videoStatus: string | null;
  /** Có giá trị khi là báo cáo bình luận. */
  commentId: string | null;
  /** null nếu bình luận đã bị xoá. */
  commentContent: string | null;
  commentAuthor: string | null;
  /** Có giá trị khi là báo cáo kênh (khi đó các trường video là null). */
  channelId: string | null;
  /** null nếu kênh không còn. */
  channelName: string | null;
  channelHandle: string | null;
  channelPicture: string | null;
  channelStatus: string | null;
  reason: string;
  details: string | null;
  status: ReportStatus;
  resolutionNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export type ReportAction =
  | "HIDE_VIDEO"
  | "DELETE_COMMENT"
  | "HIDE_CHANNEL"
  | "UNHIDE_VIDEO"
  | "UNHIDE_CHANNEL"
  | "DISMISS";

export type ReportTarget = { videoId: string } | { commentId: string } | { channelId: string };

export const REPORT_REASONS: Record<string, string> = {
  SPAM: "Spam hoặc gây hiểu lầm",
  VIOLENCE: "Bạo lực hoặc nguy hiểm",
  SEXUAL: "Nội dung khiêu dâm",
  HATE: "Thù ghét hoặc quấy rối",
  COPYRIGHT: "Vi phạm bản quyền",
  MISINFORMATION: "Thông tin sai lệch",
  OTHER: "Lý do khác",
};

/** Lý do "Báo cáo người dùng" (kênh), theo YouTube. */
export const CHANNEL_REPORT_REASONS: Record<string, string> = {
  IMPERSONATION: "Mạo danh người khác",
  INAPPROPRIATE_PROFILE: "Ảnh, tên hoặc mô tả kênh không phù hợp",
  SPAM: "Spam hoặc lừa đảo",
  HATE: "Thù ghét hoặc quấy rối",
  OTHER: "Lý do khác",
};

/** Nhãn lý do của một báo cáo (báo cáo kênh dùng bộ lý do riêng). */
export const reportReasonLabel = (r: { channelId: string | null; reason: string }) =>
  (r.channelId ? CHANNEL_REPORT_REASONS : REPORT_REASONS)[r.reason] ?? r.reason;

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
  /** Báo cáo video ({ videoId }), bình luận ({ commentId }) hoặc kênh ({ channelId }). */
  create: async (
    target: ReportTarget,
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
