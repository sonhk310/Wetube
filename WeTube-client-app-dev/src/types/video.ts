import type { NotificationMode } from "./channel";
import { VideoSubtitleDto } from "./videoSubtitleDto";

export interface Author {
  id: string;
  name: string;
  avatarUrl: string;
}

export interface Video {
  id: string;
  title: string;
  description?: string;
  thumbnailUrl?: string;
  videoUrl?: string;
  createdAt: string;
  author?: Author;
  userId?: string;
  views?: number;
}

// ======================= RECOMMEND =======================

export interface RecommendedVideoItem {
  id: string;
  title: string;
  thumbnailUrl: string;
  videoUrl: string;
  totalView: number;
  createAt: string;
  name: string; // Channel name
  duration: number;
  picture: string; // Avatar
  uploadedAgo?: string;
  historyDuration?: number;
  channelId?: string;
  isShort?: boolean;
}

export interface RecommendVideoDto {
  id: string;
  title: string;
  thumbnailUrl: string;
  totalView: number;
  createAt: string;
  name: string;
  duration: number;
  picture: string;
  videoUrl?: string;
  channelId?: string;
  isShort?: boolean;
}

export interface TagDto {
  id: string;
  name: string;
  createdAt: string; // LocalDateTime -> string (ISO format)
  count: number;
}

export interface Tag {
  id: string;
  name: string;
  createdAt: string;
  count: number;
}

export interface RecommendResponseDto {
  video: RecommendVideoDto[];
  tags: TagDto[];
}

export interface RecommendResponse {
  video: RecommendedVideoItem[];
  tags: Tag[];
}

// ======================= VIDEO DETAIL =======================

export interface VideoDetailDto {
  id: string; // UUID
  title: string;
  description: string;
  videoUrl: string;
  thumbnailUrl?: string | null;
  duration?: number | null;
  createAt: string; // ISO, giờ UTC (…Z)
  totalView: number;
  name: string; // channel name
  picture: string; // channel avatar
  totalSubscribers: number;
  comments: ApiComment[];
  subscribed: boolean;
  /** Chế độ thông báo của người xem với kênh; null nếu chưa đăng ký. */
  notificationMode?: NotificationMode | null;
  channelId: string;
  like?: { likeCount: number; liked: boolean | null; disliked?: boolean | null };
  subtitles: VideoSubtitleDto[]; // ✅ subtitles
  /** Giây đã xem lần trước (đã đăng nhập), để phát tiếp. */
  resumeAt?: number | null;
}

export interface VideoDetailResponseDto {
  detail: VideoDetailDto;
  recommend: RecommendResponseDto;
}

// export interface VideoDetail {
//   id: string;
//   title: string;
//   description: string;
//   videoUrl: string;
//   createAt: string;
//   totalView: number;
//   name: string;
//   picture: string;
// }

export interface VideoDetailResponse {
  detail: VideoDetailDto;
  recommend: RecommendResponse;
}

// ======================= COMMENT =======================

export interface ApiUser {
  id: string;
  name: string;
  picture: string;
}

export interface ApiComment {
  id: string;
  content: string;
  user: ApiUser;
  likeCount: number;
  createdAt: string;
  updatedAt: string;
  replyCount: number | null;
  replies: ApiComment[] | null;
}

// ======================= SEARCH =======================

export interface SearchVideoItem {
  id: string;
  title: string;
  description: string;
  thumbnailUrl: string;
  videoUrl: string;
  videosStatus: string;
  duration: number;
  createdAt: string;
  user: User;
}

type User = {
  id: string;
  name: string;
  picture: string;
};

// ======================= SHORTS =======================

export interface Short {
  id: string;
  title: string;
  thumbnail: string;
  videoUrl?: string;
  views: number;
  channel?: {
    id: string;
    name: string;
    avatar?: string;
  };
}

// ======================= RELATED =======================

export interface RelatedVideo {
  id: string;
  title: string;
  thumbnailUrl: string;
  totalView: number;
  createAt: string;
  name: string;
  duration: number;
  picture: string;
}

// ======================= FORM =======================

export interface VideoFormDetail {
  id: string;
  title: string;
  description: string;
  videoUrl: string;
  thumbnailUrl: string;
  status: "ACTIVE" | "PUBLIC" | "PRIVATE" | "UNLISTED" | "PENDING" | "INACTIVE" | "DRAFT";
  createdAt: string;
  updatedAt: string;
  tags: Tag[];
  /** Từ khóa tìm kiếm do chủ video duyệt, phân cách dấu phẩy. */
  searchKeywords?: string | null;
  /** Chủ đề (C8); null với bản nháp / video cũ chưa chọn. */
  topicId?: string | null;
  duration: number;
  isShort?: boolean;
}
