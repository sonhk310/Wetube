"use client";

import apiClient from "@/lib/apiClient";
import axios from "axios";
import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

interface User {
  sub: string;
  name: string;
  email: string;
  picture: string;
  channelId: string;
  roles?: string[];
  /** Ngày hết hạn Premium (yyyy-MM-dd); null nếu không có Premium còn hiệu lực. */
  premiumUntil?: string | null;
    channel: {
    id: string;
    backgroundImgUrl: string;
    name: string;
    totalSubscribers: number;
    totalVideos: number;
    description: string;
    countryCode: string;
    createdAt: string; // ISO date string
    totalViews: number;
  };
  playlists: {
    playlistId: string;
    playlistTitle: string;
    playlistType: string;
    totalVideos: number;
    createdAt: string;
    privacy: string;
    lastUpdatedLabel: string;
    thumbnailUrl: string | null;
  }[];
}

interface AuthContextType {
  user: User | null;
  /** Đang có Premium còn hiệu lực (suy ra từ user.premiumUntil). */
  isPremium: boolean;
  isLoading: boolean;
  login: () => void;
  logout: () => Promise<void>;
  /** Tải lại thông tin người đang đăng nhập (vd. sau khi đổi tên / ảnh kênh ở Studio). */
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const response = await apiClient.get<User>("/me");
        const user = response.data;
        console.log("✅ Data from backendddddd:", response.data);
        console.log("✅ Data from Usseerrrr:", user.playlists);
        // Lấy playlist có type là HISTORY
        const historyPlaylist = user.playlists.find(
        (pl) => pl.playlistType === "HISTORY"
        );
        console.log("❤ PLaylist Data:", historyPlaylist?.playlistId);
        setUser(user);
        // Vừa đăng nhập từ liên kết mời phòng xem chung: quay lại đúng phòng
        try {
          const back = window.sessionStorage.getItem("wetube_after_login");
          if (back) {
            window.sessionStorage.removeItem("wetube_after_login");
            if (back.startsWith("/rooms/") && window.location.pathname !== back) window.location.replace(back);
          }
        } catch {
          // sessionStorage bị chặn: ở lại trang hiện tại
        }
      } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 401) {
          console.log("User is not authenticated (handled gracefully).");
        } else {
          console.error(
            "❌ An unexpected error occurred while fetching user:",
            error
          );
        }
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUser();

    const handleAuthFailure = () => {
      console.log("Auth failure event received, clearing user.");
      setUser(null);
    };
    window.addEventListener("auth-failure", handleAuthFailure);

    return () => {
      window.removeEventListener("auth-failure", handleAuthFailure);
    };
  }, []);

  const login = useCallback(() => {
    window.location.href = `${apiClient.defaults.baseURL}/auth/login/google`;
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiClient.post("/auth/logout");
      setUser(null);
    } catch (error) {
      console.error("Logout failed:", error);
    }
  }, []);

  const refresh = useCallback(async () => {
    try {
      const response = await apiClient.get<User>("/me");
      setUser(response.data);
    } catch (error) {
      console.error("Failed to refresh user:", error);
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      isPremium: !!user?.premiumUntil,
      isLoading,
      login,
      logout,
      refresh,
    }),
    [user, isLoading, login, logout, refresh]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
