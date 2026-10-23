"use client";

import React, {useState} from 'react';
import {Button} from "@/components/ui/button";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

type DropdownAlign = "start" | "center" | "end";

interface CreateButtonProps {
    triggerIcon: React.ElementType;
    children: React.ReactNode | ((close: () => void) => React.ReactNode);
    align?: DropdownAlign;
}

export const CreateButton: React.FC<CreateButtonProps> = ({triggerIcon: TriggerIcon, children, align = "start"
                                                          }) => {
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    let renderChildren: React.ReactNode;

    if (typeof children === "function") {
        renderChildren = (children as (close: () => void) => React.ReactNode)(
            () => setIsDropdownOpen(false)
        );
    } else {
        renderChildren = children;
    }

    return (
        <DropdownMenu open={isDropdownOpen} onOpenChange={setIsDropdownOpen}>
            <DropdownMenuTrigger asChild>
                <Button
                    variant="ghost"
                    className="h-9 w-9 md:w-auto has-[>svg]:px-0 md:has-[>svg]:pl-2.5 md:has-[>svg]:pr-4 flex items-center justify-center gap-1.5
             rounded-full bg-gray-100 hover:bg-gray-200 text-black text-sm font-medium transition-colors"
                >
                    <TriggerIcon className="size-6"/>
                    <span className="hidden md:block">Tạo</span>
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align={align} sideOffset={8} className="w-56">
                {renderChildren}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};