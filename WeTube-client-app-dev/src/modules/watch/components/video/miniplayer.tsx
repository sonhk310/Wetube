"use client"

import {useControls} from "@/hooks/use-controls";
import {useEffect, useRef} from "react";
import ReactPlayer from "react-player";
import {OnProgressProps} from "react-player/base";
import {useVideoStore} from "@/store/zustand/videoStore";
import {Loader} from "../misc/loader";

export default function MiniPlayer() {
    const {
        isPlaying,
        pipMode,
        pauseVideo,
        setPercentage,
        setTotalSeek,
        volume,
        setLoaded,
        playbackSpeed,
        seekSync,
    } = useControls();
    const videoUrl = useVideoStore((state) => state.videoDetail?.detail.videoUrl);
    const reactPlayerRef = useRef<ReactPlayer | null>(null);

    useEffect(() => {
        if (reactPlayerRef.current) {
            reactPlayerRef.current.seekTo(seekSync, "seconds");
        }
    }, [seekSync]);

    if (!videoUrl) {
        return (
            <div className="absolute top-0 left-0 h-full w-full bg-black flex justify-center items-center">
                <Loader/>
            </div>
        );
    }

    return (
        <ReactPlayer
            ref={reactPlayerRef}
            fallback={
                <div className="absolute top-0 left-0 h-full w-full bg-black flex justify-center items-center"></div>
            }
            playing={isPlaying}
            volume={pipMode ? volume / 100 : 0}
            controls={false}
            progressInterval={500}
            url={videoUrl}
            width="640px"
            playbackRate={playbackSpeed}
            style={{
                aspectRatio: "16/9",
                position: "absolute",
                bottom: 0,
                right: 0,
                zIndex: pipMode ? 100 : -100,
                opacity: pipMode ? 1 : 0,
            }}
            onProgress={(state: OnProgressProps) => {
                setPercentage(Math.min(100, state.played * 100));
                setLoaded(Math.min(100, state.loaded * 100));
            }}
            onReady={(player) => {
                setTotalSeek(player.getDuration());
            }}
            onEnded={() => {
                pauseVideo();
            }}
        />
    );
}
