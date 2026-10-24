import type { Metadata } from "next";
import { API_PREFIX } from "@/constants/appConstant";
import WatchPage from "./watch-page";

/** Thẻ chia sẻ được làm mới tối đa sau chừng này giây (Facebook / Zalo cũng tự cache). */
const METADATA_REVALIDATE_SECONDS = 300;
const DESCRIPTION_MAX = 200;

interface ShareInfo {
  title: string;
  description?: string;
  thumbnailUrl?: string;
  name?: string;
  createAt?: string;
  duration?: number;
}

/**
 * Thumbnail lưu trên Cloudinary là webp 300x200; Facebook cần ảnh lớn (>= 600x315) mới hiện khung
 * lớn và Zalo không chắc đọc được webp, nên xin Cloudinary bản JPG 1200x630.
 */
function shareImage(thumbnailUrl: string): string {
  const match = thumbnailUrl.match(/^(https:\/\/res\.cloudinary\.com\/[^/]+\/image\/upload\/)(?:[a-z]{1,3}_[^/]*\/)?(.+?)(?:\.\w+)?$/);
  if (!match) return thumbnailUrl;
  return `${match[1]}c_fill,w_1200,h_630,q_auto/${match[2]}.jpg`;
}

/** Thông tin video cho thẻ chia sẻ; null nếu video không tồn tại / không công khai với khách. */
async function fetchShareInfo(videoId: string): Promise<ShareInfo | null> {
  try {
    const res = await fetch(`${API_PREFIX}/videos/${encodeURIComponent(videoId)}/detail`, {
      next: { revalidate: METADATA_REVALIDATE_SECONDS },
    });
    if (!res.ok) return null;
    const body = await res.json();
    return body?.detail ?? null;
  } catch {
    return null;
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ videoId: string }>;
}): Promise<Metadata> {
  const { videoId } = await params;
  const video = await fetchShareInfo(videoId);
  if (!video) return { title: "WeTube" };

  const raw = video.description?.trim() || `Video của ${video.name ?? "WeTube"} trên WeTube`;
  const description = raw.length > DESCRIPTION_MAX ? `${raw.slice(0, DESCRIPTION_MAX - 1)}…` : raw;
  const url = `/watch/${videoId}`;
  const image = video.thumbnailUrl ? shareImage(video.thumbnailUrl) : undefined;
  const images = image ? [{ url: image, width: 1200, height: 630, alt: video.title }] : undefined;

  return {
    title: `${video.title} - WeTube`,
    description,
    alternates: { canonical: url },
    openGraph: {
      type: "video.other",
      siteName: "WeTube",
      locale: "vi_VN",
      url,
      title: video.title,
      description,
      images,
      ...(video.createAt ? { releaseDate: video.createAt } : {}),
      ...(video.duration ? { duration: Math.round(video.duration) } : {}),
    },
    twitter: {
      card: image ? "summary_large_image" : "summary",
      title: video.title,
      description,
      images: image ? [image] : undefined,
    },
  };
}

export default function Page() {
  return <WatchPage />;
}
