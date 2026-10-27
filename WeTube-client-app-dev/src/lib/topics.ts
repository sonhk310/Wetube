import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";

export interface Topic {
  id: string;
  name: string;
}

/** chip: tên ngắn hiện trên chip trang chủ. */
export interface TopicGroup {
  id: string;
  name: string;
  chip: string;
  topics: Topic[];
}

let cached: Promise<TopicGroup[]> | null = null;

/** Danh sách chủ đề cố định 2 tầng (C8): tải một lần; lỗi thì lần gọi sau tải lại. */
export function loadTopics(): Promise<TopicGroup[]> {
  cached ??= apiFetch(`${API_PREFIX}/topics`)
    .then((res) => {
      if (!res.ok) throw new Error("Không tải được danh sách chủ đề");
      return res.json() as Promise<TopicGroup[]>;
    })
    .catch((err) => {
      cached = null;
      throw err;
    });
  return cached;
}

/** "music.lo-fi" → "music"; id nhóm giữ nguyên. */
export const groupIdOf = (topicId: string) => topicId.split(".")[0];
