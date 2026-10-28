import { apiFetch } from "@/lib/apiFetch";
import { API_PREFIX } from "@/constants/appConstant";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type {
  ChannelLives,
  CreateLiveBody,
  LiveCard,
  LiveChatMessage,
  LiveTokenResponse,
  LiveView,
  PollView,
  UpdateLiveBody,
} from "@/modules/live/types";

/**
 * Phát trực tiếp. Mọi truy vấn đặt keepUnusedDataFor: 0 vì trạng thái buổi phát (đang phát, số người xem, Poll) đổi
 * liên tục; cache cũ sẽ hiện sai.
 */
export const liveApi = createApi({
  reducerPath: "liveApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}/live`,
    credentials: "include",
    fetchFn: apiFetch,
  }),
  tagTypes: ["MyLive"],
  endpoints: (builder) => ({
    getLive: builder.query<LiveView, string>({
      query: (id) => `/${id}`,
      keepUnusedDataFor: 0,
    }),

    /** Các buổi đang phát, nhiều người xem trước. */
    getLiveList: builder.query<LiveCard[], void>({
      query: () => "",
      keepUnusedDataFor: 0,
    }),

    getChannelLives: builder.query<ChannelLives, { channelId: string; page?: number; size?: number }>({
      query: ({ channelId, page = 0, size = 12 }) => `/channel/${channelId}?page=${page}&size=${size}`,
      keepUnusedDataFor: 0,
    }),

    /** Buổi chưa kết thúc của kênh người gọi (kèm Ingress); 204 → null. */
    getMyLive: builder.query<LiveView | null, void>({
      query: () => "/me",
      providesTags: ["MyLive"],
      keepUnusedDataFor: 0,
    }),

    getLiveChat: builder.query<LiveChatMessage[], string>({
      query: (id) => `/${id}/chat`,
      keepUnusedDataFor: 0,
    }),

    createLive: builder.mutation<LiveView, CreateLiveBody>({
      query: (body) => ({ url: "", method: "POST", body }),
      invalidatesTags: ["MyLive"],
    }),

    updateLive: builder.mutation<LiveView, UpdateLiveBody>({
      query: ({ id, ...body }) => ({ url: `/${id}`, method: "PATCH", body }),
      invalidatesTags: ["MyLive"],
    }),

    /** Đặt / đổi ảnh thu nhỏ (PNG hoặc JPEG, tối đa 2 MB) khi buổi chưa kết thúc. */
    uploadLiveThumbnail: builder.mutation<LiveView, { id: string; file: File }>({
      query: ({ id, file }) => {
        const formData = new FormData();
        formData.append("file", file);
        return { url: `/${id}/thumbnail`, method: "POST", body: formData };
      },
      invalidatesTags: ["MyLive"],
    }),

    /** Webcam: báo đã đăng luồng để chuyển sang LIVE ngay (không đợi job 15 giây). */
    markLiveStarted: builder.mutation<null, string>({
      query: (id) => ({ url: `/${id}/started`, method: "POST" }),
      invalidatesTags: ["MyLive"],
    }),

    endLive: builder.mutation<null, string>({
      query: (id) => ({ url: `/${id}/end`, method: "POST" }),
      invalidatesTags: ["MyLive"],
    }),

    /**
     * Mutation vì mỗi lần gọi cấp một token mới; không bao giờ để token trên URL. `publish`: chỉ trang phát của chủ kênh
     * (webcam) xin token phát; trang xem, kể cả của chính chủ kênh, xin token xem. Cùng identity với trang phát thì
     * LiveKit ngắt trang phát.
     */
    getLiveToken: builder.mutation<LiveTokenResponse, { id: string; publish?: boolean }>({
      query: ({ id, publish }) => ({ url: `/${id}/token${publish ? "?publish=true" : ""}`, method: "POST" }),
    }),

    openPoll: builder.mutation<PollView, { id: string; question: string; options: string[] }>({
      query: ({ id, ...body }) => ({ url: `/${id}/polls`, method: "POST", body }),
    }),

    closePoll: builder.mutation<PollView, { id: string; pollId: string }>({
      query: ({ id, pollId }) => ({ url: `/${id}/polls/${pollId}/close`, method: "POST" }),
    }),

    votePoll: builder.mutation<PollView, { id: string; pollId: string; option: number }>({
      query: ({ id, pollId, option }) => ({
        url: `/${id}/polls/${pollId}/vote`,
        method: "POST",
        body: { option },
      }),
    }),
  }),
});

export const {
  useGetLiveQuery,
  useGetLiveListQuery,
  useGetChannelLivesQuery,
  useGetMyLiveQuery,
  useGetLiveChatQuery,
  useLazyGetLiveChatQuery,
  useCreateLiveMutation,
  useUpdateLiveMutation,
  useUploadLiveThumbnailMutation,
  useMarkLiveStartedMutation,
  useEndLiveMutation,
  useGetLiveTokenMutation,
  useOpenPollMutation,
  useClosePollMutation,
  useVotePollMutation,
} = liveApi;
