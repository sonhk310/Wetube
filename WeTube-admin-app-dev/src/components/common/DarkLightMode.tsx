"use client";

import { motion } from "framer-motion";
import { useEffect, useState } from "react";

export default function LayoutAnimation() {
  const [isDark, setIsDark] = useState(false);

  // Load trạng thái từ localStorage (nếu có)
  useEffect(() => {
    const storedTheme = localStorage.getItem("theme");
    if (storedTheme === "dark") {
      setIsDark(true);
      document.documentElement.classList.add("dark");
    }
  }, []);

  const toggleTheme = () => {
    const newTheme = !isDark;
    setIsDark(newTheme);

    if (newTheme) {
      document.documentElement.classList.add("dark");
      localStorage.setItem("theme", "dark");
    } else {
      document.documentElement.classList.remove("dark");
      localStorage.setItem("theme", "light");
    }
  };

  return (
    <button
      className="toggle-container"
      style={{
        ...container,
        justifyContent: isDark ? "flex-end" : "flex-start",
      }}
      onClick={toggleTheme}
    >
      <motion.div
        className="w-6 h-6 bg-purple-900 rounded-full"
        style={handle}
        layout
        transition={{
          type: "spring",
          duration: 0.2,
          bounce: 0.2,
        }}
      />
    </button>
  );
}

/**
 * ==============   Styles   ================
 */

const container = {
  width: 50,
  height: 30,
  backgroundColor: "#8F89B7",
  borderRadius: 100,
  cursor: "pointer",
  display: "flex",
  padding: 5,
};

const handle = {
  width: 20,
  height: 20,
  backgroundColor: "white",
  borderRadius: "50%",
};
