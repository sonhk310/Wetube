/** Kiểu dữ liệu phát trực tiếp, khớp DTO của backend (`dto/live`). Giờ là chuỗi ISO UTC. */

export type LiveStatus = "CREATED" | "LIVE" | "ENDED" | "CANCELED";
export type LiveSource = "BROWSER" | "ENCODER";
export type LiveProtocol = "WHIP" | "RTMP";

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
  status: LiveStatus;
  source: LiveSource;
  protocol: LiveProtocol | null;
  chatEnabled: boolean;
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
  viewers: number;
  peakViewers: number;
  startedAt: string | null;
  endedAt: string | null;
  status: LiveStatus;
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
}

export interface UpdateLiveBody {
  id: string;
  title?: string;
  description?: string;
  chatEnabled?: boolean;
}
