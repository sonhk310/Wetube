"use client";

import {useEffect} from "react";
import Image from "next/image";
import Link from "next/link";
import {usePathname} from "next/navigation";
import {Separator} from "@/components/ui/separator";
import {Sidebar, SidebarContent, SidebarHeader, SidebarTrigger, useSidebar} from "@/components/ui/sidebar";
import {InfoSection} from "./info-section";
import {MainSection} from "./main-section";
import {OtherServices} from "./otherservices-section";
import {PersonalSection} from "./personal-section";
import {SubscriptionsSection} from "./subscriptions-section";

const HomeSidebar = () => {
    const {isMobile, setOpenMobile} = useSidebar();
    const pathname = usePathname();

    // Dạng drawer: chuyển trang thì tự đóng
    useEffect(() => {
        setOpenMobile(false);
    }, [pathname, setOpenMobile]);

    return (
        <Sidebar className="pt-16 z-40 group-data-[side=left]:border-r-0" collapsible="icon">
            {isMobile && (
                // Đầu drawer giống thanh trên cùng: nút đóng + logo
                <SidebarHeader className="h-16 flex-row items-center gap-1 px-2 py-0">
                    <SidebarTrigger/>
                    <Link href="/" className="flex items-center gap-1 p-2">
                        <Image src="/image/Logo.png" alt="Logo" width={32} height={32}/>
                        <span className="text-xl font-semibold tracking-tight">WeTube</span>
                    </Link>
                </SidebarHeader>
            )}
            <SidebarContent className="no-scrollbar bg-background">
                <MainSection/>
                <Separator/>
                <PersonalSection/>
                <Separator/>
                <SubscriptionsSection/>
                <Separator/>
                <OtherServices/>
                <Separator />
                <InfoSection />
            </SidebarContent>
        </Sidebar>
    );
};

export default HomeSidebar;
