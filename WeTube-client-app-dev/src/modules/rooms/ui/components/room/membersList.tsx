"use client";

import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { Users } from "lucide-react";

export default function MemberList() {
  const { room } = useRoomStore();

  return (
    <Dialog>
      <DialogTrigger asChild>
        <button className="relative flex items-center gap-1 p-2 rounded-lg hover:bg-muted">
          <Users className="w-5 h-5" />

          <Badge className="absolute -top-1 -right-2 px-2 py-0.5 text-xs">
            {room?.members.length ?? 0}
          </Badge>
        </button>
      </DialogTrigger>
      <DialogContent className="max-w-xs">
        <DialogHeader>
          <DialogTitle>Room Members ({room?.members.length})</DialogTitle>
        </DialogHeader>
        <ul className="mt-2 space-y-1 text-sm">
          {room?.members.length === 0 && (
            <li className="text-muted-foreground italic">No members</li>
          )}
          {room!.members.map((m) => (
            <li key={m.username} className="px-2 py-1 rounded hover:bg-muted">
              {m.username}
            </li>
          ))}
        </ul>
      </DialogContent>
    </Dialog>
  );
}
