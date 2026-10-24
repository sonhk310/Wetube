import { apiFetch } from "@/lib/apiFetch";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";

export type TargetType = "POST" | "VIDEO" | "COMMENT";

/** top: nhiều thích + trả lời trước; newest: mới nhất trước. Bình luận đã ghim luôn ở đầu. */
export type CommentSort = "top" | "newest";

export interface Comment {
    id: string;
    userId: string;
    content: string;
    createdAt: string;
    updatedAt?: string;
    parentCommentId?: string;
    replyCount?: number;
    replies?: Comment[];
    likeCount?: number;
    /** Chủ video đã ghim. */
    pinned?: boolean;
    /** Chủ video đã thả tim. */
    hearted?: boolean;
    /** Người viết là chủ video. */
    byCreator?: boolean;

    user?: {
        id: string;
        name: string;
        picture?: string;
    };
}

export interface CreateCommentRequest {
    targetId: string;
    targetType: TargetType;
    userId: string;
    content: string;
    parentCommentId?: string;
}

export interface UpdateCommentRequest {
    id: string;
    userId: string;
    content: string;
    targetId: string;
    targetType: TargetType;
}

export const commentApi = createApi({
    reducerPath: "commentApi",
    baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}/comments`, credentials: "include", fetchFn: apiFetch }),
    tagTypes: ["Comment"],
    endpoints: (builder) => ({
        getCommentsByTarget: builder.query<Comment[], { targetId: string; targetType: TargetType; sort?: CommentSort }>({
            query: ({ targetId, targetType, sort }) => `/${targetType}/${targetId}${sort ? `?sort=${sort}` : ""}`,
            providesTags: (result, error, { targetId }) =>
                result
                    ? [
                        ...result.map(({ id }) => ({ type: "Comment" as const, id })),
                        { type: "Comment", id: `LIST-${targetId}` },
                    ]
                    : [{ type: "Comment", id: `LIST-${targetId}` }],
        }),

        getReplies: builder.query<Comment[], string>({
            query: (parentId) => `/reply/${parentId}`,
            providesTags: (result, error, parentId) =>
                result
                    ? [
                        ...result.map(({ id }) => ({ type: "Comment" as const, id })),
                        { type: "Comment", id: `REPLIES-${parentId}` },
                    ]
                    : [{ type: "Comment", id: `REPLIES-${parentId}` }],
        }),

        createComment: builder.mutation<Comment, CreateCommentRequest>({
            query: (body) => ({
                url: ``,
                method: "POST",
                body,
            }),
            invalidatesTags: (result, error, { targetId }) => [
                { type: "Comment", id: `LIST-${targetId}` },
            ],
        }),

        updateComment: builder.mutation<Comment, UpdateCommentRequest>({
            query: ({ id, ...body }) => ({
                url: `/${id}`,
                method: "PUT",
                body,
            }),
            invalidatesTags: (result, error, { targetId, id }) => [
                { type: "Comment", id },
                { type: "Comment", id: `LIST-${targetId}` },
            ],
        }),

        /** Chủ video ghim / bỏ ghim bình luận gốc (targetId = id video, để tải lại danh sách). */
        pinComment: builder.mutation<Comment, { id: string; targetId: string; pinned: boolean }>({
            query: ({ id, pinned }) => ({
                url: `/${id}/pin`,
                method: pinned ? "POST" : "DELETE",
            }),
            invalidatesTags: (result, error, { targetId }) => [{ type: "Comment", id: `LIST-${targetId}` }],
        }),

        /** Chủ video thả / bỏ tim. Thẻ bình luận tự cập nhật trạng thái, không tải lại danh sách. */
        heartComment: builder.mutation<Comment, { id: string; hearted: boolean }>({
            query: ({ id, hearted }) => ({
                url: `/${id}/heart`,
                method: hearted ? "POST" : "DELETE",
            }),
        }),

        deleteComment: builder.mutation<void, { id: string; targetId: string }>({
            query: ({ id }) => ({
                url: `/${id}`,
                method: "DELETE",
            }),
            invalidatesTags: (result, error, { id, targetId }) => [
                { type: "Comment", id },
                { type: "Comment", id: `LIST-${targetId}` },
            ],
        }),
    }),
});

export const {
    useGetCommentsByTargetQuery,
    useGetRepliesQuery,
    useCreateCommentMutation,
    useUpdateCommentMutation,
    useDeleteCommentMutation,
    usePinCommentMutation,
    useHeartCommentMutation,
} = commentApi;
