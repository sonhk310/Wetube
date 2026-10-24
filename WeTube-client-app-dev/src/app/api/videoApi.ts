// app/store/services/videoApi.ts
import { apiFetch } from "@/lib/apiFetch";
import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {VideoDetailResponseDto, VideoFormDetail} from "@/types/video";
import {API_PREFIX} from "@/constants/appConstant";
import type {LikeInfo} from "@/app/api/likeApi";

export interface PopularTag {
    name: string;
    count: number;
}

export type UpdateVideoPayload = Partial<
    Pick<VideoFormDetail, "title" | "description" | "status" | "tags">
> & { id: string };

export const videoApi = createApi({
    reducerPath: "videoApi",
    baseQuery: fetchBaseQuery({
        baseUrl: `${API_PREFIX}`,
        credentials: "include", fetchFn: apiFetch,
    }),
    tagTypes: ["VideoList", "VideoDetail", "Playlist"],
    endpoints: (builder) => ({
        getVideoDetail: builder.query<
            VideoDetailResponseDto,
            { videoId: string }
        >({
            query: ({videoId}) => `/videos/${videoId}/detail`,
            // Không giữ cache: mỗi lần mở trang xem phải có vị trí đang xem (resumeAt), lượt xem, bình luận mới nhất
            keepUnusedDataFor: 0,
        }),

        getVideoFormDetails: builder.query<VideoFormDetail, string>({
            query: (videoId) => `/videos/${videoId}/form-details`,
            providesTags: (result, error, id) => [{type: "VideoDetail", id}],
        }),

        updateVideoDetails: builder.mutation<
            void,
            { videoId: string; formData: FormData }
        >({
            query: ({videoId, formData}) => ({
                url: `/videos/${videoId}`,
                method: "PUT",
                body: formData,
            }),
            invalidatesTags: (result, error, {videoId}) => [
                {type: "VideoDetail", id: videoId},
                "VideoList",
                "Playlist",
            ],
        }),

        deleteVideo: builder.mutation<void, string>({
            query: (videoId) => ({
                url: `/videos/${videoId}`,
                method: "DELETE",
            }),
            invalidatesTags: ["VideoList", "Playlist"],
        }),

        toggleDislike: builder.mutation<LikeInfo, string>({
            query: (videoId) => ({
                url: `/videos/${videoId}/dislike`,
                method: "POST",
            }),
        }),

        getPopularTags: builder.query<PopularTag[], void>({
            query: () => `/tags/popular?limit=12`,
        }),
    }),
});

export const {
    useGetVideoDetailQuery,
    useGetVideoFormDetailsQuery,
    useUpdateVideoDetailsMutation,
    useDeleteVideoMutation,
    useToggleDislikeMutation,
    useGetPopularTagsQuery,
} = videoApi;
