"use client";

import { useAuth } from "@/contexts/auth-context";
import { usePathname, useRouter } from "next/navigation";

const tabs = [
  { label: "Home", slug: "" },
  { label: "Videos", slug: "videos" },
  { label: "Playlists", slug: "playlists" },
  { label: "Posts", slug: "posts" },
];

export default function ChannelTabs() {
  const pathname = usePathname();
  const router = useRouter();
  const { user } = useAuth();

  const channelId = user?.channelId;
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
              (tab.slug === "" && currentTab === "channel") ||
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
