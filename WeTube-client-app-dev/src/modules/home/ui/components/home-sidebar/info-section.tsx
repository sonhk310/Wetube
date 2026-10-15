"use client";

import {
    SidebarGroup,
    SidebarGroupContent,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem
} from "@/components/ui/sidebar";
import {pageUrls} from "@/lib/enums/page-urls";
import {Settings, MessageCircleQuestionMark, Flag, MessageSquareWarning} from "lucide-react";
import Link from "next/link";
import {usePathname} from "next/navigation";

const items = [
    {
        title: "Setting",
        url: pageUrls.HISTORY,
        icon: Settings,
    },
    {
        title: "Report diary",
        url: pageUrls.REPORT,
        icon: Flag  ,
    },
    {
        title: "Support",
        url: pageUrls.REPORT,
        icon: MessageCircleQuestionMark  
    },
    {
        title: "Send feedback",
        url: pageUrls.FEEDBACK,
        icon: MessageSquareWarning   ,
    }
];

export const InfoSection = () => {
    const pathname = usePathname();
    return (
        <SidebarGroup>
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
                                    <item.icon/>
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