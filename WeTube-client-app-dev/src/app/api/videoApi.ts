// app/store/services/videoApi.ts
import {createApi, fetchBaseQuery} from "@reduxjs/toolkit/query/react";
import {Video as VideoListItem, VideoDetailResponseDto, VideoFormDetail, Video} from "@/types/video";
import {API_PREFIX} from "@/constants/appConstant";

export type UpdateVideoPayload = Partial<
    Pick<VideoFormDetail, "title" | "description" | "status" | "tags">
> & { id: string };

export const videoApi = createApi({
    reducerPath: "videoApi",
    baseQuery: fetchBaseQuery({
        baseUrl: `${API_PREFIX}`,
        credentials: "include",
    }),
    tagTypes: ["VideoList", "VideoDetail", "Playlist"],
    endpoints: (builder) => ({
        getVideos: builder.query<
            Video[],
            { categoryId?: string; page?: number; limit?: number }
        >({
            query: ({categoryId = "", page = 1, limit = 6}) =>
                `/videos?categoryId=${categoryId}&page=${page}&limit=${limit}`,
            providesTags: ["VideoList"],
        }),

        getVideoDetail: builder.query<
            VideoDetailResponseDto,
            { videoId: string }
        >({
            query: ({videoId}) => `/videos/${videoId}/detail`,
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

        uploadVideo: builder.mutation<
            { message: string; videoId?: string },
            FormData
        >({
            query: (formData) => ({
                url: "/videos/uploadFile",
                method: "POST",
                body: formData,
            }),
            invalidatesTags: ["VideoList", "Playlist"],
        }),
    }),
});

export const {
    useGetVideosQuery,
    useGetVideoDetailQuery,
    useGetVideoFormDetailsQuery,
    useUpdateVideoDetailsMutation,
    useUploadVideoMutation,
} = videoApi;
