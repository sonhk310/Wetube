"use client";

import { useParams, usePathname } from "next/navigation";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { useGetChannelLivesQuery } from "@/app/api/liveApi";
import { useProgressRouter } from "@/hooks/use-progress-router";

const tabsBefore = [
  { label: "Trang chủ", slug: "" },
  { label: "Video", slug: "videos" },
  { label: "Shorts", slug: "shorts" },
];
const liveTab = { label: "Trực tiếp", slug: "live" };
const tabsAfter = [
  { label: "Danh sách phát", slug: "playlists" },
  { label: "Bài đăng", slug: "posts" },
];

export default function ChannelTabs() {
  const pathname = usePathname();
  const router = useProgressRouter();
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  // Cùng tham số với phần đầu trang kênh nên RTK Query gộp thành một yêu cầu
  const { data: lives } = useGetChannelLivesQuery({ channelId: channel?.id ?? "", size: 1 }, { skip: !channel?.id });
  // Kênh chưa từng phát thì không có tab Trực tiếp
  const hasLives = !!lives?.current || (lives?.totalPast ?? 0) > 0;
  const tabs = hasLives ? [...tabsBefore, liveTab, ...tabsAfter] : [...tabsBefore, ...tabsAfter];
  const lastSegment = pathname.split("/").pop();
  // Trang chủ của kênh là mọi đường dẫn không kết thúc bằng tên một thẻ khác
  const currentTab = tabs.some((tab) => tab.slug && tab.slug === lastSegment) ? lastSegment : "";
  // Có handle thì dùng /@handle, chưa có thì dùng id
  const base = channel?.handle ? `/@${channel.handle}` : `/channel/${channelId}`;

  const handleTabClick = (slug: string) => {
    router.push(slug ? `${base}/${slug}` : base);
  };

  return (
    <div className="overflow-x-auto border-b border-gray-300 [scrollbar-width:none]">
      <ul className="flex gap-6 whitespace-nowrap text-sm font-medium text-gray-600">
        {tabs.map((tab) => (
          <li
            key={tab.slug}
            onClick={() => handleTabClick(tab.slug)}
            className={`cursor-pointer border-b-2 pb-2 ${
              currentTab === tab.slug
                ? "border-black font-semibold text-black"
                : "border-transparent hover:text-black"
            }`}
          >
            {tab.label}
          </li>
        ))}
      </ul>
    </div>
  );
}
