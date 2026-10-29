"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import axios from "axios";
import { useDispatch } from "react-redux";
import { Loader2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DialogTitle } from "@/components/ui/dialog";
import { useUpdateVideoDetailsMutation, videoApi } from "@/app/api/videoApi";
import { playlistApi } from "@/app/api/playlistApi";
import { publishDraftSubtitles } from "@/app/api/subtitleApi";
import { apiErrorMessage } from "@/lib/apiError";
import { isShortVideo, shortsReason } from "@/lib/shorts";
import { toastEmitter } from "@/lib/toastEmitter";
import { uploadDraft } from "@/lib/uploadDraft";
import { videoImageUrl } from "@/lib/videoMedia";
import { playlistService } from "@/modules/playlists/ui/list/playlist-API";
import { StepChecks } from "./step-checks";
import { StepDetails } from "./step-details";
import { StepElements } from "./step-elements";
import { StepVisibility } from "./step-visibility";
import { UploadPreview } from "./upload-preview";
import { UploadStepper } from "./upload-stepper";
import { UploadFooterStatus, UploadTopBar } from "./upload-status";
import type { FormErrors, StepId, UploadState, VideoMeta, WizardForm } from "./types";
import { confirmDialog } from "@/lib/dialogs";

const STEPS: { id: StepId; label: string }[] = [
  { id: "details", label: "Chi tiết" },
  { id: "elements", label: "Các thành phần của video" },
  { id: "checks", label: "Kiểm tra" },
  { id: "visibility", label: "Chế độ hiển thị" },
];

const MAX_THUMBNAIL_BYTES = 5 * 1024 * 1024;
// Giữ quy tắc thẻ của form cũ: "#the1 #the2"; nhận chữ có dấu (#âmnhạc)
const TAGS_PATTERN = /^(#[\p{L}\p{N}_]+(\s+#[\p{L}\p{N}_]+)*)$/u;

/** Dùng chung với trang Chi tiết video của Studio. `forDraft`: lưu bản nháp thì chưa cần chủ đề (C8). */
export function validate(form: WizardForm, forDraft = false): FormErrors {
  const errors: FormErrors = {};
  const title = form.title.trim();
  if (!title) errors.title = "Vui lòng nhập tiêu đề.";
  else if (title.length > 100) errors.title = "Tiêu đề tối đa 100 ký tự.";
  if (form.description.length > 5000) errors.description = "Mô tả tối đa 5000 ký tự.";
  const tags = form.tags.trim();
  if (tags.length > 200) errors.tags = "Thẻ tối đa 200 ký tự.";
  else if (tags && !TAGS_PATTERN.test(tags)) errors.tags = 'Thẻ phải có dạng "#the1 #the2"';
  if (form.thumbnailFile && form.thumbnailFile.size > MAX_THUMBNAIL_BYTES) {
    errors.thumbnailFile = "Ảnh thu nhỏ tối đa 5MB.";
  }
  if (!forDraft && !form.topicId) errors.topicId = "Hãy chọn chủ đề cho video.";
  return errors;
}

/** Chụp một khung hình làm ảnh thu nhỏ mặc định. */
function captureFrame(video: HTMLVideoElement): Promise<File> {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d");
    if (!ctx || !video.videoWidth) {
      reject(new Error("Không chụp được khung hình"));
      return;
    }
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(
      (blob) => (blob ? resolve(new File([blob], "thumbnail.jpg", { type: "image/jpeg" })) : reject(new Error())),
      "image/jpeg",
      0.8
    );
  });
}

/** Bản nháp đã có (mở lại từ Studio): không tải lên nữa, sửa tiếp rồi đăng. */
export interface DraftVideo {
  id: string;
  title: string;
  description: string;
  tags: string;
  searchKeywords: string;
  topicId: string;
  thumbnailUrl: string | null;
  videoUrl: string;
  isShort: boolean;
}

