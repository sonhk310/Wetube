import { apiFetch } from "@/lib/apiFetch";
import { API_PREFIX } from "@/constants/appConstant";
import { PlaylistDetail, PlaylistSummary, VideoFromApi } from "@/types/playlistSummary";

import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

export const playlistApi = createApi({
  reducerPath: "playlistApi",
  baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}/playlists`, credentials: "include", fetchFn: apiFetch }),
  tagTypes: ["Playlist"],
  endpoints: (builder) => ({
    // 🔹 Lấy tất cả playlist theo user
    getPlaylistsByUserId: builder.query<PlaylistSummary[], string>({
      query: (userId) => `all/${userId}`,
      providesTags: (result, error, userId) => [
        { type: "Playlist", id: `USER_${userId}` },
        "Playlist",
      ],
    }),

    // 🔹 Lấy detail 1 playlist
    getPlaylistDetails: builder.query<PlaylistDetail, string>({
      query: (playlistId) => `detail/${playlistId}`,
      providesTags: (result, error, playlistId) => [
        { type: "Playlist", id: playlistId },
        "Playlist",
      ],
    }),

    // 🔹 Thêm video vào playlist (bao gồm cả HISTORY)
    addVideoToPlaylist: builder.mutation<
      VideoFromApi, // backend trả PlaylistVideoDto
      { playlistId: string; videoId: string; historyDuration?: number }
    >({
      query: (body) => ({
        url: `add`,
        method: "POST",
        body,
        headers: { "Content-Type": "application/json" },
      }),
      invalidatesTags: (result, error, { playlistId }) => [
        { type: "Playlist", id: playlistId },
        "Playlist",
      ],
    }),
  }),
});

export const {
  useGetPlaylistsByUserIdQuery,
  useGetPlaylistDetailsQuery,
  useAddVideoToPlaylistMutation, // ✅ mutation thêm video
} = playlistApi;
