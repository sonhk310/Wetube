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
    title: "History",
    url: pageUrls.HISTORY,
    icon: HistoryIcon,
    auth: true,
  },
  {
    title: "Liked videos",
    url: pageUrls.LIKED_VIDEOS,
    icon: ThumbsUpIcon,
    auth: true,
  },
  {
    title: "All playlists",
    url: pageUrls.ALL_PLAYLISTS,
    icon: ListVideoIcon,
    auth: true,
  },
  {
    title: "Your Video",
    url: pageUrls.YOURVIDEO,
    icon: Youtube,
    auth: true,
  },
  {
    title: "Watch Later",
    url: pageUrls.WATCHLATER,
    icon: Clock3,
    auth: true,
  },
];

export const PersonalSection = () => {
  const pathname = usePathname();
  return (
    <SidebarGroup>
      <SidebarGroupLabel>You</SidebarGroupLabel>
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
