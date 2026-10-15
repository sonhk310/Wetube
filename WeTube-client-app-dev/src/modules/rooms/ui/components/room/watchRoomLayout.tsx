"use client";
import { ChatMessage, useChatStore } from "@/store/zustand/useChatStore";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { useStompStore } from "@/store/zustand/useStompStore";
import { MediaPlayerState, VideoRoom, WatchMember } from "@/types/room";
import { MessageCircle } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import VideoPlayer from "../video/videoPlayer";
import MemberList from "./membersList";
import RoomChat from "./roomChat";
import UpcomingList from "./upcomingList";

export default function WatchRoomLayout() {
  const router = useRouter();
  const [chatOpen, setChatOpen] = useState(false);

  const { room, myUsername, setMediaState, addSong, host, setMemberList } =
    useRoomStore();
  const { client, publish, subscribe, unsubscribe } = useStompStore();
  const addMessage = useChatStore((state) => state.addMessage);
  const clearMessages = useChatStore((state) => state.clearMessages);

  useEffect(() => {
    const roomId = room!.roomId;

    const subs = [
      subscribe(`/topic/rooms/addSong/${roomId}`, (msg) => {
        const payload: VideoRoom = JSON.parse(msg.body);
        addSong(payload);
      }),
      subscribe(`/topic/rooms/chat/${roomId}`, (msg) => {
        const payload: ChatMessage = JSON.parse(msg.body);
        addMessage(payload);
      }),

      subscribe(`/topic/rooms/member/${roomId}`, (msg) => {
        if (msg.body) {
          let members: WatchMember[] = [];
          try {
            const parsed = JSON.parse(msg.body);

            // đảm bảo là array
            members = Array.isArray(parsed) ? parsed : [parsed];
          } catch (e) {
            console.error("Failed to parse members from server", e);
          }

          console.log("Updated member list:", members);
          setMemberList(members);
        }
      }),
    ];

    if (!host) {
      subs.push(
        subscribe(`/topic/rooms/mediaState/${roomId}`, (msg) => {
          const payload: MediaPlayerState = JSON.parse(msg.body);
          setMediaState(payload);
        })
      );
      publish(`/app/room/member/${roomId}`, { username: myUsername });
    }

    publish(`/app/chat/${roomId}`, { type: "JOIN", sender: myUsername });

    return () => {
      publish(`/app/chat/${roomId}`, { type: "LEAVE", sender: myUsername });
      publish(`/app/room/member/leave/${room?.roomId}`, {
        username: myUsername,
      });

      subs.forEach(unsubscribe);
      clearMessages();
    };
  }, []);

  const handleLeaveRoom = () => {
    publish(`/app/chat/${room?.roomId}`, { type: "LEAVE", sender: myUsername });
    publish(`/app/room/member/leave/${room?.roomId}`, { username: myUsername });
    router.push("/");
  };

  return (
    <div className="flex flex-col flex-1 w-full h-full bg-white text-black p-3 md:p-4 overflow-hidden">
      <div className="flex flex-col lg:flex-row w-full h-full gap-4 overflow-hidden">
        <div className="flex flex-col flex-1 gap-4 min-w-0">
          {/* Video Player */}
          <div className="flex-1 w-full aspect-video bg-black rounded-lg shadow-lg overflow-hidden">
            <VideoPlayer
            // onChangeVideo={setCurrentVideoId}
            />
          </div>

          {/* Upcomming Video and Leave Room */}
          <div className="bg-gray-100 rounded-lg shadow-lg p-4 flex flex-col border border-gray-200 overflow-hidden">
            {room && (
              <div className="flex flex-col md:flex-row justify-between items-center mb-3 gap-2">
                <h2 className="text-lg md:text-xl font-semibold text-gray-800">
                  Upcoming Videos
                </h2>
                <button
                  onClick={handleLeaveRoom}
                  className="px-4 py-2 bg-red-600 text-white font-semibold rounded-full hover:bg-red-700 transition-colors"
                >
                  Leave Room
                </button>
              </div>
            )}

            <div className="overflow-x-auto scrollbar-thin scrollbar-thumb-gray-400">
              {room && (
                <UpcomingList
                //   onPlay={setCurrentVideoId}
                />
              )}
            </div>

            {room && client && (
              <div className="mt-3">
                <MemberList />
              </div>
            )}
          </div>
        </div>

                <div
                    className="hidden lg:flex w-full lg:w-[30%] flex-col bg-gray-100 rounded-lg shadow-lg border border-gray-200 overflow-hidden">
                    {room && client && (
                        <div className="flex flex- flex-1 min-h-0">
                                {room && client && <RoomChat/>}
                        </div>
                    )}
                </div>
            </div>

      <button
        onClick={() => setChatOpen(true)}
        className="lg:hidden fixed bottom-4 right-4 bg-blue-600 text-white p-3 rounded-full shadow-lg"
      >
        <MessageCircle className="w-6 h-6" />
      </button>

      {chatOpen && (
        <div className="lg:hidden fixed inset-0 bg-black bg-opacity-50 flex justify-center items-end z-50">
          <div className="bg-white w-full h-3/4 rounded-t-lg shadow-lg flex flex-col">
            <div className="flex justify-between items-center p-3 border-b">
              <h2 className="font-semibold text-lg">Room Chat</h2>
              <button
                onClick={() => setChatOpen(false)}
                className="text-gray-600 hover:text-black"
              >
                ✕
              </button>
            </div>
            <div className="flex-1 overflow-y-auto">
              {room && client && <RoomChat />}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
