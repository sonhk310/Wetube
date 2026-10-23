"use client";

import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from "@/components/ui/sidebar";
import { pageUrls } from "@/lib/enums/page-urls";
import {
  Clock3,
  HistoryIcon,
  ListVideoIcon,
  ThumbsUpIcon,
  Youtube,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const items = [
  {
    title: "Lịch sử",
    url: pageUrls.HISTORY,
    icon: HistoryIcon,
    auth: true,
  },
  {
    title: "Video đã thích",
    url: pageUrls.LIKED_VIDEOS,
    icon: ThumbsUpIcon,
    auth: true,
  },
  {
    title: "Tất cả danh sách phát",
    url: pageUrls.ALL_PLAYLISTS,
    icon: ListVideoIcon,
    auth: true,
  },
  {
    title: "Video của bạn",
    url: pageUrls.YOURVIDEO,
    icon: Youtube,
    auth: true,
  },
  {
    title: "Xem sau",
    url: pageUrls.WATCHLATER,
    icon: Clock3,
    auth: true,
  },
];

export const PersonalSection = () => {
  const pathname = usePathname();
  return (
    <SidebarGroup>
      <SidebarGroupLabel>Bạn</SidebarGroupLabel>
      <SidebarGroupContent>
        <SidebarMenu>
          {items.map((item, index) => (
            <SidebarMenuItem key={index}>
              <SidebarMenuButton
                tooltip={item.title}
                asChild
                isActive={pathname === item.url}
              >
                <Link
                  prefetch
                  href={item.url}
                  className="flex items-center gap-4"
                >
                  <item.icon />
                  <span className="text-sm">{item.title}</span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </SidebarGroupContent>
    </SidebarGroup>
  );
};
