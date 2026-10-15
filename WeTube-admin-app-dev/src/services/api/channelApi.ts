import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { IChannel } from '../../types/channelTypes/channel';

export const channelApi = createApi({
  reducerPath: "channelApi",
  baseQuery: fetchBaseQuery({
    baseUrl: "https://687076877ca4d06b34b6db29.mockapi.io/api/v1/",
  }),
  tagTypes: ["Channels"],
  endpoints: (builder) => ({
    getAllChannels: builder.query<IChannel[], void>({
      query: () => "channel",
      providesTags: ["Channels"],
    }),

    getChannels: builder.query<IChannel[], { [key: string]: string } | void>({
      query: (params) => ({
        url: "channel",
        params: params ?? undefined,
      }),
      transformResponse: (response: IChannel[], _meta, arg) => {
        if (!arg) return response;
        const key = Object.keys(arg)[0] as keyof IChannel;
        const value = arg[key];

        if (key === "status") {
          const statuses = value.split(","); // Handle multiple statuses (e.g., "Active,Banned")
          return response.filter((channel) =>
            statuses.includes(channel.status)
          );
        }
        if (key !== "channelName" && key !== "email") return response;
        return response.filter((channel) =>
          String(channel[key]).toLowerCase().includes(value.toLowerCase())
        );
      },
      providesTags: ["Channels"],
    }),

    getChannelInfo: builder.query<IChannel, string>({
      query: (id) => `channel/${id}`,
      providesTags: (_result, _error, id) => [{ type: "Channels", id }],
    }),

    deleteChannel: builder.mutation<{ success: boolean }, string>({
      query: (id) => ({
        url: `channel/${id}`,
        method: "DELETE",
      }),
      invalidatesTags: ["Channels"],
    }),
  }),
});

export const {
  useGetAllChannelsQuery,
  useGetChannelsQuery,
  useGetChannelInfoQuery,
  useDeleteChannelMutation,
} = channelApi;