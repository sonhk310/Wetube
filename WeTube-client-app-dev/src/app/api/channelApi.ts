import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";
import { ChannelInfo, ChannelSection, ChannelStructure, UpdateCategoryOrderRequest } from '@/types/channel';


export const channelApi = createApi({
  reducerPath: "channelApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}`,
    credentials: "include",
  }),
  tagTypes: ["ChannelLayout", "ChannelInfo"],
  endpoints: (builder) => ({
    getChannelInfo: builder.query<ChannelInfo, string>({
      query: (channelId) => `/channels/${channelId}`,
      providesTags: (result, error, channelId) => [
        { type: "ChannelInfo", id: channelId },
      ],
    }),

    getChannelLayout: builder.query<ChannelSection[], string>({
      query: (channelId) => `/channel/${channelId}/structure`,
      transformResponse: (response: ChannelStructure) => {
        if (response?.categories) {
          return response.categories.sort(
            (a, b) => a.orderPosition - b.orderPosition
          );
        }
        return [];
      },
      providesTags: (result, error, channelId) => [
        { type: "ChannelLayout", id: channelId },
      ],
    }),

    updateCategoryOrder: builder.mutation<
      void,
      { channelId: string; body: UpdateCategoryOrderRequest }
    >({
      query: ({ channelId, body }) => ({
        url: `/channel/${channelId}/categories/order`,
        method: "PUT",
        body,
      }),
      invalidatesTags: (result, error, { channelId }) => [
        { type: "ChannelLayout", id: channelId },
      ],
    }),
  }),
});

export const {
  useGetChannelInfoQuery,
  useGetChannelLayoutQuery,
  useUpdateCategoryOrderMutation,
} = channelApi;
