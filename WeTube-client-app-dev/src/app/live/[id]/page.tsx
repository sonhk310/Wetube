import type { Metadata } from "next";
import { API_PREFIX } from "@/constants/appConstant";
import LiveWatchView from "@/modules/live/ui/live-watch-view";

/** Thẻ chia sẻ được làm mới tối đa sau chừng này giây. */
const METADATA_REVALIDATE_SECONDS = 60;
const DESCRIPTION_MAX = 200;

interface ShareInfo {
  title: string;
  description?: string | null;
  channelName?: string | null;
  channelPicture?: string | null;
  thumbnailUrl?: string | null;
  status?: string;
}

/** Thông tin buổi phát cho thẻ chia sẻ; null nếu không tồn tại. */
async function fetchShareInfo(id: string): Promise<ShareInfo | null> {
  try {
    const res = await fetch(`${API_PREFIX}/live/${encodeURIComponent(id)}`, {
      next: { revalidate: METADATA_REVALIDATE_SECONDS },
    });
    if (!res.ok) return null;
    return (await res.json()) as ShareInfo;
  } catch {
    return null;
  }
}

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  const live = await fetchShareInfo(id);
  if (!live) return { title: "WeTube" };

  const raw = live.description?.trim() || `Phát trực tiếp của ${live.channelName ?? "WeTube"} trên WeTube`;
  const description = raw.length > DESCRIPTION_MAX ? `${raw.slice(0, DESCRIPTION_MAX - 1)}…` : raw;
  const url = `/live/${id}`;
  const image = live.thumbnailUrl || live.channelPicture;
  const images = image ? [{ url: image, alt: live.title }] : undefined;

  return {
    title: `${live.title} - WeTube`,
    description,
    alternates: { canonical: url },
    openGraph: { type: "website", siteName: "WeTube", locale: "vi_VN", url, title: live.title, description, images },
    twitter: {
      card: live.thumbnailUrl ? "summary_large_image" : "summary",
      title: live.title,
      description,
      images: image ? [image] : undefined,
    },
  };
}

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <LiveWatchView id={id} />;
}
