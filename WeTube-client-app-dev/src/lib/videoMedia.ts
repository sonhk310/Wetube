/** Link Cloudinary của video (…/video/upload/…/id.mp4). */
const isCloudinary = (url: string) => url.includes("res.cloudinary.com") && url.includes("/upload/");

/**
 * Ảnh để hiện cho video: ảnh thu nhỏ nếu có, không thì khung hình đầu do Cloudinary cắt từ video
 * (đổi đuôi .mp4 thành .jpg). Bản nháp chưa có ảnh thu nhỏ nên dùng cách này.
 */
export function videoImageUrl(thumbnailUrl: string | null | undefined, videoUrl: string | null | undefined): string {
  if (thumbnailUrl) return thumbnailUrl;
  if (videoUrl && isCloudinary(videoUrl)) return videoUrl.replace(/\.mp4(\?.*)?$/, ".jpg");
  return "";
}

/** Link tải tệp video về máy (Cloudinary fl_attachment ép trình duyệt tải xuống). */
export function videoDownloadUrl(videoUrl: string): string {
  return isCloudinary(videoUrl) ? videoUrl.replace("/upload/", "/upload/fl_attachment/") : videoUrl;
}
