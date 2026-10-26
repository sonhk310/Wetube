"use client";

import { useParams } from "next/navigation";
import { ControlsProvider } from "@/contexts/controls-context";
import RoomView from "@/modules/rooms/ui/room-view";

export default function RoomPage() {
  const { code } = useParams<{ code: string }>();

  return (
    <ControlsProvider>
      <RoomView code={code} />
    </ControlsProvider>
  );
}
