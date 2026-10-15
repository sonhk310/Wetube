import { ChatMessage, useChatStore } from "@/store/zustand/useChatStore";
import { useRoomStore } from "@/store/zustand/useRoomStore";
import { useStompStore } from "@/store/zustand/useStompStore";
import { useEffect, useRef, useState } from "react";

export default function RoomChat() {
  const { room, myUsername } = useRoomStore();

  const messages = useChatStore((state) => state.messages);

  const [input, setInput] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { publish } = useStompStore();

  // Scroll tới cuối
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;

    const publishEndpoint = `/app/chat/${room!.roomId}`;

    const message: ChatMessage = {
      type: "CHAT",
      sender: myUsername,
      content: input.trim(),
    };

    publish(publishEndpoint, message);
    setInput("");
  };

  return (
    <div className="flex flex-col w-full min-w-0 h-full border rounded-lg shadow-md bg-white">
      <div className="p-3 border-b font-semibold">
        Room Chat ({room!.roomId})
      </div>
      <div className="flex-1 min-h-0 overflow-y-auto p-3 space-y-2 bg-gray-50 h-[calc(100vh-200px)]">
        {messages.map((msg, i) => (
          <div key={i}>
            {msg.type === "JOIN" && (
              <p className="text-sm text-gray-500 italic">
                {msg.sender} joined the room
              </p>
            )}
            {msg.type === "LEAVE" && (
              <p className="text-sm text-gray-500 italic">
                {msg.sender} left the room
              </p>
            )}
            {msg.type === "CHAT" && (
              <p className="text-sm">
                <b>{msg.sender}:</b> {msg.content}
              </p>
            )}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>
      <form onSubmit={handleSendMessage} className="flex p-2 border-t bg-white shadow-inner">
        <input
          type="text"
          className="flex-1 border rounded px-2 py-1 text-sm"
          placeholder="Type a message..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
        <button
          type="submit"
          className="ml-2 px-3 py-1 bg-blue-600 text-white text-sm rounded"
        >
          Send
        </button>
      </form>
    </div>
  );
}
