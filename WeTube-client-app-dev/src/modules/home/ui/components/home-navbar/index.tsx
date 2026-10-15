"use client";

import { SidebarTrigger } from "@/components/ui/sidebar";
import Image from "next/image";
import Link from "next/link";
import SearchInput from "./search-input";
import { Button } from "@/components/ui/button";
import { CreateButton } from "@/modules/home/ui/components/home-navbar/create-button";
import { Bell, Plus, RadioTower, SquarePen, Video } from "lucide-react";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";
import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import AuthButton from "@/modules/auth/ui/components/auth-button";
import { useAuth } from "@/contexts/auth-context";

const HomeNavbar = () => {
  const router = useRouter();
  const { user, isLoading } = useAuth();

  useEffect(() => {
    const fetchIpAndCountry = async () => {
      try {
        const response = await fetch("https://api.ipify.org?format=json");
        const data = await response.json();
        console.log("User IP:", data.ip);
      } catch (error) {
        console.error("Error fetching IP and country:", error);
      }
    };

    void fetchIpAndCountry();
  }, []);
  const handleUploadVideoClick = (closeDropdown: () => void) => {
    closeDropdown();
    router.push("/studio?openUploadModal=true");
  };

  return (
    <nav className="fixed top-0 left-0 right-0 h-16 bg-white flex items-center px-2 pr-5 z-50">
      <div className="flex items-center gap-4 w-full">
        <div className="flex items-center flex-shrink-0">
          <SidebarTrigger />
          <Link
            prefetch
            href="/"
            className="hidden md:block"
          >
            <div className="flex items-center p-4 gap-1">
              <Image
                src="/image/Logo.png"
                alt="Logo"
                width={32}
                height={32}
              />
              <p className="text-xl font-semibold tracking-tight">WeTube</p>
            </div>
          </Link>
        </div>

        <div className="flex-1 flex justify-center max-w-[720px] mx-auto">
          <SearchInput />
        </div>

        <div className="flex-shrink-0 items-center flex gap-4">
          {isLoading ? (
            <div className="flex items-center gap-4">
              <div className="h-10 w-10 bg-gray-200 rounded-full animate-pulse" />
              <div className="h-10 w-24 bg-gray-200 rounded-full animate-pulse" />
            </div>
          ) : (
            <>
              {user && (
                <>
                  <CreateButton triggerIcon={Plus}>
                    {(closeDropdown) => (
                      <>
                        <DropdownMenuItem
                          onSelect={(e) => {
                            e.preventDefault();
                            handleUploadVideoClick(closeDropdown);
                          }}
                          className="cursor-pointer text-base p-3"
                        >
                          <Video className="mr-2 h-5 w-5" />
                          <span>Upload video</span>
                        </DropdownMenuItem>

                        <DropdownMenuItem
                          onSelect={() => closeDropdown()}
                          className="cursor-pointer text-base p-3"
                        >
                          <RadioTower className="mr-2 h-5 w-5" />
                          <span>Live stream</span>
                        </DropdownMenuItem>

                        <DropdownMenuItem
                          onSelect={() => closeDropdown()}
                          className="cursor-pointer text-base p-3"
                        >
                          <SquarePen className="mr-2 h-5 w-5" />
                          <span>Create post</span>
                        </DropdownMenuItem>
                      </>
                    )}
                  </CreateButton>
                  {/* <Button
                    variant="ghost"
                    size="icon"
                    className="rounded-full"
                  >
                    <Bell className="h-6 w-6" />
                  </Button> */}
                  <Button
                    variant="ghost"
                    size="icon"
                    className="rounded-full px-4 py-2 flex items-center gap-2 text-black shadow-md transition-all duration-200"
                    onClick={() => {
                      console.log("Notification button clicked");
                    }}
                  >
                    <Bell className="h-10 w-10" />
                  </Button>
                </>
              )}
              <AuthButton />
            </>
          )}
        </div>
      </div>
    </nav>
  );
};

export default HomeNavbar;
