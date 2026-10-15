"use client";

import { useState } from "react";

interface Props {
    onSubmit: (url: string) => void;
}

export default function VideoUrlInput({ onSubmit }: Props) {
    const [url, setUrl] = useState("");

    const handleSubmit = () => {
        if (!url.trim()) return;
        onSubmit(url.trim());
        setUrl("");
    };

    return (
        <div className="flex gap-2 items-center mt-4 w-full max-w-2xl">
            <input
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="Paste video URL..."
                className="flex-1 bg-neutral-800 px-3 py-2 rounded-xl outline-none"
            />
            <button
                onClick={handleSubmit}
                className="bg-red-600 px-4 py-2 rounded-xl hover:bg-red-700"
            >
                Load
            </button>
        </div>
    );
}
