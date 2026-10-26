"use client";

import { Crown } from "lucide-react";
import { useState } from "react";
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
import ConfirmDialog from "./confirm-dialog";

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
  const [kickTarget, setKickTarget] = useState<RoomMember | null>(null);

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
            <DropdownMenu key={m.userId} modal={false}>
              <DropdownMenuTrigger asChild>
                <button type="button" title={label} aria-label={label} className="rounded-full">
                  <MemberAvatar member={m} />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start">
                <DropdownMenuItem onSelect={() => setKickTarget(m)}>
                  Mời {m.name} ra khỏi phòng
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          );
        })}
      </div>
      <ConfirmDialog
        open={!!kickTarget}
        onOpenChange={(open) => !open && setKickTarget(null)}
        title={`Mời ${kickTarget?.name ?? ""} ra khỏi phòng?`}
        description="Người này sẽ không vào lại được phòng này, kể cả khi có liên kết mời."
        confirmLabel="Mời ra"
        onConfirm={() => kickTarget && sendRoom("kick", { userId: kickTarget.userId })}
      />
    </div>
  );
}
