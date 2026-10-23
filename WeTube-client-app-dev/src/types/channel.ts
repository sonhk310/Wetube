export interface ChannelInfo {
  id: string;
  userId: string | null;
  name: string;
  picture: string | null;
  backgroundImgUrl: string | null;
  description: string | null;
  totalSubscribers: number;
  totalVideos: number;
  totalViews: number;
  createdAt: string;
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
