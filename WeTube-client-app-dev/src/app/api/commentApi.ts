import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { API_PREFIX } from "@/constants/appConstant";

export type TargetType = "POST" | "VIDEO" | "COMMENT";

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
    baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}/comments` }),
    tagTypes: ["Comment"],
    endpoints: (builder) => ({
        getCommentsByTarget: builder.query<Comment[], { targetId: string; targetType: TargetType }>({
            query: ({ targetId, targetType }) => `/${targetType}/${targetId}`,
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
} = commentApi;
