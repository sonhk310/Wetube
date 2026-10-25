/** Một video trong GET /api/playlists/detail/{id}. */
export interface PlaylistVideoItem {
  videoId: string;
  videoTitle: string;
  channelName: string | null;
  channelId: string | null;
  thumbnailUrl: string | null;
  duration: number;
  isShort: boolean | null;
  totalView: number;
  /** Ngày xuất bản của video. */
  createdAt: string;
  /** Ngày thêm vào danh sách. */
  addedAt: string | null;
}

/** GET /api/playlists/detail/{id}; video theo "Ngày thêm (mới nhất)". */
export interface PlaylistDetail {
  playlistId: string;
  playlistTitle: string;
  playlistType: string;
  privacy: string;
  ownerId: string;
  ownerName: string | null;
  totalVideos: number;
  hiddenCount: number;
  createdAt: string;
  lastUpdatedAt: string;
  videos: PlaylistVideoItem[];
}

export const SYSTEM_PLAYLIST_TITLES: Record<string, string> = {
  WATCH_LATER: "Xem sau",
  LIKED: "Video đã thích",
  HISTORY: "Lịch sử xem",
  USER_UPLOADED: "Video đã tải lên",
};

/** Tên hiển thị: danh sách hệ thống dùng tên tiếng Việt cố định. */
export function playlistDisplayTitle(type: string, title: string): string {
  return SYSTEM_PLAYLIST_TITLES[type] ?? title;
}

/** Chỉ danh sách tự tạo đổi tên / chế độ / xoá được. */
export function isEditablePlaylist(type: string): boolean {
  return type === "USER_PLAYLIST";
}

/** "Xoá khỏi danh sách": danh sách tự tạo và Xem sau (không có ở Đã thích, Video đã tải lên, Lịch sử). */
export function canRemoveFromPlaylist(type: string): boolean {
  return type === "USER_PLAYLIST" || type === "WATCH_LATER";
}

export function isPrivatePlaylist(privacy: string | null | undefined): boolean {
  return privacy?.toLowerCase() === "private";
}
