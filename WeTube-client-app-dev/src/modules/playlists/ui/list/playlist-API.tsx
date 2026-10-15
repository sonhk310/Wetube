const API_BASE = "https://wetube.name.vn/api/playlists";

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
    if (!data.userId) throw new Error("UserId is required");

    const res = await fetch(`${API_BASE}/create`, {
      method: "POST",
      headers: jsonHeaders(),
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error("Failed to create playlist");
    return res.json();
  },


  getByUser: async (userId: string) => {
    if (!userId) throw new Error("UserId is required");

    const res = await fetch(`${API_BASE}/all/${userId}`, {
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Failed to fetch user playlists");
    return res.json();
  },


  getCreated: async (userId: string) => {
    if (!userId) throw new Error("UserId is required");

    const res = await fetch(`${API_BASE}/created/${userId}`, {
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Failed to fetch created playlists");
    return res.json();
  },

  getDetail: async (playlistId: string) => {
    if (!playlistId) throw new Error("PlaylistId is required");

    const res = await fetch(`${API_BASE}/detail/${playlistId}`, {
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Failed to fetch playlist detail");
    return res.json();
  },
  getByUserAndType: async (userId: string, playlistType: string) => {
    if (!userId || !playlistType) throw new Error("UserId and playlistType are required");

    const res = await fetch(
      `${API_BASE}/${userId}/playlistType?playlistType=${playlistType}`,
      { headers: jsonHeaders() }
    );
    if (!res.ok) throw new Error("Failed to fetch playlist by user and type");
    return res.json();
  },

  getDetailByChannelAndName: async (channelId: string, playlistName: string) => {
    if (!channelId || !playlistName)
      throw new Error("channelId and playlistName are required");

    const res = await fetch(
      `${API_BASE}/detail?channelId=${channelId}&playlistName=${encodeURIComponent(
        playlistName
      )}`,
      { headers: jsonHeaders() }
    );
    if (!res.ok) throw new Error("Failed to fetch playlist detail by channelId and name");
    return res.json();
  },

  // POST /add
  addVideo: async (playlistId: string, videoId: string) => {
    if (!playlistId || !videoId) throw new Error("PlaylistId and VideoId required");

    const res = await fetch(`${API_BASE}/add`, {
      method: "POST",
      headers: jsonHeaders(),
      body: JSON.stringify({ playlistId, videoId }),
    });
    if (!res.ok) throw new Error("Failed to add video to playlist");
    return res.json();
  },

  // DELETE /{playlistId}/{videoId}
  removeVideo: async (playlistId: string, videoId: string) => {
    if (!playlistId || !videoId) throw new Error("PlaylistId and VideoId required");

    const res = await fetch(`${API_BASE}/${playlistId}/${videoId}`, {
      method: "DELETE",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Failed to remove video from playlist");
    return res.json();
  },

  // DELETE /{playlistId}
  removePlaylist: async (playlistId: string) => {
    if (!playlistId) throw new Error("PlaylistId is required");

    const res = await fetch(`${API_BASE}/${playlistId}`, {
      method: "DELETE",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Failed to remove playlist");
    return res.json();
  },
  // POST /{userId}/history/add/{videoId}
  addToHistory: async (userId: string, videoId: string) => {
    if (!userId || !videoId) throw new Error("   and VideoId are required");

    const res = await fetch(`${API_BASE}/${userId}/history/add/${videoId}`, {
      method: "POST",
      headers: jsonHeaders(),
    });
    if (!res.ok) throw new Error("Failed to add video to history");
    return res.json();
  },

  getHistory: async (userId: string) => {
  if (!userId) throw new Error("UserId is required");

  const res = await fetch(`${API_BASE}/${userId}/history`, {
    method: "GET",
    headers: jsonHeaders(),
  });
  if (!res.ok) throw new Error("Failed to fetch watch history");
  return res.json();
},

};
