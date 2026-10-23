import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";
import { ChannelInfo, ChannelVideo } from "@/types/channel";


export const channelApi = createApi({
  reducerPath: "channelApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}`,
    credentials: "include",
  }),
  tagTypes: ["ChannelInfo"],
  endpoints: (builder) => ({
    getChannelInfo: builder.query<ChannelInfo, string>({
      query: (channelId) => `/channels/${channelId}`,
      providesTags: (result, error, channelId) => [
        { type: "ChannelInfo", id: channelId },
      ],
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
} = channelApi;
