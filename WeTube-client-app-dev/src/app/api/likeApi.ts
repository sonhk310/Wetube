import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";

export interface LikeInfo {
    targetId: string;
    targetType: "POST" | "COMMENT" | "VIDEO";
    likeCount: number;
    likedUserIds: string[];
    liked: boolean | null;
    /** Chỉ trạng thái của chính mình; backend không bao giờ trả số lượt không thích. */
    disliked?: boolean | null;
}

export interface ToggleLikeRequest {
    targetId: string;
    targetType: "POST" | "COMMENT" | "VIDEO";
    userId: string;
}

export const likesApi = createApi({
    reducerPath: "likesApi",
    baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}/likes`, credentials: "include" }),
    tagTypes: ["Like"],
    endpoints: (builder) => ({
        getLikeInfo: builder.query<
            LikeInfo,
            { targetId: string; targetType: "POST" | "COMMENT" | "VIDEO"; userId: string }
        >({
            query: ({ targetId, targetType, userId }) =>
                `?targetId=${targetId}&targetType=${targetType}&userId=${userId}`,
            providesTags: (result, _error, { targetId, targetType, userId }) => [
                { type: "Like", id: `${targetType}-${targetId}-${userId}` },
            ],
        }),

        toggleLike: builder.mutation<LikeInfo, ToggleLikeRequest>({
            query: ({ targetId, targetType, userId }) => ({
                url: "/toggle",
                method: "POST",
                params: { targetId, targetType, userId },
            }),
            // Đổi số ngay khi bấm; lỗi thì trả lại như cũ. Sau đó vẫn lấy lại số thật từ server.
            async onQueryStarted({ targetId, targetType, userId }, { dispatch, queryFulfilled }) {
                const patch = dispatch(
                    likesApi.util.updateQueryData("getLikeInfo", { targetId, targetType, userId }, (draft) => {
                        const liked = !draft.liked;
                        draft.liked = liked;
                        draft.likeCount = Math.max(0, draft.likeCount + (liked ? 1 : -1));
                        if (liked) draft.disliked = false;
                    })
                );
                try {
                    await queryFulfilled;
                } catch {
                    patch.undo();
                }
            },
            invalidatesTags: (_result, _error, { targetId, targetType, userId }) => [
                { type: "Like", id: `${targetType}-${targetId}-${userId}` },
            ],
        }),
    }),
});

export const { useGetLikeInfoQuery, useToggleLikeMutation } = likesApi;
