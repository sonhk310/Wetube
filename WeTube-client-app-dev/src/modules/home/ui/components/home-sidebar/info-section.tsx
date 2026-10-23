"use client";

import {
    SidebarGroup,
    SidebarGroupContent,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem
} from "@/components/ui/sidebar";
import {pageUrls} from "@/lib/enums/page-urls";
import {Settings, MessageCircleQuestionMark, Flag, MessageSquareWarning, ShieldCheck} from "lucide-react";
import {useAuth} from "@/contexts/auth-context";
import Link from "next/link";
import {usePathname} from "next/navigation";

const items = [
    {
        title: "Cài đặt",
        url: pageUrls.HISTORY,
        icon: Settings,
    },
    {
        title: "Nhật ký báo cáo",
        url: pageUrls.REPORT,
        icon: Flag  ,
    },
    {
        title: "Hỗ trợ",
        url: pageUrls.REPORT,
        icon: MessageCircleQuestionMark  
    },
    {
        title: "Gửi phản hồi",
        url: pageUrls.FEEDBACK,
        icon: MessageSquareWarning   ,
    }
];

const adminItems = [
    {
        title: "Quản lý báo cáo",
        url: "/admin/reports",
        icon: ShieldCheck,
    },
];

export const InfoSection = () => {
    const pathname = usePathname();
    const {user} = useAuth();
    const visibleItems = user?.roles?.includes("ROLE_ADMIN") ? [...adminItems, ...items] : items;
    return (
        <SidebarGroup>
            <SidebarGroupContent>
                <SidebarMenu>
                    {visibleItems.map((item) => (
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