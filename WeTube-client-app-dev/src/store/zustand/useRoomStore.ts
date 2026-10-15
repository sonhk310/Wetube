import { MediaPlayerState, Room, VideoRoom, WatchMember } from "@/types/room";
import { create } from "zustand";

interface RoomStore {
  room: Room | null;
  myUsername: string;
  host: boolean;
  setRoom: (room: Room) => void;
  clearRoom: () => void;
  setMyUsername: (username: string) => void;
  clearMyUsername: () => void;
  addSong: (song: VideoRoom) => void;
  setMediaState: (mediaState: MediaPlayerState) => void;
  setCurrentSongId: (id: string) => void;
  setMemberList: (members: WatchMember[]) => void;
}

export const useRoomStore = create<RoomStore>((set) => ({
  room: null,
  myUsername: "",
  host: false,

  setRoom: (room) =>
    set((state) => {
      const hostMember = room.members.find((m) => m.host);
      const isHost = hostMember?.username === state.myUsername;
      return {
        room: {
          ...room,
          members: room.members || [],
        },
        host: isHost,
      };
    }),

  clearRoom: () => set({ room: null }),

  setMyUsername: (username) => set({ myUsername: username }),
  clearMyUsername: () => set({ myUsername: "" }),

  addSong: (song) =>
    set((state) => ({
      room: {
        ...state.room,
        playlist: [...(state.room?.playlist || []), song],
      } as Room,
    })),

  setCurrentSongId: (id: string) =>
    set((state) => ({
      room: state.room
        ? {
            ...state.room,
            playerState: { ...state.room.playerState, currentSongId: id },
          }
        : null,
    })),

  setMediaState: (mediaState: MediaPlayerState) =>
    set((state) => ({
      room: state.room
        ? {
            ...state.room,
            playerState: {
              ...state.room.playerState,
              ...mediaState,
            },
          }
        : null,
    })),

  setMemberList: (members: WatchMember[]) =>
    set((state) =>
      state.room
        ? {
            room: {
              ...state.room,
              members: members || [],
            },
          }
        : state
    ),
}));
