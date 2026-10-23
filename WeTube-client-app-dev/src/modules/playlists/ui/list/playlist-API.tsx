import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";

const API_BASE = `${API_PREFIX}/playlists`;

function jsonHeaders() {
  return {
    "Content-Type": "application/json",
  };
}

export const playlistService = {
  create: async (data: {
    userId: string;
    title: string;
    description?: string;
    type: string;
  }) => {
    if (!data.userId) throw new Error("Thiếu userId");

    const res = await apiFetch(`${API_BASE}/create`, {
      method: "POST",
      headers: jsonHeaders(),
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Không tạo được danh sách phát");
    return res.json();
  },


  getByUser: async (userId: string) => {
    if (!userId) throw new Error("Thiếu userId");

    const res = await apiFetch(`${API_BASE}/all/${userId}`, {
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Không tải được danh sách phát của người dùng");
    return res.json();
  },


  getCreated: async (userId: string) => {
    if (!userId) throw new Error("Thiếu userId");

    const res = await apiFetch(`${API_BASE}/created/${userId}`, {
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Không tải được danh sách phát đã tạo");
    return res.json();
  },

  getDetail: async (playlistId: string) => {
    if (!playlistId) throw new Error("Thiếu playlistId");

    const res = await apiFetch(`${API_BASE}/detail/${playlistId}`, {
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Không tải được chi tiết danh sách phát");
    return res.json();
  },
  getByUserAndType: async (userId: string, playlistType: string) => {
    if (!userId || !playlistType) throw new Error("Thiếu userId hoặc playlistType");

    const res = await apiFetch(
      `${API_BASE}/${userId}/playlistType?playlistType=${playlistType}`,
      { headers: jsonHeaders() }
    );
    if (!res.ok) throw new Error("Không tải được danh sách phát");
    return res.json();
  },

  getDetailByChannelAndName: async (channelId: string, playlistName: string) => {
    if (!channelId || !playlistName)
      throw new Error("Thiếu channelId hoặc tên playlist");

    const res = await apiFetch(
      `${API_BASE}/detail?channelId=${channelId}&playlistName=${encodeURIComponent(
        playlistName
      )}`,
      { headers: jsonHeaders() }
    );
    if (!res.ok) throw new Error("Không tải được chi tiết danh sách phát");
    return res.json();
  },

  // GET /containing/{videoId}: id các playlist của mình đang chứa video này
  getContaining: async (videoId: string): Promise<string[]> => {
    if (!videoId) throw new Error("Thiếu videoId");

    const res = await apiFetch(`${API_BASE}/containing/${videoId}`, {
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Không tải được các danh sách phát chứa video");
    return res.json();
  },

  // POST /add
  addVideo: async (playlistId: string, videoId: string) => {
    if (!playlistId || !videoId) throw new Error("Thiếu playlistId hoặc videoId");

    const res = await apiFetch(`${API_BASE}/add`, {
      method: "POST",
      headers: jsonHeaders(),
      body: JSON.stringify({ playlistId, videoId }),
    });
    if (!res.ok) throw new Error("Không thêm được video vào danh sách phát");
    return res.json();
  },

  // DELETE /{playlistId}/{videoId}
  removeVideo: async (playlistId: string, videoId: string) => {
    if (!playlistId || !videoId) throw new Error("Thiếu playlistId hoặc videoId");

    const res = await apiFetch(`${API_BASE}/${playlistId}/${videoId}`, {
      method: "DELETE",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Không gỡ được video khỏi danh sách phát");
    return res.json();
  },

  // DELETE /{playlistId}
  removePlaylist: async (playlistId: string) => {
    if (!playlistId) throw new Error("Thiếu playlistId");

    const res = await apiFetch(`${API_BASE}/${playlistId}`, {
      method: "DELETE",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Không xoá được danh sách phát");
    return res.json();
  },
  // POST|DELETE /{userId}/system/{LIKED|WATCH_LATER}/{videoId}
  addToSystemPlaylist: async (
    userId: string,
    videoId: string,
    type: "LIKED" | "WATCH_LATER"
  ) => {
    const res = await apiFetch(`${API_BASE}/${userId}/system/${type}/${videoId}`, {
      method: "POST",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error(`Không thêm được video vào ${type === "LIKED" ? "danh sách đã thích" : "danh sách xem sau"}`);
  },
  removeFromSystemPlaylist: async (
    userId: string,
    videoId: string,
    type: "LIKED" | "WATCH_LATER"
  ) => {
    const res = await apiFetch(`${API_BASE}/${userId}/system/${type}/${videoId}`, {
      method: "DELETE",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error(`Không gỡ được video khỏi ${type === "LIKED" ? "danh sách đã thích" : "danh sách xem sau"}`);
  },
  // POST /{userId}/history/add/{videoId}
  addToHistory: async (userId: string, videoId: string) => {
    if (!userId || !videoId) throw new Error("Thiếu userId hoặc videoId");

    const res = await apiFetch(`${API_BASE}/${userId}/history/add/${videoId}`, {
      method: "POST",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Không thêm được video vào lịch sử");
    return res.json();
  },

  getHistory: async (userId: string) => {
  if (!userId) throw new Error("Thiếu userId");

  const res = await apiFetch(`${API_BASE}/${userId}/history`, {
    method: "GET",
    headers: jsonHeaders(),
  });
  if (!res.ok) throw new Error("Không tải được lịch sử xem");
  return res.json();
},

};
