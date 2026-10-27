import { apiFetch } from "@/lib/apiFetch";
import { API_PREFIX } from "@/constants/appConstant";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";

/** id: SUBSCRIPTIONS | NEW | UNWATCHED | WATCHED, hoặc "topic:{id chủ đề}" (C8). */
export interface HomeChip {
  id: string;
  label: string;
}

export const homeApi = createApi({
  reducerPath: "homeApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}/home`,
    credentials: "include", fetchFn: apiFetch,
  }),
  endpoints: (builder) => ({
    /** Các chip đang có video; rỗng cả hàng nếu chưa có lịch sử xem. */
    getHomeChips: builder.query<HomeChip[], { seed: string }>({
      query: ({ seed }) => ({ url: "/chips", params: { seed } }),
    }),
  }),
});

export const { useGetHomeChipsQuery } = homeApi;
