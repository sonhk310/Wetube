import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";

export interface LikeInfo {
    targetId: string;
    targetType: "POST" | "COMMENT" | "VIDEO";
    likeCount: number;
    likedUserIds: string[];
    liked: boolean | null;
}

export interface ToggleLikeRequest {
    targetId: string;
    targetType: "POST" | "COMMENT" | "VIDEO";
    userId: string;
}

export const likesApi = createApi({
    reducerPath: "likesApi",
    baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}/likes` }),
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
            invalidatesTags: (_result, _error, { targetId, targetType, userId }) => [
                { type: "Like", id: `${targetType}-${targetId}-${userId}` },
            ],
        }),
    }),
});

export const { useGetLikeInfoQuery, useToggleLikeMutation } = likesApi;
