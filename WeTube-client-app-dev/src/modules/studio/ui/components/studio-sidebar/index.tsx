"use client";

import {
  Sidebar,
  SidebarContent,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenu,
  SidebarGroup,
} from "@/components/ui/sidebar";
import { Separator } from "@/components/ui/separator";
import { StudioSidebarHeader } from "./studio-sidebar-header";
import { pageUrls } from "@/lib/enums/page-urls";

import { LogOutIcon, VideoIcon, SlidersHorizontal } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

export default function StudioSidebar() {
  const pathname = usePathname();

  return (
    <Sidebar
      className="pt-16 z-40 shadow-md"
      collapsible="icon"
    >
      <SidebarContent className="bg-background">
        <SidebarGroup>
          <SidebarMenu>
            <StudioSidebarHeader />
            <SidebarMenuItem>
              <SidebarMenuButton
                isActive={pathname === pageUrls.STUDIO}
                tooltip="Nội dung"
                asChild
              >
                <Link
                  prefetch
                  href={pageUrls.STUDIO}
                >
                  <VideoIcon className="size-5" />
                  <span className="text-sm">Nội dung</span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <SidebarMenuButton
                isActive={pathname === pageUrls.CUSTOMIZATION}
                tooltip="Tuỳ chỉnh"
                asChild
              >
                <Link
                  prefetch
                  href={pageUrls.CUSTOMIZATION}
                >
                  <SlidersHorizontal className="size-5" />
                  <span className="text-sm">Tuỳ chỉnh</span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <Separator />
            <SidebarMenuItem>
              <SidebarMenuButton
                tooltip="Thoát studio"
                asChild
              >
                <Link
                  prefetch
                  href={pageUrls.HOME}
                >
                  <LogOutIcon className="size-5" />
                  <span className="text-sm">Thoát studio</span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>
      </SidebarContent>
    </Sidebar>
  );
}
