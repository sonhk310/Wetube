"use client";

import { useRoomStore } from "@/store/zustand/useRoomStore";
import { useStompStore } from "@/store/zustand/useStompStore";
import { VideoRoom } from "@/types/room";
import {
  closestCenter,
  DndContext,
  KeyboardSensor,
  PointerSensor,
  useDndMonitor,
  useSensor,
  useSensors,
} from "@dnd-kit/core";
import {
  horizontalListSortingStrategy,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Play } from "lucide-react";
import Image from "next/image";
import { useRef } from "react";

// Component hiển thị thông tin bài hát
function SortableVideo({
  roomId,
  video,
  isActive,
}: {
  roomId: string;
  video: VideoRoom;
  isActive: boolean;
}) {
  const { attributes, listeners, setNodeRef, transform, transition } =
    useSortable({
      id: video.id,
    });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  const { host, setMediaState } = useRoomStore();
  const { publish } = useStompStore();

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      className={`relative w-40 h-24 rounded-lg overflow-hidden cursor-move group ${
        isActive ? "ring-2 ring-blue-500" : ""
      }`}
    >
      <Image
        src={video.thumbnailUrl}
        alt={video.title}
        width={160}
        height={96}
        className="w-full h-full object-cover"
      />
      <div
        className={`absolute inset-0 bg-black/40 transition flex items-center justify-center
        ${
          isActive
            ? "opacity-0 pointer-events-none"
            : "opacity-0 group-hover:opacity-100"
        }`}
      >
        <button
          // Phát bài hát khi nhấn nút play
          onDoubleClick={() => {
            console.log("Double Clicked to play:", video);
            console.log("Is HOst??????????:", host);
            const newMediaState = {
              roomId: roomId,
              playing: true,
              currentTimeMillis: 0,
              currentSongId: video.videoUrl,
            };

            if (host) {
              publish(`/app/room/mediaState/${roomId}`, newMediaState);
            }
            setMediaState(newMediaState);
          }} // Update room.playerState.currentSongId
          className="bg-white/80 rounded-full p-2 hover:bg-white"
        >
          <Play className="w-6 h-6 text-black" />
        </button>
      </div>
    </div>
  );
}

// Danh sách bài hát có khả năng kéo và thả
function DraggableVideoList({
  roomId,
  videos,
  currentVideoId,
}: {
  roomId: string;
  videos: VideoRoom[];
  currentVideoId: string;
}) {
  const scrollRef = useRef<HTMLDivElement>(null);

  useDndMonitor({
    onDragMove(event) {
      if (!scrollRef.current) return;
      const container = scrollRef.current;
      const { activatorEvent } = event;

      if (
        activatorEvent &&
        "clientX" in activatorEvent &&
        typeof activatorEvent.clientX === "number"
      ) {
        const rect = container.getBoundingClientRect();
        const x = activatorEvent.clientX;

        const edgeThreshold = 80;
        const scrollSpeed = 10;

        if (x < rect.left + edgeThreshold) {
          container.scrollLeft -= scrollSpeed;
        } else if (x > rect.right - edgeThreshold) {
          container.scrollLeft += scrollSpeed;
        }
      }
    },
  });

  return (
    <SortableContext
      items={videos.map((v) => v.id)}
      strategy={horizontalListSortingStrategy}
    >
      <div
        ref={scrollRef}
        className="flex gap-3 overflow-x-auto p-2 scrollbar-thin scrollbar-thumb-gray-400"
      >
        {videos.map((v) => (
          <SortableVideo
            key={v.id}
            roomId={roomId}
            video={v}
            isActive={v.id === currentVideoId}
          />
        ))}
      </div>
    </SortableContext>
  );
}

export default function UpcomingList() {
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  const { room } = useRoomStore();

  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter}>
      <DraggableVideoList
        videos={room?.playlist || []}
        roomId={room!.roomId}
        currentVideoId={room?.playerState.currentSongId || ""}
      />
    </DndContext>
  );
}
