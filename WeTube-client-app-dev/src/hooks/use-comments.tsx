import { CommentsContext } from "@/contexts/comment-context";
import { useContext } from "react";

export const useComments = () => {
  const ctx = useContext(CommentsContext);

  if (!ctx) {
    throw new Error("useComments must be used within a CommentsProvider");
  }

  return ctx;
};
