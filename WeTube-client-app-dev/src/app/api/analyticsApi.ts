import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";
import { AnalyticsRange, ChannelAnalytics } from "@/types/analytics";

export const analyticsApi = createApi({
  reducerPath: "analyticsApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}`,
    credentials: "include",
  }),
  endpoints: (builder) => ({
    getChannelAnalytics: builder.query<ChannelAnalytics, AnalyticsRange>({
      query: (days) => `/studio/analytics?days=${days}`,
      // Vào lại trang thì lấy số mới
      keepUnusedDataFor: 0,
    }),
  }),
});

export const { useGetChannelAnalyticsQuery } = analyticsApi;
