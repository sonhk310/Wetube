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
import {useState} from "react";
import { useAuth } from "@/contexts/auth-context";
import RoomModal from "@/modules/rooms/ui/components/room/roomModal";
import {Bounce, toast } from "react-toastify";

const items = [
    { title: "Home", url: pageUrls.HOME, icon: HomeIcon },
    { title: "Subscriptions", url: pageUrls.SUBSCRIPTIONS, icon: PlaySquareIcon, auth: true },
    { title: "Shorts", url: pageUrls.SHORTS, icon: FileVideo },
    { title: "WatchTogether", url: pageUrls.ROOMS, icon: Popcorn },
    { title: "Live", url: pageUrls.LIVESTREAM, icon: Radio },
];
export const MainSection = () => {
    const pathname = usePathname();
    const [openRoomModal, setOpenRoomModal] = useState(false);
    const { user } = useAuth();
    const userId = user?.sub;
    const isTest = true;
    
    const handleRoomClick = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.preventDefault();

        if(isTest){
            setOpenRoomModal(true);
            return;
        }
        
        if (!userId) {
            toast('👀 You need to sign in to use this feature !', {
                position: "top-center",
                autoClose: 5000,
                hideProgressBar: false,
                closeOnClick: false,
                pauseOnHover: true,
                draggable: true,
                theme: "light",
                transition: Bounce,
            });
            return;
        }

        setOpenRoomModal(true);
    };

    return (
        <>
            <SidebarGroup>
                <SidebarGroupContent>
                    <SidebarMenu>
                        {items.map((item, index) => (
                            <SidebarMenuItem key={index}>
                                {item.url === pageUrls.ROOMS ? (
                                    <SidebarMenuButton
                                        tooltip={item.title}
                                        isActive={pathname === item.url}
                                        asChild
                                    >
                                        <button
                                            className="flex items-center gap-4 w-full"
                                            onClick={handleRoomClick}
                                        >
                                            <item.icon/>
                                            <span className="text-sm">{item.title}</span>
                                        </button>
                                    </SidebarMenuButton>
                                ) : (
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
                                            <item.icon/>
                                            <span className="text-sm">{item.title}</span>
                                        </Link>
                                    </SidebarMenuButton>
                                )}
                            </SidebarMenuItem>
                        ))}
                    </SidebarMenu>
                </SidebarGroupContent>
            </SidebarGroup>

            {/* RoomModal */}
            <RoomModal open={openRoomModal} onOpenChange={setOpenRoomModal}/>
        </>
    );
};
