import { notFound, redirect } from "next/navigation";
import { API_PREFIX } from "@/constants/appConstant";

/**
 * Đích của /@handle (next.config.ts viết lại /@handle thành /at/handle): đổi handle sang id kênh rồi
 * chuyển tới trang kênh. Không cache vì handle có thể được đổi.
 */
export default async function HandleRedirect({ params }: { params: Promise<{ handle: string }> }) {
  const { handle } = await params;
  let channelId: string | null = null;
  try {
    const res = await fetch(`${API_PREFIX}/channels/handle/${encodeURIComponent(handle)}`, { cache: "no-store" });
    if (res.ok) channelId = (await res.json())?.id ?? null;
  } catch {
    // backend không trả lời: coi như không tìm thấy
  }
  if (!channelId) notFound();
  redirect(`/channel/${channelId}`);
}
