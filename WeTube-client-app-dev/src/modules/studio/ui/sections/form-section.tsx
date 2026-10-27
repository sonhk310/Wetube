"use client";

import { useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useGetVideoFormDetailsQuery, useUpdateVideoDetailsMutation, videoApi } from "@/app/api/videoApi";
import { playlistApi } from "@/app/api/playlistApi";
import { API_PREFIX } from "@/constants/appConstant";
import { useAuth } from "@/contexts/auth-context";
import { apiErrorMessage } from "@/lib/apiError";
import { apiFetch } from "@/lib/apiFetch";
import { shortsReason } from "@/lib/shorts";
import { toastEmitter } from "@/lib/toastEmitter";
import { videoImageUrl } from "@/lib/videoMedia";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { StepDetails } from "@/components/upload/step-details";
import { StepElements } from "@/components/upload/step-elements";
import { StepVisibility } from "@/components/upload/step-visibility";
import { UploadPreview } from "@/components/upload/upload-preview";
import { toFormData, validate } from "@/components/upload/upload-wizard";
import type { VideoMeta, Visibility, WizardForm } from "@/components/upload/types";
import type { VideoFormDetail } from "@/types/video";

interface FormSectionProps {
  readonly videoId: string;
}

/** Trang Chi tiết video của Studio: cùng các phần như hộp thoại tải lên (Chi tiết, Phụ đề AI, Chế độ hiển thị). */
export default function FormSection({ videoId }: FormSectionProps) {
  const { isLoading: isAuthLoading } = useAuth();
  const { data: video, isLoading, error } = useGetVideoFormDetailsQuery(videoId, { skip: isAuthLoading });

  if (isLoading || isAuthLoading) return <FormSectionSkeleton />;
  if (error || !video) return <p className="text-red-500">Không tải được thông tin video.</p>;
  return <VideoDetailsEditor key={video.id} video={video} />;
}

const toVisibility = (status: VideoFormDetail["status"]): Visibility =>
  status === "UNLISTED" || status === "PRIVATE" ? status : "ACTIVE";

const sameIds = (a: string[], b: string[]) => a.length === b.length && a.every((id) => b.includes(id));

