import { apiFetch } from "@/lib/apiFetch";
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
  /** Mỗi lần mở trang chủ một mã mới: cùng mã thì thứ tự ổn định, đổi mã thì đổi nhẹ. */
  seed?: string;
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
    credentials: "include", fetchFn: apiFetch,
  }),
  endpoints: (builder) => ({
    getRecommendVideos: builder.query<
      PageResponse<RecommendedVideoItem>,
      GetRecommendVideosParams
    >({
      query: ({ userId, page, limit, seed }) => ({
        url: `/${userId}`,
        params: {
          page: page - 1,
          size: limit,
          seed,
        },
      }),
      forceRefetch({ currentArg, previousArg }) {
        return currentArg?.userId !== previousArg?.userId || currentArg?.seed !== previousArg?.seed;
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
