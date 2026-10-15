"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { XMarkIcon } from "@heroicons/react/24/outline";
import { useState } from "react";
import axios from "axios";
import { API_PREFIX } from "@/constants/appConstant";

export default function TranscriptModal({ videoId }: { videoId: string }) {
    const [loading, setLoading] = useState(false);
    const [transcript, setTranscript] = useState<string | null>(null);

    const fetchTranscript = async (lang: string) => {
        setLoading(true);
        try {
            const res = await axios.post(
                `${API_PREFIX}/ai/transcript`,
                {
                    params: { videoId, lang },
                }
            );
            setTranscript(res.data.transcript);
        } catch (err) {
            console.error("Error fetching transcript:", err);
        } finally {
            setLoading(false);
        }
    };


    return (
        <Dialog.Root>
            <Dialog.Trigger asChild>
                <button className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full cursor-pointer">
                    📜
                </button>
            </Dialog.Trigger>

            <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 bg-black/40 z-40" />
                <Dialog.Content className="fixed top-1/2 left-1/2 z-50 w-[90vw] max-w-2xl max-h-[80vh] -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg shadow-lg p-6 overflow-y-auto">
                    <div className="flex justify-between items-center mb-4">
                        <Dialog.Title className="text-lg font-bold">Transcript</Dialog.Title>
                        <Dialog.Close asChild>
                            <button>
                                <XMarkIcon className="size-6" />
                            </button>
                        </Dialog.Close>
                    </div>

                    <div className="flex gap-2 mb-4">
                        <button
                            onClick={() => fetchTranscript("en")}
                            className="px-3 py-1 bg-gray-200 rounded hover:bg-gray-300"
                        >
                            English
                        </button>
                        <button
                            onClick={() => fetchTranscript("vi")}
                            className="px-3 py-1 bg-gray-200 rounded hover:bg-gray-300"
                        >
                            Vietnamese
                        </button>
                    </div>

                    {loading ? (
                        <p className="text-gray-500">Loading transcript...</p>
                    ) : transcript ? (
                        <p className="whitespace-pre-line text-sm">{transcript}</p>
                    ) : (
                        <p className="text-gray-400">Select a language to generate transcript.</p>
                    )}
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
}
