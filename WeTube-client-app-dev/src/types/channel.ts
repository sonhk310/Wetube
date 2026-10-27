export interface ChannelInfo {
  id: string;
  userId: string | null;
  name: string;
  /** @handle của kênh, không có "@". */
  handle?: string | null;
  picture: string | null;
  backgroundImgUrl: string | null;
  description: string | null;
  /** Mã quốc gia 2 chữ ("UN" = chưa xác định). */
  countryCode?: string | null;
  totalSubscribers: number;
  totalVideos: number;
  totalViews: number;
  /** INACTIVE: kênh bị admin ẩn (chỉ chủ kênh và admin còn xem được). */
  status?: string | null;
  createdAt: string;
  /** Người xem hiện tại đã đăng ký kênh chưa (false nếu chưa đăng nhập). */
  subscribed?: boolean;
  /** Chế độ thông báo của người xem với kênh; null / không có nếu chưa đăng ký. */
  notificationMode?: NotificationMode | null;
}

export type ChannelVideoSort = "latest" | "popular" | "oldest";

export type NotificationMode = "ALL" | "PERSONALIZE" | "NONE_NOTIFICATION";

export interface ChannelVideo {
  id: string;
  title: string;
  description?: string;
  thumbnailUrl: string;
  videoUrl: string;
  duration: number;
  totalView: number;
  createdAt: string;
  isShort?: boolean;
}
