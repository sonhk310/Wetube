import { ShortsContext } from "@/contexts/shorts-context";
import { useContext } from "react";

export const useShorts = () => {
  const ctx = useContext(ShortsContext);

  if (!ctx) {
    throw new Error("useShorts must be used within a ShortsProvider");
  }

  return ctx;
};
