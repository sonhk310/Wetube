"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { toastEmitter } from "@/lib/toastEmitter";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { useStompStore } from "@/store/zustand/useStompStore";
import { Room } from "@/types/room";
import { useRouter } from "next/navigation";
import { useState } from "react";

interface RoomProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export default function RoomModal({ open, onOpenChange }: RoomProps) {
  const [roomId, setRoomId] = useState("");
  const [username, setUsername] = useState("");
  const router = useRouter();
  const { setRoom, setMyUsername } = useRoomStore();
  const { connect, publish, subscribe, unsubscribe } = useStompStore();

  // -------------------
  // JOIN ROOM
  // -------------------
  const handleJoin = async () => {
    if (!roomId.trim() || !username.trim()) return;
    const toastId = toastEmitter.loading("🐱‍🏍 Connecting...");

    onOpenChange(false);
    try {
      await connect();
      toastEmitter.updateSuccess(toastId, "Connected Successfully!");
    } catch (err) {
      toastEmitter.updateError(toastId, "Connection failed!");
      return;
    }

    // Subscribe để nhận phản hồi khi phòng được tạo thành công
    const sub = subscribe(`/topic/rooms/join/${roomId}`, (message) => {
      try {
        console.log("Received message:", message);
        const room: Room = JSON.parse(message.body);

        setMyUsername(username);
        setRoom(room);
        unsubscribe(sub);
        toastEmitter.success("Room Joined!");
        router.push("/rooms");
      } catch (err) {
        toastEmitter.error("Failed to parse room data");
        console.error(err);
      }
    });

    // Gửi yêu cầu tạo phòng
    publish(`/app/room/join/${roomId}`, { username });
  };

  // -------------------
  // CREATE ROOM
  // -------------------
  const handleCreate = async () => {
    if (!username.trim()) return;

    onOpenChange(false);
    // const toastId = toastEmitter.loading("🐱‍🏍 Connecting...");

    try {
      await connect();
      //   toastEmitter.updateSuccess(toastId, "Connected Successfully!");
    } catch (err) {
      //   toastEmitter.updateError(toastId, "Connection failed!");
      return;
    }

    // Subscribe để nhận phản hồi khi phòng được tạo thành công
    const subscription = subscribe("/topic/room/create", (message) => {
      try {
        // In ra toàn bộ message, bạn có thể debug để kiểm tra
        // console.log("Received message:", message);

        // Parse message.body để lấy thông tin Room
        const room: Room = JSON.parse(message.body); // Bây giờ parse ở đây
        console.log("Parsed room:", room);

        setMyUsername(username);
        setRoom(room);
        toastEmitter.success("Room created!");
        unsubscribe(subscription);
        router.push("/rooms");
      } catch (err) {
        toastEmitter.error("Failed to parse room data");
        console.error(err);
      }
    });

    // Gửi yêu cầu tạo phòng
    publish("/app/room/create", { username });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Join or Create a Room</DialogTitle>
        </DialogHeader>

        <div className="flex flex-col gap-6">
          {/* Username */}
          <div>
            <label className="text-sm font-medium">Your Name</label>
            <Input
              placeholder="Enter username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>

          {/* Join Room */}
          <div>
            <label className="text-sm font-medium">Join with Room ID</label>
            <div className="flex gap-2 mt-1">
              <Input
                placeholder="Enter Room ID"
                value={roomId}
                onChange={(e) => setRoomId(e.target.value)}
              />
              <Button
                onClick={handleJoin}
                disabled={!roomId.trim() || !username.trim()}
              >
                Join
              </Button>
            </div>
          </div>

          {/* Create Room */}
          <div>
            <label className="text-sm font-medium">Create New Room</label>
            <div className="flex flex-col gap-2 mt-1">
              <Button
                variant="secondary"
                onClick={handleCreate}
                disabled={!username.trim()}
              >
                Create Room
              </Button>
            </div>
          </div>
        </div>

        <DialogFooter>
          <p className="text-xs text-gray-500">
            Share the Room ID with friends so they can join your room.
          </p>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
