"use client";

import { useParams, usePathname } from "next/navigation";
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
  const currentTab = pathname.split("/").pop();

  const handleTabClick = (slug: string) => {
    router.push(`/channel/${channelId}/${slug}`);
  };

  return (
    <div className="border-b border-gray-300 px-6 pt-0 pb-0">
      <ul className="flex gap-6 text-sm font-medium text-gray-600">
        {tabs.map((tab) => (
          <li
            key={tab.slug}
            onClick={() => handleTabClick(tab.slug)}
            className={`cursor-pointer pb-2 ${
              (tab.slug === "" && currentTab === channelId) ||
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
