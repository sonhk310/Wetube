import { ControlsContext, TheaterContext } from "@/contexts/controls-context";
import { useContext } from "react";


export const useControls = () => {
  const ctx = useContext(ControlsContext);

  if (!ctx) {
    throw new Error("useControls must be used within a ControlsProvider");
  }

  return ctx;
};

export const useTheater = () => {
  const ctx = useContext(TheaterContext);

  if (!ctx) {
    throw new Error("useTheater must be used within a ControlsProvider");
  }

  return ctx;
};
