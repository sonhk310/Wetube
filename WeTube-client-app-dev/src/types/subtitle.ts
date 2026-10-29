/** Phụ đề như chủ video thấy (E2): AI tạo → bản nháp → công khai. */
export type SubtitleStatus = "PROCESSING" | "DRAFT" | "PUBLISHED" | "FAILED";

/** Một câu phụ đề; giờ tính bằng giây. original: câu gốc AI nghe được, để đối chiếu bản dịch. */
export interface SubtitleCue {
  start: number;
  end: number;
  text: string;
  original?: string | null;
}

export interface ManagedSubtitle {
  id: string;
  language: string;
  status: SubtitleStatus;
  aiGenerated: boolean;
  errorMessage: string | null;
  subtitleUrl: string | null;
  cueCount: number;
  updatedAt: string;
  /** Chỉ có khi lấy một phụ đề để sửa; null với phụ đề cũ chỉ có file .vtt. */
  cues: SubtitleCue[] | null;
}