interface UploadWizardProps {
  /** Tệp mới chọn; hoặc `draft` khi mở lại bản nháp (đúng một trong hai). */
  file?: File;
  draft?: DraftVideo;
  /** Đóng hộp thoại (đã đăng, đã lưu bản nháp hoặc đã huỷ). */
  onClose: () => void;
}

/** Nội dung gửi PUT /videos/{id}; dùng chung với trang Chi tiết video của Studio. */
export function toFormData(form: WizardForm, status: string): FormData {
  const formData = new FormData();
  formData.append("title", form.title.trim());
  formData.append("description", form.description);
  formData.append("tags", form.tags.trim());
  formData.append("searchKeywords", form.searchKeywords);
  if (form.topicId) formData.append("topicId", form.topicId);
  formData.append("status", status);
  if (form.thumbnailFile) formData.append("thumbnailFile", form.thumbnailFile);
  return formData;
}

/**
 * Luồng tải lên 4 bước kiểu YouTube: tệp tải lên thành bản nháp ngay khi chọn, trong lúc đó người dùng
 * điền Chi tiết → Các thành phần → Kiểm tra → Chế độ hiển thị rồi bấm Đăng (PUT /videos/{id}).
 * Đóng giữa chừng thì lưu nội dung vào bản nháp; mở lại bản nháp từ Studio cũng dùng hộp thoại này.
 */
