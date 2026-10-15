"use client";

import { SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarMenu, SidebarMenuButton, SidebarMenuItem } from "@/components/ui/sidebar";
import { pageUrls } from "@/lib/enums/page-urls";
import { Crown, ListMusic , Clapperboard, CircleUserRound   } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const items = [
    {
        title: "WeTube Premium",
        url: pageUrls.PREMIUM,
        icon: Crown ,
    },
    {
        title: "WeTube Studio",
        url: "/otherservices/studio",
        icon: Clapperboard ,
    },
    {
        title: "WeTube Music",
        url:pageUrls.MUSIC,
        icon: ListMusic 
    },
    {
        title: "WeTube Kids",
        url: "/otherservices/kids",
        icon: CircleUserRound  ,
        auth: true
    }
];

export const OtherServices = () => {
    const pathname = usePathname();
    return (
        <SidebarGroup>
            <SidebarGroupLabel>Other services of WeTube</SidebarGroupLabel>
            <SidebarGroupContent>
                <SidebarMenu>
                    {items.map((item, index) => (
                        <SidebarMenuItem key={index}>
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