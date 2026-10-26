/** Vùng cắt tính bằng điểm ảnh của ảnh gốc (react-easy-crop trả về dạng này). */
export interface CropArea {
  x: number;
  y: number;
  width: number;
  height: number;
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new window.Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error("Không đọc được ảnh này, hãy thử ảnh khác (JPG, PNG hoặc WebP)."));
    img.src = url;
  });
}

/**
 * Cắt vùng `area` của ảnh rồi thu về tối đa `maxWidth` điểm ảnh bề ngang (giữ tỉ lệ của vùng cắt), trả về tệp JPEG.
 * Ảnh PNG trong suốt được đặt trên nền trắng vì JPEG không có độ trong suốt.
 */
export async function cropImageToFile(
  imageUrl: string,
  area: CropArea,
  maxWidth: number,
  fileName: string
): Promise<File> {
  const img = await loadImage(imageUrl);
  const outWidth = Math.max(1, Math.round(Math.min(area.width, maxWidth)));
  const outHeight = Math.max(1, Math.round((outWidth * area.height) / area.width));

  const canvas = document.createElement("canvas");
  canvas.width = outWidth;
  canvas.height = outHeight;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Trình duyệt không hỗ trợ cắt ảnh.");
  ctx.fillStyle = "#fff";
  ctx.fillRect(0, 0, outWidth, outHeight);
  ctx.drawImage(img, area.x, area.y, area.width, area.height, 0, 0, outWidth, outHeight);

  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.92));
  if (!blob) throw new Error("Không cắt được ảnh, vui lòng thử lại.");
  return new File([blob], fileName.replace(/\.[^.]+$/, "") + ".jpg", { type: "image/jpeg" });
}
