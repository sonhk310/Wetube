import axios from "axios";
import apiClient from "@/lib/apiClient";

/** Giới hạn của backend (spring.servlet.multipart.max-file-size). */
export const MAX_UPLOAD_BYTES = 100 * 1024 * 1024;

/**
 * Tải tệp lên thành bản nháp (POST /videos/drafts). Dùng axios để có tiến trình thật theo số byte đã gửi
 * (fetch không báo được). Lỗi được đổi thành Error có thông báo tiếng Việt; huỷ bằng signal thì ném lại
 * lỗi huỷ của axios (kiểm tra bằng axios.isCancel).
 */
export async function uploadDraft(
  file: File,
  meta: { duration?: number; width?: number; height?: number },
  onProgress: (percent: number) => void,
  signal: AbortSignal
): Promise<{ id: string }> {
  const formData = new FormData();
  formData.append("videoFile", file);
  if (meta.duration) formData.append("duration", String(meta.duration));
  if (meta.width) formData.append("width", String(meta.width));
  if (meta.height) formData.append("height", String(meta.height));

  try {
    const res = await apiClient.post<{ id: string }>("/videos/drafts", formData, {
      signal,
      onUploadProgress: (e) => {
        if (e.total) onProgress(Math.round((e.loaded / e.total) * 100));
      },
    });
    return { id: res.data.id };
  } catch (err) {
    if (axios.isCancel(err)) throw err;
    if (axios.isAxiosError(err)) {
      const message = (err.response?.data as { message?: string } | undefined)?.message;
      if (message) throw new Error(message);
      if (err.response?.status === 413) throw new Error("Tệp quá lớn, tối đa 100MB.");
      if (!err.response) throw new Error("Mất kết nối khi tải lên, vui lòng thử lại.");
    }
    throw new Error("Tải lên thất bại, vui lòng thử lại.");
  }
}
