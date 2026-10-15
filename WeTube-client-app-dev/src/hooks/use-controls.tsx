import { ControlsContext } from "@/contexts/controls-context";
import { useContext } from "react";


export const useControls = () => {
  const ctx = useContext(ControlsContext);

  if (!ctx) {
    throw new Error("useControls must be used within a ControlsProvider");
  }

  return ctx;
};
