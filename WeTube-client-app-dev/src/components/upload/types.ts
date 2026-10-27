export type UploadState =
  /** etaSeconds: thời gian còn lại ước theo tốc độ đã gửi (chưa có ở lúc đầu). */
  | { phase: "uploading"; percent: number; etaSeconds?: number }
  | { phase: "processing" }
  | { phase: "done"; videoId: string }
  | { phase: "error"; message: string };

export type Visibility = "ACTIVE" | "UNLISTED" | "PRIVATE";

export type WizardForm = {
  title: string;
  description: string;
  tags: string;
  /** Từ khóa tìm kiếm, phân cách dấu phẩy (S2); không phải thẻ. */
  searchKeywords: string;
  /** Chủ đề (C8): id trong danh sách chủ đề; "" = chưa chọn. */
  topicId: string;
  thumbnailFile: File | null;
  playlistIds: string[];
  targetLang: string;
  visibility: Visibility;
};

export type StepId = "details" | "elements" | "checks" | "visibility";

export type FormErrors = Partial<Record<"title" | "description" | "tags" | "topicId" | "thumbnailFile", string>>;

export type StepProps = {
  form: WizardForm;
  setForm: (patch: Partial<WizardForm>) => void;
};

/** Kích thước / thời lượng đọc từ tệp trên máy (0 khi chưa đọc được). */
export type VideoMeta = { duration: number; width: number; height: number };
