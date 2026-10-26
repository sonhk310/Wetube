import { apiFetch } from "@/lib/apiFetch";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";
import { ChannelInfo, ChannelVideo } from "@/types/channel";


export const channelApi = createApi({
  reducerPath: "channelApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}`,
    credentials: "include", fetchFn: apiFetch,
  }),
  tagTypes: ["ChannelInfo"],
  endpoints: (builder) => ({
    getChannelInfo: builder.query<ChannelInfo, string>({
      query: (channelId) => `/channels/${channelId}`,
      providesTags: (result, error, channelId) => [
        { type: "ChannelInfo", id: channelId },
      ],
    }),

    /** Handle dùng được cho kênh của mình không (đúng quy tắc, còn trống, hết thời gian chờ đổi). */
    checkHandle: builder.query<{ available: boolean; reason?: string | null }, string>({
      query: (handle) => `/channels/handle-check?handle=${encodeURIComponent(handle)}`,
      keepUnusedDataFor: 0,
    }),

    changeHandle: builder.mutation<ChannelInfo, { channelId: string; handle: string }>({
      query: ({ handle }) => ({ url: "/channels/me/handle", method: "PUT", body: { handle } }),
      invalidatesTags: (result, error, { channelId }) => [{ type: "ChannelInfo", id: channelId }],
    }),

    getChannelVideos: builder.query<
      { content: ChannelVideo[]; last: boolean },
      { userId: string; page?: number; size?: number }
    >({
      query: ({ userId, page = 0, size = 12 }) =>
        `/videos/by-user/${userId}?page=${page}&size=${size}`,
    }),
  }),
});

export const {
  useGetChannelInfoQuery,
  useGetChannelVideosQuery,
  useLazyCheckHandleQuery,
  useChangeHandleMutation,
} = channelApi;
