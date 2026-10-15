import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { Post } from "@/types/post";
import { API_PREFIX } from "@/constants/appConstant";

export interface PollOptionDto {
  optionId: string;
  optionText: string;
  voteCount: number;
  percentage: number;
}

export interface PollSummaryDto {
  options: PollOptionDto[];
  totalVotes: number;
  userVotedOptionId?: string;
}

export interface VoteRequestDto {
  postId: string;
  optionId: string;
  userId: string;
}

export interface UploadResponse {
  url: string;
  error?: string;
}

export const postsApi = createApi({
  reducerPath: "postsApi",
  baseQuery: fetchBaseQuery({ baseUrl: `${API_PREFIX}` }),
  tagTypes: ["Post"],
  endpoints: (builder) => ({
    getPostsByUser: builder.query<Post[], string>({
      query: (userId) => `/posts/${userId}`,
      providesTags: (result, _error, _userId) =>
        result
          ? [
              ...result.map((post) => ({ type: "Post" as const, id: post.id })),
              { type: "Post", id: "LIST" },
            ]
          : [{ type: "Post", id: "LIST" }],
    }),

    createPost: builder.mutation<
      Post,
      {
        userId: string;
        content: string;
        imageUrl?: string | null;
        poll?: { options: { optionText: string }[] };
      }
    >({
      query: (body) => ({
        url: "/posts",
        method: "POST",
        body,
        headers: {
          "Content-Type": "application/json",
        },
      }),
      invalidatesTags: [{ type: "Post", id: "LIST" }],
    }),

    updatePost: builder.mutation<Post, { postId: string; body: Partial<Post> }>(
      {
        query: ({ postId, body }) => ({
          url: `/posts/${postId}`,
          method: "PUT",
          body,
        }),
        invalidatesTags: (result, error, { postId }) => [
          { type: "Post", id: postId },
        ],
      }
    ),

    deletePost: builder.mutation<void, string>({
      query: (postId) => ({
        url: `/posts/${postId}`,
        method: "DELETE",
      }),
      invalidatesTags: (result, error, postId) => [
        { type: "Post", id: postId },
        { type: "Post", id: "LIST" },
      ],
    }),

    votePoll: builder.mutation<PollSummaryDto, VoteRequestDto>({
      query: ({ postId, optionId, userId }) => ({
        url: `/posts/${postId}/polls/vote`,
        method: "POST",
        body: { optionId, userId },
      }),
      invalidatesTags: (result, error, { postId }) => [
        { type: "Post", id: postId },
      ],
    }),

    uploadImage: builder.mutation<UploadResponse, File>({
      query: (file) => {
        const formData = new FormData();
        formData.append("file", file);

        return {
          url: "/uploads/image",
          method: "POST",
          body: formData,
        };
      },
    }),
  }),
});

export const {
  useGetPostsByUserQuery,
  useCreatePostMutation,
  useUpdatePostMutation,
  useDeletePostMutation,
  useVotePollMutation,
  useUploadImageMutation,
} = postsApi;
