export interface ChannelInfo {
  id: string;
  userId: string | null;
  name: string;
  /** @handle của kênh, không có "@". */
  handle?: string | null;
  picture: string | null;
  backgroundImgUrl: string | null;
  description: string | null;
  totalSubscribers: number;
  totalVideos: number;
  totalViews: number;
  createdAt: string;
  /** Người xem hiện tại đã đăng ký kênh chưa (false nếu chưa đăng nhập). */
  subscribed?: boolean;
}

export interface ChannelVideo {
  id: string;
  title: string;
  description?: string;
  thumbnailUrl: string;
  videoUrl: string;
  duration: number;
  totalView: number;
  createdAt: string;
}
