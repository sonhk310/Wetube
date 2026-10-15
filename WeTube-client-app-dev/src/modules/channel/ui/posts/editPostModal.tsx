"use client";

import { useState, useEffect } from "react";

interface EditPostModalProps {
    isOpen: boolean;
    initialContent: string;
    onClose: () => void;
    onSave: (newContent: string) => void;
}

export default function EditPostModal({
                                          isOpen,
                                          initialContent,
                                          onClose,
                                          onSave,
                                      }: EditPostModalProps) {
    const [content, setContent] = useState(initialContent);

    useEffect(() => {
        if (isOpen) {
            setContent(initialContent);
        }
    }, [isOpen, initialContent]);

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded-xl shadow-lg w-full max-w-lg">
                <h2 className="text-lg font-semibold mb-4">Edit your post</h2>

                <textarea
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    className="w-full border rounded-lg p-2 text-sm text-black"
                    rows={4}
                />

                <div className="flex justify-end space-x-3 mt-4">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 rounded-lg bg-gray-200 hover:bg-gray-300 text-sm"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={() => {
                            onSave(content);
                            onClose();
                        }}
                        className="px-4 py-2 rounded-lg bg-blue-500 hover:bg-blue-600 text-white text-sm"
                    >
                        Save
                    </button>
                </div>
            </div>
        </div>
    );
}
