import { apiFetch } from "@/lib/apiFetch";
import { API_PREFIX } from "@/constants/appConstant";
import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { ManagedSubtitle, SubtitleCue } from "@/types/subtitle";

type SubtitleRef = { videoId: string; subtitleId: string };

/** Chủ video quản lý phụ đề (E2). Không cache lâu: trạng thái AI đổi trong lúc chờ. */
export const subtitleApi = createApi({
  reducerPath: "subtitleApi",
  baseQuery: fetchBaseQuery({
    baseUrl: `${API_PREFIX}/videos`,
    credentials: "include",
    fetchFn: apiFetch,
  }),
  tagTypes: ["Subtitles"],
  endpoints: (builder) => ({
    getManagedSubtitles: builder.query<ManagedSubtitle[], string>({
      query: (videoId) => `/${videoId}/subtitles/manage`,
      providesTags: (_r, _e, videoId) => [{ type: "Subtitles", id: videoId }],
      keepUnusedDataFor: 0,
    }),

    getSubtitle: builder.query<ManagedSubtitle, SubtitleRef>({
      query: ({ videoId, subtitleId }) => `/${videoId}/subtitles/${subtitleId}`,
      keepUnusedDataFor: 0,
    }),

    generateSubtitle: builder.mutation<ManagedSubtitle, { videoId: string; language: string }>({
      query: ({ videoId, language }) => ({ url: `/${videoId}/subtitles/ai?targetLang=${language}`, method: "POST" }),
      invalidatesTags: (_r, _e, { videoId }) => [{ type: "Subtitles", id: videoId }],
    }),

    saveSubtitle: builder.mutation<ManagedSubtitle, SubtitleRef & { cues: SubtitleCue[] }>({
      query: ({ videoId, subtitleId, cues }) => ({ url: `/${videoId}/subtitles/${subtitleId}`, method: "PUT", body: cues }),
      invalidatesTags: (_r, _e, { videoId }) => [{ type: "Subtitles", id: videoId }],
    }),

    publishSubtitle: builder.mutation<ManagedSubtitle, SubtitleRef>({
      query: ({ videoId, subtitleId }) => ({ url: `/${videoId}/subtitles/${subtitleId}/publish`, method: "POST" }),
      invalidatesTags: (_r, _e, { videoId }) => [{ type: "Subtitles", id: videoId }],
    }),

    unpublishSubtitle: builder.mutation<ManagedSubtitle, SubtitleRef>({
      query: ({ videoId, subtitleId }) => ({ url: `/${videoId}/subtitles/${subtitleId}/unpublish`, method: "POST" }),
      invalidatesTags: (_r, _e, { videoId }) => [{ type: "Subtitles", id: videoId }],
    }),

    deleteSubtitle: builder.mutation<void, SubtitleRef>({
      query: ({ videoId, subtitleId }) => ({ url: `/${videoId}/subtitles/${subtitleId}`, method: "DELETE" }),
      invalidatesTags: (_r, _e, { videoId }) => [{ type: "Subtitles", id: videoId }],
    }),
  }),
});

export const {
  useGetManagedSubtitlesQuery,
  useGetSubtitleQuery,
  useGenerateSubtitleMutation,
  useSaveSubtitleMutation,
  usePublishSubtitleMutation,
  useUnpublishSubtitleMutation,
  useDeleteSubtitleMutation,
} = subtitleApi;

/**
 * Đăng video (bản nháp → đã đăng): công khai luôn các phụ đề đã tạo xong (anh chốt E2).
 * Trả về số phụ đề còn đang tạo (ở lại bản nháp) và có lỗi khi công khai hay không.
 */
export async function publishDraftSubtitles(videoId: string): Promise<{ processing: number; failed: boolean }> {
  const res = await apiFetch(`${API_PREFIX}/videos/${videoId}/subtitles/manage`).catch(() => null);
  if (!res?.ok) return { processing: 0, failed: true };
  const subtitles: ManagedSubtitle[] = await res.json();
  const drafts = subtitles.filter((s) => s.status === "DRAFT");
  const results = await Promise.allSettled(
    drafts.map((s) =>
      apiFetch(`${API_PREFIX}/videos/${videoId}/subtitles/${s.id}/publish`, { method: "POST" }).then((r) => {
        if (!r.ok) throw new Error(String(r.status));
      })
    )
  );
  return {
    processing: subtitles.filter((s) => s.status === "PROCESSING").length,
    failed: results.some((r) => r.status === "rejected"),
  };
}
