"use client";

import { SidebarTrigger } from "@/components/ui/sidebar";
import { pageUrls } from "@/lib/enums/page-urls";
import Image from "next/image";
import Link from "next/link";
import {
  Bell,
  ListPlus,
  RadioTower,
  SquarePen,
  Upload,
  Video,
} from "lucide-react";
import { CreateButton } from "@/modules/home/ui/components/home-navbar/create-button";
import React from "react";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { VideoUploadModal } from "@/components/video-upload-modal";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";

export default function StudioNavbar() {
  // const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

  const searchParams = useSearchParams();
  const router = useRouter();

  const isUploadModalOpen = searchParams.get("openUploadModal") === "true";

  const updateQueryParam = (key: string, value: string | null) => {
    const currentPath = window.location.pathname;
    const newSearchParams = new URLSearchParams(searchParams.toString());

    if (value === null) {
      newSearchParams.delete(key);
    } else {
      newSearchParams.set(key, value);
    }

    const newUrl = newSearchParams.toString()
      ? `${currentPath}?${newSearchParams.toString()}`
      : currentPath;

    router.replace(newUrl, { scroll: false });
  };

  return (
    <nav className="fixed top-0 left-0 right-0 h-16 bg-white flex items-center px-2 pr-5 z-50 border-b shadow-sm">
      <div className="flex items-center gap-4 w-full">
        <div className="flex items-center flex-shrink-0">
          <SidebarTrigger />
          <Link
            prefetch
            href={pageUrls.STUDIO}
            className="hidden md:block"
          >
            <div className="flex items-center p-4 gap-1">
              <Image
                src="/image/Logo.png"
                alt="Logo"
                width={32}
                height={32}
              />
              <p className="text-xl font-semibold tracking-tight">Studio</p>
            </div>
          </Link>
        </div>

        <div className="flex-1"></div>

        <div className="flex-shrink-0 items-center flex gap-4">
          <Dialog
            open={isUploadModalOpen}
            // onOpenChange={handleDialogChange}
            onOpenChange={(open) => {
              updateQueryParam("openUploadModal", open ? "true" : null);
            }}
          >
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

            <CreateButton
              triggerIcon={Video}
              align="end"
            >
              {(closeDropdown) => (
                <>
                  <DropdownMenuItem
                    onSelect={(e) => {
                      e.preventDefault();
                      //   setIsUploadModalOpen(true);
                      //   handleUploadVideoClick(closeDropdown);
                      closeDropdown();
                      updateQueryParam("openUploadModal", "true"); // Rồi mới mở modal
                    }}
                    className="cursor-pointer text-base p-3"
                  >
                    <Upload className="mr-2 h-5 w-5" />
                    <span>Upload video</span>
                  </DropdownMenuItem>

                  <DropdownMenuItem
                    onSelect={() => {
                      closeDropdown();
                    }}
                    className="cursor-pointer text-base p-3"
                  >
                    <RadioTower className="mr-2 h-5 w-5" />
                    <span>Live stream</span>
                  </DropdownMenuItem>

                  <DropdownMenuItem
                    onSelect={() => {
                      closeDropdown();
                    }}
                    className="cursor-pointer text-base p-3"
                  >
                    <SquarePen className="mr-2 h-5 w-5" />
                    <span>Create post</span>
                  </DropdownMenuItem>

                  <DropdownMenuItem
                    onSelect={() => {
                      closeDropdown();
                    }}
                    className="cursor-pointer text-base p-3"
                  >
                    <ListPlus className="mr-2 h-5 w-5" />
                    <span>Add new playlist</span>
                  </DropdownMenuItem>
                </>
              )}
            </CreateButton>
            <DialogContent
              showCloseButton={false}
              //   className="bg-white rounded-4xl border-gray-200 text-black sm:max-w-4xl p-0"
              className="bg-white rounded-lg p-0 sm:max-w-4xl h-[90vh]"
              onPointerDownOutside={(e) => e.preventDefault()}
              onEscapeKeyDown={(e) => e.preventDefault()}
            >
              <VideoUploadModal
                isOpen={isUploadModalOpen}
                onClose={() => updateQueryParam("openUploadModal", null)}
              />
            </DialogContent>
          </Dialog>
        </div>
      </div>
    </nav>
  );
}
