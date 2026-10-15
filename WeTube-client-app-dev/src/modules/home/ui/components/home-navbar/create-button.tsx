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
                    className="h-10 w-30 px-8 flex items-center gap-3
             text-black text-base md:text-md rounded-full shadow-md transition-all duration-200"
                >
                    <TriggerIcon className="w-7 h-7"/>
                    <span className="hidden md:block">Create</span>
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align={align} sideOffset={8} className="w-56">
                {renderChildren}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};