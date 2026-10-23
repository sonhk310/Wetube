"use client";
import { ControlsProvider } from "@/contexts/controls-context";
import App from "@/modules/watch/app";
import { useParams } from "next/navigation";

export default function Home() {
    const params = useParams<{ videoId: string }>();
    const videoId = params.videoId;

    return (
        <ControlsProvider>
            <App videoId={videoId} />
        </ControlsProvider>
    );
}