export function UploadWizard({ file, draft, onClose }: UploadWizardProps) {
  const dispatch = useDispatch();
  const [updateVideoDetails] = useUpdateVideoDetailsMutation();

  const [step, setStep] = useState<StepId>("details");
  // Bước xa nhất đã tới: chỉ được bấm tới các bước này (như YouTube)
  const [maxReached, setMaxReached] = useState(0);
  const [form, setFormState] = useState<WizardForm>(() => ({
    title: draft ? draft.title : (file?.name ?? "").replace(/\.[^/.]+$/, "").slice(0, 100),
    description: draft?.description ?? "",
    tags: draft?.tags ?? "",
    searchKeywords: draft?.searchKeywords ?? "",
    topicId: draft?.topicId ?? "",
    thumbnailFile: null,
    playlistIds: [],
    visibility: "ACTIVE",
  }));
  const setForm = useCallback((patch: Partial<WizardForm>) => setFormState((f) => ({ ...f, ...patch })), []);

  const [previewUrl, setPreviewUrl] = useState("");
  const [meta, setMeta] = useState<VideoMeta>({ duration: 0, width: 0, height: 0 });
  const [generatingThumbnail, setGeneratingThumbnail] = useState(!draft?.thumbnailUrl);
  const [upload, setUpload] = useState<UploadState>(
    draft ? { phase: "done", videoId: draft.id } : { phase: "uploading", percent: 0 }
  );
  const abortRef = useRef<AbortController | null>(null);
  const [publishRequested, setPublishRequested] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);

  const errors = useMemo(() => validate(form), [form]);
  const detailsHasError = Object.keys(errors).length > 0;

  const startUpload = useCallback(
    (m: VideoMeta) => {
      if (!file) return;
      abortRef.current?.abort();
      const controller = new AbortController();
      abortRef.current = controller;
      setUpload({ phase: "uploading", percent: 0 });
      const startedAt = Date.now();
      uploadDraft(
        file,
        m,
        (percent) => {
          if (controller.signal.aborted) return;
          if (percent >= 100) {
            setUpload({ phase: "processing" });
            return;
          }
          const elapsed = (Date.now() - startedAt) / 1000;
          const etaSeconds = percent > 0 ? (elapsed * (100 - percent)) / percent : undefined;
          setUpload({ phase: "uploading", percent, etaSeconds });
        },
        controller.signal
      )
        .then(({ id }) => {
          if (!controller.signal.aborted) setUpload({ phase: "done", videoId: id });
        })
        .catch((err) => {
          if (controller.signal.aborted || axios.isCancel(err)) return;
          setUpload({ phase: "error", message: apiErrorMessage(err, "Tải lên thất bại, vui lòng thử lại.") });
        });
    },
    [file]
  );

  // Bản nháp: đọc kích thước từ link video; chưa có ảnh thu nhỏ thì lấy khung hình Cloudinary cắt sẵn
  useEffect(() => {
    if (!draft) return;
    setPreviewUrl(draft.videoUrl);
    let cancelled = false;
    const video = document.createElement("video");
    video.preload = "metadata";
    video.onloadedmetadata = () => {
      setMeta({
        duration: Number.isFinite(video.duration) ? video.duration : 0,
        width: video.videoWidth,
        height: video.videoHeight,
      });
    };
    video.src = draft.videoUrl;
    if (!draft.thumbnailUrl) {
      const frame = videoImageUrl(null, draft.videoUrl);
      if (frame) {
        fetch(frame)
          .then((res) => (res.ok ? res.blob() : Promise.reject(new Error())))
          .then((blob) => {
            if (cancelled) return;
            const thumb = new File([blob], "thumbnail.jpg", { type: blob.type || "image/jpeg" });
            setFormState((f) => (f.thumbnailFile ? f : { ...f, thumbnailFile: thumb }));
          })
          .catch(() => undefined)
          .finally(() => !cancelled && setGeneratingThumbnail(false));
      } else {
        setGeneratingThumbnail(false);
      }
    }
    return () => {
      cancelled = true;
      video.onloadedmetadata = null;
      video.removeAttribute("src");
      video.load();
    };
  }, [draft]);

  // Đọc kích thước / thời lượng, bắt đầu tải lên, chụp ảnh thu nhỏ mặc định
  useEffect(() => {
    if (!file) return;
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    const video = document.createElement("video");
    video.preload = "auto";
    video.muted = true;
    let started = false;
    const begin = (m: VideoMeta) => {
      if (started) return;
      started = true;
      setMeta(m);
      startUpload(m);
    };
    video.onloadedmetadata = () => {
      const duration = Number.isFinite(video.duration) ? video.duration : 0;
      begin({ duration, width: video.videoWidth, height: video.videoHeight });
      if (duration > 0) video.currentTime = Math.min(duration / 2, 1);
      else setGeneratingThumbnail(false);
    };
    video.onseeked = () => {
      captureFrame(video)
        .then((thumb) => setFormState((f) => (f.thumbnailFile ? f : { ...f, thumbnailFile: thumb })))
        .catch(() => undefined)
        .finally(() => setGeneratingThumbnail(false));
    };
    video.onerror = () => {
      begin({ duration: 0, width: 0, height: 0 });
      setGeneratingThumbnail(false);
    };
    video.src = url;
    return () => {
      // Gỡ handler trước khi dừng tải tệp, để thẻ video tạm không bắt đầu thêm một lượt tải lên
      video.onloadedmetadata = null;
      video.onseeked = null;
      video.onerror = null;
      video.removeAttribute("src");
      video.load();
      abortRef.current?.abort();
      URL.revokeObjectURL(url);
    };
  }, [file, startUpload]);

  const fileThumbnail = useMemo(
    () => (form.thumbnailFile ? URL.createObjectURL(form.thumbnailFile) : ""),
    [form.thumbnailFile]
  );
  useEffect(() => () => (fileThumbnail ? URL.revokeObjectURL(fileThumbnail) : undefined), [fileThumbnail]);
  const thumbnailPreview = fileThumbnail || draft?.thumbnailUrl || "";

  // Bản nháp đã được backend xếp loại Shorts / video thường lúc tải lên
  const isShort = draft ? draft.isShort : isShortVideo(meta.duration, meta.width, meta.height);
  const reason = shortsReason(meta.duration, meta.width, meta.height);

  const publish = useCallback(
    async (videoId: string) => {
      setPublishing(true);
      try {
        await updateVideoDetails({ videoId, formData: toFormData(form, form.visibility) }).unwrap();
      } catch (err) {
        toastEmitter.error(apiErrorMessage(err, "Không đăng được video, vui lòng thử lại."));
        setPublishing(false);
        return;
      }

      // Video đã đăng; lỗi ở các bước sau chỉ báo lại, không huỷ việc đăng
      const failed: string[] = [];
      for (const playlistId of form.playlistIds) {
        try {
          await playlistService.addVideo(playlistId, videoId);
        } catch {
          if (!failed.includes("danh sách phát")) failed.push("danh sách phát");
        }
      }
      // Phụ đề AI đã tạo xong (bản nháp) công khai cùng video; còn đang tạo thì ở lại bản nháp
      const subtitles = await publishDraftSubtitles(videoId);
      if (subtitles.failed) failed.push("phụ đề");
      if (failed.length) {
        toastEmitter.error(`Video đã đăng, nhưng lỗi ở ${failed.join(" và ")}, hãy kiểm tra lại trong Studio.`);
      } else if (subtitles.processing > 0) {
        toastEmitter.success("Đã đăng video. Phụ đề đang tạo sẽ là bản nháp, xem và công khai trong Studio.");
      } else {
        toastEmitter.success("Đã đăng video");
      }
      dispatch(playlistApi.util.invalidateTags(["Playlist"]));
      dispatch(videoApi.util.invalidateTags(["VideoList"]));
      onClose();
    },
    [form, updateVideoDetails, dispatch, onClose]
  );

  // Bấm Đăng khi tệp chưa tải lên xong: đăng ngay khi có bản nháp
  useEffect(() => {
    if (!publishRequested) return;
    if (upload.phase === "done" && !publishing) {
      setPublishRequested(false);
      publish(upload.videoId);
    } else if (upload.phase === "error") {
      setPublishRequested(false);
    }
  }, [publishRequested, upload, publishing, publish]);

  const handlePublish = () => {
    if (detailsHasError) {
      setStep("details");
      toastEmitter.error("Vui lòng sửa các lỗi ở bước Chi tiết.");
      return;
    }
    if (upload.phase === "done") publish(upload.videoId);
    else if (upload.phase === "error") toastEmitter.error("Tệp chưa tải lên được, hãy bấm Thử lại.");
    else setPublishRequested(true);
  };

  const handleClose = async () => {
    if (publishing || savingDraft) return;
    if (upload.phase === "uploading" || upload.phase === "processing") {
      const discard = await confirmDialog({
        title: "Huỷ tải lên?",
        message: "Video sẽ không được lưu.",
        confirmLabel: "Huỷ tải lên",
        cancelLabel: "Tiếp tục tải lên",
        destructive: true,
      });
      if (!discard) return;
      abortRef.current?.abort();
    } else if (upload.phase === "done") {
      // Như YouTube: đóng là lưu những gì đã điền vào bản nháp (vẫn là bản nháp, chưa đăng)
      if (Object.keys(validate(form, true)).length > 0) {
        const closeAnyway = await confirmDialog({
          title: "Vẫn đóng?",
          message: "Các thay đổi đang có lỗi ở bước Chi tiết sẽ không được lưu.",
          confirmLabel: "Đóng",
          cancelLabel: "Ở lại",
          destructive: true,
        });
        if (!closeAnyway) return;
      } else {
        setSavingDraft(true);
        try {
          await updateVideoDetails({ videoId: upload.videoId, formData: toFormData(form, "DRAFT") }).unwrap();
        } catch (err) {
          setSavingDraft(false);
          toastEmitter.error(apiErrorMessage(err, "Không lưu được bản nháp, vui lòng thử lại."));
          return;
        }
        setSavingDraft(false);
      }
      toastEmitter.success("Đã lưu bản nháp trong Studio");
      // Studio lấy danh sách từ playlist "đã tải lên": phải làm mới cả cache Playlist
      dispatch(playlistApi.util.invalidateTags(["Playlist"]));
      dispatch(videoApi.util.invalidateTags(["VideoList"]));
    }
    onClose();
  };

  const stepIndex = STEPS.findIndex((s) => s.id === step);
  const isLast = stepIndex === STEPS.length - 1;
  const canOpen = (index: number) => index <= maxReached && (index === 0 || !detailsHasError);

  const goNext = () => {
    if (step === "details" && detailsHasError) {
      toastEmitter.error("Vui lòng sửa các lỗi ở bước Chi tiết trước.");
      return;
    }
    const next = stepIndex + 1;
    setStep(STEPS[next].id);
    setMaxReached((m) => Math.max(m, next));
  };
  const waitingToPublish = publishRequested && upload.phase !== "done";

  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between px-6 py-4">
        <DialogTitle className="truncate pr-4 text-xl font-medium text-black">
          {form.title.trim() || "Tải video lên"}
        </DialogTitle>
        <div className="flex shrink-0 items-center gap-3">
          {upload.phase === "done" && (
            <span className="rounded bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700">Đã lưu ở dạng bản nháp</span>
          )}
          <Button
            variant="ghost"
            size="icon"
            className="rounded-full"
            onClick={handleClose}
            disabled={publishing || savingDraft}
          >
            {savingDraft ? <Loader2 className="h-5 w-5 animate-spin" /> : <X className="h-6 w-6" />}
            <span className="sr-only">Đóng</span>
          </Button>
        </div>
      </div>
      <UploadTopBar upload={upload} />

      <UploadStepper
        steps={STEPS.map((s, i) => ({ ...s, error: s.id === "details" && detailsHasError, disabled: !canOpen(i) }))}
        current={step}
        onSelect={(id) => canOpen(STEPS.findIndex((s) => s.id === id)) && setStep(id)}
      />

      <div className="grid min-h-0 flex-1 grid-cols-1 gap-6 overflow-y-auto p-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          {step === "details" && (
            <StepDetails
              form={form}
              setForm={setForm}
              errors={errors}
              thumbnailPreview={thumbnailPreview}
              generatingThumbnail={generatingThumbnail}
              isShort={isShort}
            />
          )}
          {step === "elements" && (
            <StepElements
              videoId={upload.phase === "done" ? upload.videoId : undefined}
              videoSrc={previewUrl}
              videoIsDraft
            />
          )}
          {step === "checks" && (
            <StepChecks
              errors={errors}
              upload={upload}
              hasThumbnail={!!thumbnailPreview}
              isShort={isShort}
              shortsReason={reason}
              goTo={setStep}
            />
          )}
          {step === "visibility" && <StepVisibility form={form} setForm={setForm} />}
        </div>
        {/* Khung xem trước đứng yên khi cột trái cuộn */}
        <div className="lg:sticky lg:top-0 lg:col-span-1 lg:self-start">
          <UploadPreview
            src={previewUrl}
            isShort={isShort}
            reason={reason}
            fileName={file?.name ?? ""}
            videoId={upload.phase === "done" ? upload.videoId : undefined}
          />
        </div>
      </div>

      <div className="flex items-center gap-4 border-t border-gray-200 px-6 py-3">
        <UploadFooterStatus
          upload={upload}
          height={Math.min(meta.width, meta.height)}
          checksOk={!detailsHasError}
          onRetry={() => startUpload(meta)}
        />
        {stepIndex > 0 && (
          <Button variant="ghost" className="rounded-full" onClick={() => setStep(STEPS[stepIndex - 1].id)} disabled={publishing}>
            Quay lại
          </Button>
        )}
        {isLast ? (
          <Button
            onClick={handlePublish}
            disabled={publishing || waitingToPublish}
            className="rounded-full bg-blue-600 px-5 text-white hover:bg-blue-700"
          >
            {publishing || waitingToPublish ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
            {upload.phase === "done" ? "Đăng" : "Đăng khi tải lên xong"}
          </Button>
        ) : (
          <Button onClick={goNext} className="rounded-full bg-blue-600 px-5 text-white hover:bg-blue-700">
            Tiếp
          </Button>
        )}
      </div>
    </div>
  );
}
