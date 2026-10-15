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
  createAt: string; // YYYY-MM-DD
  totalView: number;
  name: string; // channel name
  picture: string; // channel avatar
  totalSubscribers: number;
  comments: ApiComment[];
  subscribed: boolean;
  channelId: string;
  subtitles: VideoSubtitleDto[]; // ✅ subtitles
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
  status: "ACTIVE" | "PRIVATE" | "UNLISTED" | "PENDING" | "INACTIVE";
  createdAt: string;
  updatedAt: string;
  tags: Tag[];
  duration: number;
}
