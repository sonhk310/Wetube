export type UploadState =
  | { phase: "uploading"; percent: number }
  | { phase: "processing" }
  | { phase: "done"; videoId: string }
  | { phase: "error"; message: string };

export type Visibility = "ACTIVE" | "UNLISTED" | "PRIVATE";

export type WizardForm = {
  title: string;
  description: string;
  tags: string;
  thumbnailFile: File | null;
  playlistIds: string[];
  targetLang: string;
  visibility: Visibility;
};

export type StepId = "details" | "elements" | "checks" | "visibility";

export type FormErrors = Partial<Record<"title" | "description" | "tags" | "thumbnailFile", string>>;

export type StepProps = {
  form: WizardForm;
  setForm: (patch: Partial<WizardForm>) => void;
};

/** Kích thước / thời lượng đọc từ tệp trên máy (0 khi chưa đọc được). */
export type VideoMeta = { duration: number; width: number; height: number };
