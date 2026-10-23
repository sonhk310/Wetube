"use client";

import { SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarMenu, SidebarMenuButton, SidebarMenuItem } from "@/components/ui/sidebar";
import { pageUrls } from "@/lib/enums/page-urls";
import { Music, Newspaper , Gamepad2, Trophy   } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const items = [
    {
        title: "Âm nhạc",
        url: pageUrls.MUSIC,
        icon: Music 
    },
    {
        title: "Trò chơi",
        url: pageUrls.LIKED_VIDEOS,
        icon: Gamepad2 
    },
    {
        title: "Tin tức",
        url: pageUrls.ALL_PLAYLISTS,
        icon: Newspaper 
    },
    {
        title: "Thể thao",
        url: pageUrls.SPORTS,
        icon: Trophy  
    },
];

export const ExploreSection = () => {
    const pathname = usePathname();
    return (
        <SidebarGroup>
            <SidebarGroupLabel>Khám phá</SidebarGroupLabel>
            <SidebarGroupContent>
                <SidebarMenu>
                    {items.map((item) => (
                        <SidebarMenuItem key={item.title}>
                            <SidebarMenuButton
                                tooltip={item.title}
                                asChild
                                isActive={pathname === item.url}
                            >
                                <Link prefetch href={item.url} className="flex items-center gap-4">
                                    <item.icon />
                                    <span className="text-sm">{item.title}</span>
                                </Link>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    ))}
                </SidebarMenu>
            </SidebarGroupContent>
        </SidebarGroup>
    )
}