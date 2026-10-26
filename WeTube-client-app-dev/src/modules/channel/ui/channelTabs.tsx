"use client";

import { useParams, usePathname } from "next/navigation";
import { useGetChannelInfoQuery } from "@/app/api/channelApi";
import { useProgressRouter } from "@/hooks/use-progress-router";

const tabs = [
  { label: "Trang chủ", slug: "" },
  { label: "Video", slug: "videos" },
  { label: "Danh sách phát", slug: "playlists" },
  { label: "Bài đăng", slug: "posts" },
];

export default function ChannelTabs() {
  const pathname = usePathname();
  const router = useProgressRouter();
  const { channelId } = useParams<{ channelId: string }>();
  const { data: channel } = useGetChannelInfoQuery(channelId);
  const lastSegment = pathname.split("/").pop();
  // Trang chủ của kênh là mọi đường dẫn không kết thúc bằng tên một thẻ khác
  const currentTab = tabs.some((tab) => tab.slug && tab.slug === lastSegment) ? lastSegment : "";
  // Có handle thì dùng /@handle, chưa có thì dùng id
  const base = channel?.handle ? `/@${channel.handle}` : `/channel/${channelId}`;

  const handleTabClick = (slug: string) => {
    router.push(slug ? `${base}/${slug}` : base);
  };

  return (
    <div className="border-b border-gray-300 px-6 pt-0 pb-0">
      <ul className="flex gap-6 text-sm font-medium text-gray-600">
        {tabs.map((tab) => (
          <li
            key={tab.slug}
            onClick={() => handleTabClick(tab.slug)}
            className={`cursor-pointer pb-2 ${
              currentTab === tab.slug
                ? "text-black border-b-2 border-black"
                : ""
            }`}
          >
            {tab.label}
          </li>
        ))}
      </ul>
    </div>
  );
}
