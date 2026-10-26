"use client";

import { Crown } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth } from "@/contexts/auth-context";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { sendRoom } from "../lib/roomSocket";
import type { RoomMember } from "../types";

function MemberAvatar({ member }: { member: RoomMember }) {
  return (
    <span className="relative inline-flex">
      <Avatar className="size-9 ring-2 ring-white">
        <AvatarImage src={member.avatar ?? undefined} alt="" />
        <AvatarFallback>{member.name.slice(0, 1).toUpperCase()}</AvatarFallback>
      </Avatar>
      {member.host && <Crown className="absolute -right-1 -top-1 size-4 fill-yellow-400 text-yellow-600" aria-label="Chủ phòng" />}
    </span>
  );
}

/** Đang xem: ảnh từng người (rê chuột thấy tên). Chủ phòng bấm vào người khác để mời ra khỏi phòng. */
export default function RoomMembers() {
  const { user } = useAuth();
  const members = useRoomStore((s) => s.members);
  const iAmHost = members.some((m) => m.host && m.userId === user?.sub);

  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-gray-600">Đang xem ({members.length}):</span>
      <div className="flex flex-wrap items-center gap-1">
        {members.map((m) => {
          const label = m.host ? `${m.name} (Chủ phòng)` : m.name;
          if (!iAmHost || m.userId === user?.sub) {
            return (
              <span key={m.userId} title={label}>
                <MemberAvatar member={m} />
              </span>
            );
          }
          return (
            <DropdownMenu key={m.userId}>
              <DropdownMenuTrigger asChild>
                <button type="button" title={label} aria-label={label} className="rounded-full">
                  <MemberAvatar member={m} />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start">
                <DropdownMenuItem
                  onClick={() => {
                    if (window.confirm(`Mời ${m.name} ra khỏi phòng? Họ sẽ không vào lại được phòng này.`)) {
                      sendRoom("kick", { userId: m.userId });
                    }
                  }}
                >
                  Mời {m.name} ra khỏi phòng
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          );
        })}
      </div>
    </div>
  );
}
