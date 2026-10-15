import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { VideoInfo } from "../../types/managerVideoTypes/videoInfo";

export const videoApi = createApi({
    reducerPath: "videoApi",
    baseQuery: fetchBaseQuery({
        baseUrl: "https://68762e68814c0dfa653b06a0.mockapi.io",
    }),

    tagTypes: ["videoInfos", "video"],

    endpoints: (builder) => ({
        getVideos: builder.query<VideoInfo[], void>({
            query: () => "videos",
            providesTags: ["videoInfos"],
        }),
        // THAY ĐỔI Ở ĐÂY
        updateVideo: builder.mutation<VideoInfo, { id: string; data: Partial<VideoInfo> }>({
            query: ({ id, data }) => ({
                url: `videos/${id}`,
                method: "PUT",
                body: data,
            }),
            invalidatesTags: ["videoInfos", "video"],
        }),

        addVideo: builder.mutation<VideoInfo, Partial<VideoInfo>>({
            query: (data) => ({
                url: "videos",
                method: "POST",
                body: data,
            }),
            invalidatesTags: ["video"]
        }),
        getVideoInfo: builder.query<VideoInfo, string>({
            query: (id) => `videos/${id}`,
            providesTags: ["videoInfos"],
        }),
        deleteVideo: builder.mutation<{ success: boolean }, string>({
            query: (id) => ({
                url: `videos/${id}`,
                method: "DELETE",
            }),
            invalidatesTags: ["video"],
        }),
    }),
});

export const {
    useGetVideosQuery,
    useUpdateVideoMutation,
    useAddVideoMutation,
    useDeleteVideoMutation,
    useGetVideoInfoQuery
} = videoApi;