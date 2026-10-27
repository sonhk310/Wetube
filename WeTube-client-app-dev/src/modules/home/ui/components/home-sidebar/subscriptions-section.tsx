"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronDown, ChevronUp } from "lucide-react";
import {
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
} from "@/components/ui/sidebar";
import { Separator } from "@/components/ui/separator";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { useGetSubscribedChannelsQuery } from "@/app/api/subscriptionsApi";
import { useAuth } from "@/contexts/auth-context";

// Giống YouTube: hiện 7 kênh, còn lại nằm sau "Hiện thêm"
const VISIBLE_COUNT = 7;

export const SubscriptionsSection = () => {
    const pathname = usePathname();
    const { user } = useAuth();
    const { data: channels = [] } = useGetSubscribedChannelsQuery(user?.sub ?? "", { skip: !user?.sub });
    const [expanded, setExpanded] = useState(false);

    if (!user || channels.length === 0) return null;

    const shown = expanded ? channels : channels.slice(0, VISIBLE_COUNT);
    const hidden = channels.length - VISIBLE_COUNT;

    return (
        <>
            {/* Thanh bên thu nhỏ (chỉ còn biểu tượng) không hiện danh sách kênh, giống YouTube */}
            <SidebarGroup className="group-data-[collapsible=icon]:hidden">
                <SidebarGroupLabel>Kênh đăng ký</SidebarGroupLabel>
                <SidebarGroupContent>
                    <SidebarMenu>
                        {shown.map((channel) => {
                            const url = `/channel/${channel.channelId}`;
                            return (
                                <SidebarMenuItem key={channel.channelId}>
                                    <SidebarMenuButton tooltip={channel.name} asChild isActive={pathname === url}>
                                        <Link prefetch href={url} className="flex items-center gap-4">
                                            <Avatar className="size-6">
                                                <AvatarImage src={channel.avatarUrl} alt={channel.name} />
                                                <AvatarFallback className="text-xs">
                                                    {(channel.name ?? "?").slice(0, 1).toUpperCase()}
                                                </AvatarFallback>
                                            </Avatar>
                                            <span className="text-sm truncate">{channel.name}</span>
                                        </Link>
                                    </SidebarMenuButton>
                                </SidebarMenuItem>
                            );
                        })}
                        {hidden > 0 && (
                            <SidebarMenuItem>
                                <SidebarMenuButton onClick={() => setExpanded((v) => !v)} className="flex items-center gap-4">
                                    {expanded ? <ChevronUp /> : <ChevronDown />}
                                    <span className="text-sm">{expanded ? "Ẩn bớt" : `Hiện thêm ${hidden} kênh`}</span>
                                </SidebarMenuButton>
                            </SidebarMenuItem>
                        )}
                    </SidebarMenu>
                </SidebarGroupContent>
            </SidebarGroup>
            <Separator className="group-data-[collapsible=icon]:hidden" />
        </>
    );
};
