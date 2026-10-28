/** Kiểu dữ liệu phát trực tiếp, khớp DTO của backend (`dto/live`). Giờ là chuỗi ISO UTC. */

export type LiveStatus = "CREATED" | "LIVE" | "ENDED" | "CANCELED";
export type LiveSource = "BROWSER" | "ENCODER";
export type LiveProtocol = "WHIP" | "RTMP";
/** NONE: chưa ghi; RECORDING: đang ghi; STOPPING: chờ trình duyệt tải bản ghi lên; DONE: đã thành video; FAILED / SKIPPED: không có video. */
export type RecordingStatus = "NONE" | "RECORDING" | "STOPPING" | "DONE" | "FAILED" | "SKIPPED";

export interface PollView {
  id: string;
  question: string;
  options: string[];
  counts: number[];
  total: number;
  open: boolean;
  /** Lựa chọn của người đang xem; null nếu chưa chọn hoặc là khách. */
  myVote: number | null;
}

export interface LiveIngress {
  url: string;
  streamKey: string;
}

export interface LiveView {
  id: string;
  channelId: string;
  channelName: string | null;
  channelPicture: string | null;
  channelHandle: string | null;
  ownerId: string;
  title: string;
  description: string | null;
  /** Ảnh thu nhỏ do chủ kênh tải lên; null thì dùng banner / ảnh đại diện kênh. */
  thumbnailUrl: string | null;
  status: LiveStatus;
  source: LiveSource;
  protocol: LiveProtocol | null;
  chatEnabled: boolean;
  /** Chủ kênh đã bật "Ghi hình để xem lại". */
  recordingEnabled: boolean;
  recordingStatus: RecordingStatus;
  /** Lý do không có video xem lại (hết hạn mức, lỗi...); chỉ có khi người gọi là chủ kênh. */
  recordingMessage: string | null;
  /** Có khi bản ghi đã thành video: /watch/{replayVideoId}. */
  replayVideoId: string | null;
  currentViewers: number;
  peakViewers: number;
  createdAt: string;
  startedAt: string | null;
  endedAt: string | null;
  /** Chỉ có khi người gọi là chủ kênh và phát bằng phần mềm. */
  ingress: LiveIngress | null;
  /** Poll đang mở (hoặc vừa đóng trong 60 giây). */
  poll: PollView | null;
  /** Mọi Poll của buổi, chỉ khi đã kết thúc. */
  polls: PollView[] | null;
}

export interface LiveCard {
  id: string;
  title: string;
  channelId: string;
  channelName: string | null;
  channelPicture: string | null;
  channelBackground: string | null;
  thumbnailUrl: string | null;
  viewers: number;
  peakViewers: number;
  startedAt: string | null;
  endedAt: string | null;
  status: LiveStatus;
  /** Ở tab Trực tiếp của kênh: video xem lại đang công khai của buổi (thẻ mở thẳng video). */
  replayVideoId: string | null;
}

export interface ChannelLives {
  current: LiveCard | null;
  past: LiveCard[];
  totalPast: number;
  hasMore: boolean;
}

export interface LiveTokenResponse {
  /** Null khi `full`: buổi đã đủ người xem bằng hình (vẫn chat và bình chọn được). */
  token: string | null;
  wsUrl: string;
  role: "PUBLISHER" | "VIEWER";
  full: boolean;
}

export interface LiveChatMessage {
  id: string;
  userId: string;
  name: string | null;
  avatar: string | null;
  handle: string | null;
  text: string;
  owner: boolean;
  /** Mili-giây epoch (UTC). */
  at: number;
}

export type LiveEventType = "CHAT" | "CHAT_DELETED" | "CHAT_STATE" | "VIEWERS" | "POLL" | "STATUS" | "ERROR";

/** Tin phát qua STOMP; chỉ các trường thuộc `type` đó có giá trị. */
export interface LiveEvent {
  type: LiveEventType;
  viewers?: number;
  status?: LiveStatus;
  message?: LiveChatMessage;
  messageId?: string;
  chatEnabled?: boolean;
  poll?: PollView;
  text?: string;
}

export interface CreateLiveBody {
  title: string;
  description?: string;
  source: LiveSource;
  /** Chỉ dùng với ENCODER; mặc định WHIP. */
  protocol?: LiveProtocol;
  chatEnabled: boolean;
  /** Chỉ buổi phát từ trình duyệt; kèm topicId (chủ đề của video xem lại). */
  recordingEnabled?: boolean;
  topicId?: string;
}

/** Ghi hình còn được bao nhiêu trong tháng (kênh và chung, lấy mức thấp hơn). */
export interface LiveRecordingInfo {
  /** Máy chủ đã bật ghi hình (đã cấu hình kho lưu trữ). */
  enabled: boolean;
  secondsLeft: number;
  /** "CHANNEL" hoặc "TOTAL" khi đã hết; null nếu còn. */
  limitedBy: "CHANNEL" | "TOTAL" | null;
}

/** Một buổi đã phát trong danh sách quản lý ở Studio. STOPPING: còn chờ trình duyệt tải bản ghi lên (chưa xoá được). */
export interface MyPastLive {
  id: string;
  title: string;
  thumbnailUrl: string | null;
  /** Ảnh bìa kênh, dùng khi buổi không có ảnh thu nhỏ (như thẻ công khai). */
  channelBackground: string | null;
  startedAt: string | null;
  endedAt: string | null;
  peakViewers: number;
  recordingEnabled: boolean;
  recordingStatus: RecordingStatus;
  /** Lý do không có video xem lại (hết hạn mức, lỗi...). */
  recordingMessage: string | null;
  /** Có khi bản ghi đã thành video còn tồn tại: /watch/{replayVideoId}. */
  replayVideoId: string | null;
  /** Chế độ hiển thị của video xem lại (mặc định PRIVATE): ACTIVE / PUBLIC / UNLISTED / PRIVATE / INACTIVE. */
  replayStatus: string | null;
}

export interface MyPastLives {
  items: MyPastLive[];
  total: number;
  hasMore: boolean;
}

export interface DeleteLivesBody {
  ids: string[];
  /** Xoá cả video xem lại; không thì video vẫn nằm ở Nội dung. */
  deleteVideos: boolean;
}

export interface DeleteLivesResult {
  deleted: number;
  videosDeleted: number;
}

export interface UpdateLiveBody {
  id: string;
  title?: string;
  description?: string;
  chatEnabled?: boolean;
}
