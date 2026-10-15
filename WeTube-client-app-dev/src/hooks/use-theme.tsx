import { ThemeContext } from "@/contexts/theme-context";
import { useContext } from "react";

export const useTheme = () => {
  const ctx = useContext(ThemeContext);

  if (!ctx) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }

  return ctx;
};
