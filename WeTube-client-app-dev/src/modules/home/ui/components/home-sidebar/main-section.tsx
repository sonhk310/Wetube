"use client";

import {
    SidebarGroup,
    SidebarGroupContent,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
} from "@/components/ui/sidebar";
import {pageUrls} from "@/lib/enums/page-urls";
import {FileVideo, HomeIcon, PlaySquareIcon, Popcorn, Radio} from "lucide-react";
import Link from "next/link";
import {usePathname} from "next/navigation";

const items = [
    { title: "Trang chủ", url: pageUrls.HOME, icon: HomeIcon },
    { title: "Kênh đăng ký", url: pageUrls.SUBSCRIPTIONS, icon: PlaySquareIcon, auth: true },
    { title: "Shorts", url: pageUrls.SHORTS, icon: FileVideo },
    { title: "Phòng xem chung", url: pageUrls.ROOMS, icon: Popcorn },
    { title: "Trực tiếp", url: pageUrls.LIVESTREAM, icon: Radio },
];
export const MainSection = () => {
    const pathname = usePathname();

    // Trang Shorts đổi URL thành /shorts/{id} theo Short đang xem; phòng xem chung là /rooms và /rooms/{mã}
    const isActive = (url: string) =>
        pathname === url ||
        ((url === pageUrls.SHORTS || url === pageUrls.ROOMS) && pathname.startsWith(`${url}/`));

    return (
        <SidebarGroup>
            <SidebarGroupContent>
                <SidebarMenu>
                    {items.map((item, index) => (
                        <SidebarMenuItem key={index}>
                            <SidebarMenuButton
                                tooltip={item.title}
                                asChild
                                isActive={isActive(item.url)}
                            >
                                <Link
                                    prefetch
                                    href={item.url}
                                    className="flex items-center gap-4"
                                >
                                    <item.icon/>
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
