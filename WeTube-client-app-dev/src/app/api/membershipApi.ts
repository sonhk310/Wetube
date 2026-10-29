import { apiFetch } from "@/lib/apiFetch";
import { API_PREFIX } from "@/constants/appConstant";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { ChannelTiers, OwnerMembers, TierInput } from "@/types/membership";

/**
 * Hội viên kênh (M1). Không cache lâu: hội viên và doanh thu đổi ngay sau khi mua, nên truy vấn đặt
 * keepUnusedDataFor: 0.
 */
export const membershipApi = createApi({
  reducerPath: "membershipApi",
  baseQuery: fetchBaseQuery({
    baseUrl: API_PREFIX,
    credentials: "include",
    fetchFn: apiFetch,
  }),
  tagTypes: ["OwnerMembers"],
  endpoints: (builder) => ({
    /** Các cấp trả phí đang nhận hội viên của kênh + hội viên hiện tại của người xem. */
    getChannelTiers: builder.query<ChannelTiers, string>({
      query: (channelId) => `/channels/${channelId}/tiers`,
      keepUnusedDataFor: 0,
    }),

    /** Studio: các cấp, hội viên còn hạn và doanh thu của kênh mình. */
    getMyMembers: builder.query<OwnerMembers, void>({
      query: () => "/channels/me/members",
      providesTags: ["OwnerMembers"],
      keepUnusedDataFor: 0,
    }),

    createTier: builder.mutation<void, { channelId: string; input: TierInput }>({
      query: ({ channelId, input }) => ({ url: `/tiers/${channelId}`, method: "POST", body: input }),
      invalidatesTags: ["OwnerMembers"],
    }),

    updateTier: builder.mutation<void, { tierId: string; input: TierInput }>({
      query: ({ tierId, input }) => ({ url: `/tiers/${tierId}`, method: "PUT", body: input }),
      invalidatesTags: ["OwnerMembers"],
    }),

    /** archived: cấp đã có người mua nên chỉ ngừng nhận hội viên mới, không xoá thật. */
    deleteTier: builder.mutation<{ archived: boolean }, string>({
      query: (tierId) => ({ url: `/tiers/${tierId}`, method: "DELETE" }),
      invalidatesTags: ["OwnerMembers"],
    }),
  }),
});

export const {
  useGetChannelTiersQuery,
  useGetMyMembersQuery,
  useCreateTierMutation,
  useUpdateTierMutation,
  useDeleteTierMutation,
} = membershipApi;
