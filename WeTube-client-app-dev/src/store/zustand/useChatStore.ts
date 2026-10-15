import { create } from "zustand";

export interface ChatMessage {
  type: "CHAT" | "JOIN" | "LEAVE";
  sender: string;
  content?: string;
}

interface ChatState {
  messages: ChatMessage[];
  addMessage: (msg: ChatMessage) => void;
  clearMessages: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  
  addMessage: (msg) =>
    set((state) => ({
      messages: [...state.messages, msg],
    })),

  clearMessages: () =>
    set({ messages: [] }),
}));
