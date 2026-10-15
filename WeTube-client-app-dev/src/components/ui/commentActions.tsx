"use client";

import { MoreHorizontal, Pencil, Trash2, Pin } from "lucide-react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";

interface CommentActionsProps {
    canPin?: boolean;
    canEdit?: boolean;
    canDelete?: boolean;
    onEdit: () => void;
    onDelete: () => void;
    onPin?: () => void;
}

export default function CommentActions({
                                           canPin,
                                           canEdit,
                                           canDelete,
                                           onEdit,
                                           onDelete,
                                           onPin,
                                       }: CommentActionsProps) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon">
                    <MoreHorizontal className="h-5 w-5" />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="w-40">
                {canPin && (
                    <DropdownMenuItem onClick={onPin}>
                        <Pin className="mr-2 h-4 w-4" /> Ghim
                    </DropdownMenuItem>
                )}
                {canEdit && (
                    <DropdownMenuItem onClick={onEdit}>
                        <Pencil className="mr-2 h-4 w-4" /> Chỉnh sửa
                    </DropdownMenuItem>
                )}
                {canDelete && (
                    <DropdownMenuItem
                        onClick={onDelete}
                        className="text-red-600 focus:text-red-600"
                    >
                        <Trash2 className="mr-2 h-4 w-4" /> Xóa
                    </DropdownMenuItem>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
