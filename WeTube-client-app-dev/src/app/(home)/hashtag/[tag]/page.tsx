import type { Metadata } from "next";
import TagVideoGrid from "@/modules/home/ui/components/video/tagVideoGrid";

type Props = { params: Promise<{ tag: string }> };

function decodeTag(raw: string): string {
  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { tag } = await params;
  return { title: `#${decodeTag(tag)} - WeTube` };
}

/** Video công khai gắn một #thẻ (sắp theo lượt xem), mở từ #thẻ trong tiêu đề / mô tả hoặc chip thẻ ở trang xem. */
export default async function HashtagPage({ params }: Props) {
  const tag = decodeTag((await params).tag);
  return (
    <div>
      <h1 className="px-6 pt-4 text-2xl font-bold">#{tag}</h1>
      <TagVideoGrid tag={tag} />
    </div>
  );
}
