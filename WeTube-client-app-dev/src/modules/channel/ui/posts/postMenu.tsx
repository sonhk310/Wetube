"use client";

import { useState, useRef, useEffect } from "react";
import {EllipsisVertical} from "lucide-react";

interface PostMenuProps {
    onEdit: () => void;
    onDelete: () => void;
}

export default function PostMenu({ onEdit, onDelete }: PostMenuProps) {
    const [open, setOpen] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className="relative" ref={menuRef}>
            <button
                onClick={() => setOpen((prev) => !prev)}
                className="text-gray-500 hover:text-black p-1"
            >
                <EllipsisVertical size={20} />
            </button>

            {open && (
                <div className="absolute right-0 mt-2 w-32 bg-white shadow-lg rounded-lg border z-10">
                    <button
                        onClick={() => {
                            setOpen(false);
                            onEdit();
                        }}
                        className="block w-full px-4 py-2 text-left text-sm hover:bg-gray-100"
                    >
                        ✏️ Edit
                    </button>
                    <button
                        onClick={() => {
                            setOpen(false);
                            onDelete();
                        }}
                        className="block w-full px-4 py-2 text-left text-sm hover:bg-gray-100 text-red-500"
                    >
                        🗑️ Delete
                    </button>
                </div>
            )}
        </div>
    );
}