function VideoDetailsEditor({ video }: { video: VideoFormDetail }) {
  const dispatch = useDispatch();
  const [updateVideoDetails] = useUpdateVideoDetailsMutation();
  const isDraft = video.status === "DRAFT";
  const isHidden = video.status === "INACTIVE";

  // Giá trị đang lưu trên server, để biết có thay đổi chưa và khôi phục khi bấm Huỷ thay đổi
  const [baseline, setBaseline] = useState<WizardForm>(() => ({
    title: video.title,
    description: video.description ?? "",
    tags: video.tags.map((t) => `#${t.name}`).join(" "),
    searchKeywords: video.searchKeywords ?? "",
    topicId: video.topicId ?? "",
    thumbnailFile: null,
    playlistIds: [],
    targetLang: "",
    visibility: toVisibility(video.status),
  }));
  const [form, setFormState] = useState<WizardForm>(baseline);
  const setForm = (patch: Partial<WizardForm>) => setFormState((f) => ({ ...f, ...patch }));
  const [meta, setMeta] = useState<VideoMeta>({ duration: video.duration, width: 0, height: 0 });
  const [saving, setSaving] = useState(false);

  // Danh sách phát đang chứa video
  useEffect(() => {
    let cancelled = false;
    playlistService
      .getContaining(video.id)
      .then((ids) => {
        if (cancelled) return;
        setBaseline((b) => ({ ...b, playlistIds: ids }));
        setFormState((f) => ({ ...f, playlistIds: ids }));
      })
      .catch(() => toastEmitter.error("Không tải được danh sách phát chứa video"));
    return () => {
      cancelled = true;
    };
  }, [video.id]);

  // Kích thước video, để hiện "Dọc · 0:45" / "Ngang"
  useEffect(() => {
    const el = document.createElement("video");
    el.preload = "metadata";
    el.onloadedmetadata = () =>
      setMeta({
        duration: Number.isFinite(el.duration) ? el.duration : video.duration,
        width: el.videoWidth,
        height: el.videoHeight,
      });
    el.src = video.videoUrl;
    return () => {
      el.onloadedmetadata = null;
      el.removeAttribute("src");
      el.load();
    };
  }, [video.videoUrl, video.duration]);

  const fileThumbnail = useMemo(
    () => (form.thumbnailFile ? URL.createObjectURL(form.thumbnailFile) : ""),
    [form.thumbnailFile]
  );
  useEffect(() => () => (fileThumbnail ? URL.revokeObjectURL(fileThumbnail) : undefined), [fileThumbnail]);
  const thumbnailPreview = fileThumbnail || videoImageUrl(video.thumbnailUrl, video.videoUrl);

  const errors = useMemo(() => validate(form), [form]);
  const hasErrors = Object.keys(errors).length > 0;
  const dirty =
    isDraft ||
    form.title !== baseline.title ||
    form.description !== baseline.description ||
    form.tags !== baseline.tags ||
    form.searchKeywords !== baseline.searchKeywords ||
    form.topicId !== baseline.topicId ||
    form.visibility !== baseline.visibility ||
    !!form.thumbnailFile ||
    !!form.targetLang ||
    !sameIds(form.playlistIds, baseline.playlistIds);

  const save = async () => {
    if (hasErrors) {
      toastEmitter.error("Vui lòng sửa các lỗi trước khi lưu.");
      return;
    }
    setSaving(true);
    try {
      await updateVideoDetails({ videoId: video.id, formData: toFormData(form, form.visibility) }).unwrap();
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không lưu được thay đổi, vui lòng thử lại."));
      setSaving(false);
      return;
    }

    // Nội dung đã lưu; lỗi ở các bước sau chỉ báo lại
    const failed: string[] = [];
    const added = form.playlistIds.filter((id) => !baseline.playlistIds.includes(id));
    const removed = baseline.playlistIds.filter((id) => !form.playlistIds.includes(id));
    const playlistResults = await Promise.allSettled([
      ...added.map((id) => playlistService.addVideo(id, video.id)),
      ...removed.map((id) => playlistService.removeVideo(id, video.id)),
    ]);
    if (playlistResults.some((r) => r.status === "rejected")) failed.push("danh sách phát");
    if (form.targetLang) {
      const res = await apiFetch(`${API_PREFIX}/videos/${video.id}/subtitles/ai?targetLang=${form.targetLang}`, {
        method: "POST",
      }).catch(() => null);
      if (!res?.ok) failed.push("phụ đề AI");
    }
    if (failed.length) toastEmitter.error(`Đã lưu, nhưng không cập nhật được ${failed.join(" và ")}.`);
    else toastEmitter.success(isDraft ? "Đã đăng video" : "Đã lưu thay đổi");

    const saved = { ...form, thumbnailFile: null, targetLang: "" };
    setBaseline(saved);
    setFormState(saved);
    dispatch(playlistApi.util.invalidateTags(["Playlist"]));
    dispatch(videoApi.util.invalidateTags(["VideoList"]));
    setSaving(false);
  };

  return (
    <div className="flex h-full max-h-[calc(100vh-80px)] flex-col">
      <div className="flex shrink-0 items-center justify-between gap-4 border-b pb-4">
        <h1 className="text-2xl font-bold">Chi tiết video</h1>
        <div className="flex items-center gap-2">
          {dirty && !isDraft && (
            <Button variant="ghost" className="rounded-full" onClick={() => setFormState(baseline)} disabled={saving}>
              Huỷ thay đổi
            </Button>
          )}
          <Button
            onClick={save}
            disabled={!dirty || saving}
            className="rounded-full bg-blue-600 px-5 text-white hover:bg-blue-700"
          >
            {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isDraft ? "Đăng" : "Lưu"}
          </Button>
        </div>
      </div>

      {/* Cả trang cuộn chung một khung; khung xem trước đứng yên bên phải */}
      <div className="grid min-h-0 flex-1 grid-cols-1 gap-8 overflow-y-auto pb-12 pt-6 lg:grid-cols-3">
        <div className="space-y-10 lg:col-span-2">
          <StepDetails
            form={form}
            setForm={setForm}
            errors={errors}
            thumbnailPreview={thumbnailPreview}
            generatingThumbnail={false}
            isShort={!!video.isShort}
            showHeading={false}
          />
          <StepElements form={form} setForm={setForm} />
          {isHidden ? (
            <div className="space-y-2">
              <h2 className="text-xl font-semibold">Chế độ hiển thị</h2>
              <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">
                Video đã bị quản trị viên ẩn do vi phạm, bạn không thể tự công khai lại.
              </p>
            </div>
          ) : (
            <StepVisibility form={form} setForm={setForm} />
          )}
        </div>
        <div className="lg:sticky lg:top-0 lg:col-span-1 lg:self-start">
          <UploadPreview
            src={video.videoUrl}
            isShort={!!video.isShort}
            reason={shortsReason(meta.duration, meta.width, meta.height)}
            fileName=""
            videoId={video.id}
          />
        </div>
      </div>
    </div>
  );
}

const FormSectionSkeleton = () => (
  <div className="flex h-full max-h-[calc(100vh-80px)] flex-col">
    <div className="mb-4 flex items-center justify-between">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-10 w-20 rounded-full" />
    </div>
    <div className="grid flex-1 grid-cols-1 gap-8 lg:grid-cols-3">
      <div className="space-y-6 lg:col-span-2">
        <Skeleton className="h-20 w-full" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="aspect-video w-60" />
      </div>
      <div className="lg:col-span-1">
        <Skeleton className="aspect-video w-full" />
        <Skeleton className="mt-3 h-24 w-full" />
      </div>
    </div>
  </div>
);
