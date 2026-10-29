"use client";

import { useEffect, useState } from "react";
import { Loader2, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  useDeleteSubtitleMutation,
  useGenerateSubtitleMutation,
  useGetManagedSubtitlesQuery,
  usePublishSubtitleMutation,
  useUnpublishSubtitleMutation,
} from "@/app/api/subtitleApi";
import { apiErrorMessage } from "@/lib/apiError";
import { confirmDialog } from "@/lib/dialogs";
import { toastEmitter } from "@/lib/toastEmitter";
import { subtitleLabel } from "@/modules/watch/components/video/settings-menu";
import type { ManagedSubtitle } from "@/types/subtitle";
import { SubtitleEditor } from "./subtitle-editor";

const LANGUAGES = ["vi", "en", "ko", "ja", "zh"];

interface SubtitlePanelProps {
  /** Chưa có khi tệp đang tải lên. */
  videoId?: string;
  videoSrc: string;
  /** Video chưa đăng: phụ đề đã tạo xong được công khai cùng lúc đăng video. */
  videoIsDraft: boolean;
}

/** Phụ đề AI (E2): tạo bằng AI → bản nháp để xem, sửa → công khai. */
export function SubtitlePanel({ videoId, videoSrc, videoIsDraft }: SubtitlePanelProps) {
  const [language, setLanguage] = useState("vi");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [polling, setPolling] = useState(false);
  const { data: subtitles = [], isError } = useGetManagedSubtitlesQuery(videoId ?? "", {
    skip: !videoId,
    pollingInterval: polling ? 5000 : 0,
  });
  const [generate, { isLoading: generating }] = useGenerateSubtitleMutation();
  const [publish] = usePublishSubtitleMutation();
  const [unpublish] = useUnpublishSubtitleMutation();
  const [remove] = useDeleteSubtitleMutation();
  const [busyId, setBusyId] = useState<string | null>(null);

  // AI chạy nền: hỏi lại server mỗi 5 giây cho tới khi không còn phụ đề nào đang tạo
  useEffect(() => {
    setPolling(subtitles.some((s) => s.status === "PROCESSING"));
  }, [subtitles]);

  const start = async (lang: string) => {
    if (!videoId) return;
    const existing = subtitles.find((s) => s.language === lang && s.status !== "FAILED");
    if (existing) {
      const replace = await confirmDialog({
        title: `Tạo lại phụ đề ${subtitleLabel(lang)}?`,
        message: `Phụ đề ${subtitleLabel(lang)} hiện có (kể cả chỗ bạn đã sửa) sẽ bị thay bằng bản AI mới.`,
        confirmLabel: "Tạo lại",
        destructive: true,
      });
      if (!replace) return;
    }
    try {
      await generate({ videoId, language: lang }).unwrap();
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không tạo được phụ đề, vui lòng thử lại."));
    }
  };

  const run = async (s: ManagedSubtitle, action: "publish" | "unpublish" | "delete") => {
    if (!videoId) return;
    if (action === "delete") {
      const ok = await confirmDialog({
        title: `Xoá phụ đề ${subtitleLabel(s.language)}?`,
        message: s.status === "PUBLISHED" ? "Người xem sẽ không còn thấy phụ đề này." : undefined,
        confirmLabel: "Xoá",
        destructive: true,
      });
      if (!ok) return;
    }
    setBusyId(s.id);
    const ref = { videoId, subtitleId: s.id };
    try {
      if (action === "publish") await publish(ref).unwrap();
      else if (action === "unpublish") await unpublish(ref).unwrap();
      else await remove(ref).unwrap();
      toastEmitter.success(
        action === "publish" ? "Đã công khai phụ đề" : action === "unpublish" ? "Đã ẩn phụ đề" : "Đã xoá phụ đề"
      );
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không thực hiện được, vui lòng thử lại."));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <p className="mb-1 font-medium">Phụ đề AI</p>
      <p className="mb-3 text-sm text-gray-500">
        AI nghe video và tạo phụ đề dịch sang ngôn ngữ bạn chọn, mất khoảng 1–3 phút. Phụ đề tạo xong là bản nháp: bạn
        xem, sửa rồi {videoIsDraft ? "đăng video để công khai cùng lúc" : "bấm Công khai để người xem thấy"}.
      </p>

      <div className="flex flex-wrap items-center gap-2">
        <Select
          value={language}
          // Radix có thể gọi onValueChange("") khi đổi giá trị từ ngoài: bỏ qua
          onValueChange={(value) => value && setLanguage(value)}
        >
          <SelectTrigger className="w-full sm:w-56">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {LANGUAGES.map((l) => (
              <SelectItem key={l} value={l}>
                {subtitleLabel(l)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button variant="outline" className="rounded-full" onClick={() => start(language)} disabled={!videoId || generating}>
          {generating && <Loader2 className="mr-2 size-4 animate-spin" />}
          Tạo phụ đề AI
        </Button>
      </div>
      {!videoId && <p className="mt-2 text-sm text-gray-500">Tệp tải lên xong mới tạo được phụ đề.</p>}
      {isError && <p className="mt-2 text-sm text-red-600">Không tải được danh sách phụ đề.</p>}

      {subtitles.length > 0 && (
        <ul className="mt-4 divide-y rounded-lg border">
          {subtitles.map((s) => (
            <li key={s.id} className="flex flex-wrap items-center gap-x-3 gap-y-2 p-3">
              <div className="min-w-0 flex-1">
                <p className="font-medium">
                  {subtitleLabel(s.language)}
                  {s.aiGenerated && <span className="ml-2 text-xs font-normal text-gray-500">AI</span>}
                </p>
                <StatusLine subtitle={s} videoIsDraft={videoIsDraft} />
              </div>
              <div className="flex items-center gap-1">
                {(s.status === "DRAFT" || s.status === "PUBLISHED") && (
                  <Button variant="outline" size="sm" className="rounded-full" onClick={() => setEditingId(s.id)}>
                    Xem và sửa
                  </Button>
                )}
                {s.status === "DRAFT" && !videoIsDraft && (
                  <Button
                    size="sm"
                    className="rounded-full bg-blue-600 text-white hover:bg-blue-700"
                    onClick={() => run(s, "publish")}
                    disabled={busyId === s.id}
                  >
                    Công khai
                  </Button>
                )}
                {s.status === "PUBLISHED" && !videoIsDraft && (
                  <Button variant="ghost" size="sm" className="rounded-full" onClick={() => run(s, "unpublish")} disabled={busyId === s.id}>
                    Ẩn
                  </Button>
                )}
                {s.status === "FAILED" && (
                  <Button variant="outline" size="sm" className="rounded-full" onClick={() => start(s.language)} disabled={generating}>
                    Thử lại
                  </Button>
                )}
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-8"
                  aria-label="Xoá phụ đề"
                  title="Xoá phụ đề"
                  onClick={() => run(s, "delete")}
                  disabled={busyId === s.id}
                >
                  <Trash2 className="size-4" />
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {videoId && (
        <SubtitleEditor videoId={videoId} subtitleId={editingId} videoSrc={videoSrc} onClose={() => setEditingId(null)} />
      )}
    </div>
  );
}

function StatusLine({ subtitle: s, videoIsDraft }: { subtitle: ManagedSubtitle; videoIsDraft: boolean }) {
  if (s.status === "PROCESSING") {
    return (
      <p className="flex items-center gap-1 text-sm text-gray-500">
        <Loader2 className="size-3.5 animate-spin" />
        Đang tạo phụ đề…
      </p>
    );
  }
  if (s.status === "FAILED") {
    return <p className="text-sm text-red-600">{s.errorMessage || "Không tạo được phụ đề."}</p>;
  }
  const count = s.cueCount > 0 ? ` · ${s.cueCount} câu` : "";
  if (s.status === "PUBLISHED") {
    return <p className="text-sm text-green-700">Đã công khai{count}</p>;
  }
  return (
    <p className="text-sm text-amber-700">
      Bản nháp{count} · {videoIsDraft ? "công khai khi đăng video" : "người xem chưa thấy"}
    </p>
  );
}
