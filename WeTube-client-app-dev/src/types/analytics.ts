/** Số liệu phân tích của kênh (GET /api/studio/analytics). Ngày dạng "yyyy-MM-dd" theo giờ VN. */
export type AnalyticsRange = 7 | 28 | 90;

export interface AnalyticsTotals {
  views: number;
  watchSeconds: number;
  subscribers: number;
  comments: number;
}

export interface AnalyticsDay extends AnalyticsTotals {
  date: string;
}

export interface ChannelAnalytics {
  from: string;
  to: string;
  /** Ngày bắt đầu ghi lượt xem / thời gian xem; trước ngày này không có dữ liệu. */
  trackingSince: string;
  totals: AnalyticsTotals;
  daily: AnalyticsDay[];
}
