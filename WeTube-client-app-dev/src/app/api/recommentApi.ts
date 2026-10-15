import { API_PREFIX } from "@/constants/appConstant";
import { RecommendedVideoItem } from "@/types/video";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  numberOfElements: number;
  empty: boolean;
}

interface GetRecommendVideosParams {
  userId?: string;
  page: number;
  limit: number;
}

interface GetScoutVideosParams {
  userId: string;
  poolSize?: number;
  topN?: number;
}

export const recommendApi = createApi({
  reducerPath: "recommendApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}/recommend`,
    credentials: "include",
  }),
  endpoints: (builder) => ({
    getRecommendVideos: builder.query<
      PageResponse<RecommendedVideoItem>,
      GetRecommendVideosParams
    >({
      query: ({ userId, page, limit }) => ({
        url: `/${userId}`,
        params: {
          page: page - 1,
          size: limit,
        },
      }),
      forceRefetch({ currentArg, previousArg }) {
        return currentArg?.userId !== previousArg?.userId;
      },
    }),
    getScoutVideos: builder.query<RecommendedVideoItem[], GetScoutVideosParams>(
      {
        query: ({ userId, poolSize, topN }) => ({
          url: `/scout/${userId}`,
          params: { poolSize, topN },
        }),
      }
    ),
  }),
});

export const { useGetRecommendVideosQuery, useGetScoutVideosQuery } =
  recommendApi;
