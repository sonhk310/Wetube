"use client";

import { useEffect, useMemo, useRef, useState, type MutableRefObject } from "react";
import { Loader2, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { useGetSubtitleQuery, useSaveSubtitleMutation } from "@/app/api/subtitleApi";
import { apiErrorMessage } from "@/lib/apiError";
import { confirmDialog } from "@/lib/dialogs";
import { toastEmitter } from "@/lib/toastEmitter";
import { cn } from "@/lib/utils";
import { subtitleLabel } from "@/modules/watch/components/video/settings-menu";
import { parseVtt } from "@/modules/watch/lib/subtitle-utils";
import type { ManagedSubtitle, SubtitleCue } from "@/types/subtitle";

/** Giây → "m:ss.mmm" (hoặc "h:mm:ss.mmm"). */
export function formatCueTime(seconds: number): string {
  const total = Math.max(0, Math.round(seconds * 1000));
  const h = Math.floor(total / 3_600_000);
  const m = Math.floor(total / 60_000) % 60;
  const s = Math.floor(total / 1000) % 60;
  const ms = total % 1000;
  const tail = `${String(s).padStart(2, "0")}.${String(ms).padStart(3, "0")}`;
  return h > 0 ? `${h}:${String(m).padStart(2, "0")}:${tail}` : `${m}:${tail}`;
}

/** "1:02.5", "62,5", "0:01:02.500" → giây; null nếu gõ sai. */
export function parseCueTime(value: string): number | null {
  const parts = value.trim().replace(",", ".").split(":");
  if (parts.length > 3) return null;
  let seconds = 0;
  for (let i = 0; i < parts.length; i++) {
    const last = i === parts.length - 1;
    if (!(last ? /^\d+(\.\d+)?$/ : /^\d+$/).test(parts[i])) return null;
    seconds = seconds * 60 + Number(parts[i]);
  }
  return Math.round(seconds * 1000) / 1000;
}

type Row = { key: number; start: string; end: string; text: string; original?: string | null };

let nextKey = 0;
const toRow = (c: SubtitleCue): Row => ({
  key: nextKey++,
  start: formatCueTime(c.start),
  end: formatCueTime(c.end),
  text: c.text,
  original: c.original,
});
const serialize = (rows: Row[]) => JSON.stringify(rows.map((r) => [r.start, r.end, r.text]));

interface SubtitleEditorProps {
  videoId: string;
  /** null: đóng. */
  subtitleId: string | null;
  videoSrc: string;
  onClose: () => void;
}

/** Hộp thoại xem và sửa phụ đề: video bên trái (phụ đề hiện theo chữ đang sửa), danh sách câu bên phải. */
export function SubtitleEditor({ videoId, subtitleId, videoSrc, onClose }: SubtitleEditorProps) {
  const open = !!subtitleId;
  const { data, isFetching, isError } = useGetSubtitleQuery(
    { videoId, subtitleId: subtitleId ?? "" },
    { skip: !open, refetchOnMountOrArgChange: true }
  );
  const dirtyRef = useRef(false);

  const requestClose = async () => {
    if (dirtyRef.current) {
      const discard = await confirmDialog({
        title: "Bỏ các thay đổi?",
        message: "Những chỗ bạn vừa sửa chưa được lưu.",
        confirmLabel: "Bỏ thay đổi",
        cancelLabel: "Tiếp tục sửa",
        destructive: true,
      });
      if (!discard) return;
    }
    dirtyRef.current = false;
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !o && requestClose()}>
      <DialogContent className="max-h-[92vh] grid-cols-[minmax(0,1fr)] overflow-y-auto sm:max-w-5xl">
        {data && !isFetching ? (
          <EditorBody
            key={data.id}
            subtitle={data}
            videoId={videoId}
            videoSrc={videoSrc}
            dirtyRef={dirtyRef}
            onCancel={requestClose}
            onSaved={() => {
              dirtyRef.current = false;
              onClose();
            }}
          />
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Sửa phụ đề</DialogTitle>
              <DialogDescription className="sr-only">Đang tải phụ đề</DialogDescription>
            </DialogHeader>
            <div className="flex h-60 items-center justify-center text-gray-500">
              {isError ? "Không tải được phụ đề." : <Loader2 className="size-6 animate-spin" />}
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

interface EditorBodyProps {
  subtitle: ManagedSubtitle;
  videoId: string;
  videoSrc: string;
  dirtyRef: MutableRefObject<boolean>;
  onCancel: () => void;
  onSaved: () => void;
}

function EditorBody({ subtitle, videoId, videoSrc, dirtyRef, onCancel, onSaved }: EditorBodyProps) {
  const [saveSubtitle, { isLoading: saving }] = useSaveSubtitleMutation();
  // Phụ đề cũ (trước E2) chỉ có file .vtt: đọc file đó làm bản để sửa
  const needsVtt = !subtitle.cues && !!subtitle.subtitleUrl;
  const [rows, setRows] = useState<Row[]>(() => (subtitle.cues ?? []).map(toRow));
  const [initial, setInitial] = useState(() => serialize(rows));
  const [loadingVtt, setLoadingVtt] = useState(needsVtt);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [time, setTime] = useState(0);

  useEffect(() => {
    if (!needsVtt || !subtitle.subtitleUrl) return;
    let cancelled = false;
    fetch(subtitle.subtitleUrl)
      .then((res) => (res.ok ? res.text() : Promise.reject(new Error())))
      .then((vtt) => {
        if (cancelled) return;
        const loaded = parseVtt(vtt).map((c) => toRow({ ...c, original: null }));
        setRows(loaded);
        setInitial(serialize(loaded));
      })
      .catch(() => !cancelled && toastEmitter.error("Không đọc được file phụ đề."))
      .finally(() => !cancelled && setLoadingVtt(false));
    return () => {
      cancelled = true;
    };
  }, [needsVtt, subtitle.subtitleUrl]);

  const parsed = useMemo(
    () =>
      rows.map((r) => {
        const start = parseCueTime(r.start);
        const end = parseCueTime(r.end);
        const error =
          start === null || end === null
            ? "Giờ không hợp lệ, gõ dạng 1:02.500"
            : end <= start
              ? "Giờ kết thúc phải sau giờ bắt đầu"
              : null;
        return { ...r, startSec: start, endSec: end, error };
      }),
    [rows]
  );
  const hasErrors = parsed.some((r) => r.error);
  const dirty = serialize(rows) !== initial;
  useEffect(() => {
    dirtyRef.current = dirty;
  }, [dirty, dirtyRef]);

  const activeIndex = parsed.findIndex(
    (r) => r.startSec !== null && r.endSec !== null && time >= r.startSec && time < r.endSec
  );
  const activeText = activeIndex >= 0 ? parsed[activeIndex].text.trim() : "";

  const update = (key: number, patch: Partial<Row>) =>
    setRows((rs) => rs.map((r) => (r.key === key ? { ...r, ...patch } : r)));

  const seek = (seconds: number | null) => {
    if (seconds !== null && videoRef.current) videoRef.current.currentTime = seconds;
  };

  // Thêm câu ở vị trí đang phát, chèn đúng thứ tự giờ
  const addAtCurrentTime = () => {
    const row = toRow({ start: time, end: time + 2, text: "" });
    setRows((rs) => {
      const index = rs.findIndex((r) => (parseCueTime(r.start) ?? 0) > time);
      return index < 0 ? [...rs, row] : [...rs.slice(0, index), row, ...rs.slice(index)];
    });
  };

  const save = async () => {
    const cues: SubtitleCue[] = parsed.map((r) => ({
      start: r.startSec ?? 0,
      end: r.endSec ?? 0,
      text: r.text,
      original: r.original ?? null,
    }));
    try {
      await saveSubtitle({ videoId, subtitleId: subtitle.id, cues }).unwrap();
      toastEmitter.success(
        subtitle.status === "PUBLISHED" ? "Đã lưu, người xem thấy bản sửa ngay" : "Đã lưu phụ đề"
      );
      onSaved();
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không lưu được phụ đề, vui lòng thử lại."));
    }
  };

  return (
    <>
      <DialogHeader>
        <DialogTitle>Sửa phụ đề: {subtitleLabel(subtitle.language)}</DialogTitle>
        <DialogDescription>
          Bấm vào một câu để tua video tới đó. Câu để trống sẽ bị bỏ khi lưu.
          {subtitle.status === "PUBLISHED" && " Phụ đề đang công khai: lưu xong người xem thấy bản sửa ngay."}
        </DialogDescription>
      </DialogHeader>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <div className="space-y-3 lg:sticky lg:top-0 lg:self-start">
          <div className="relative overflow-hidden rounded-lg bg-black">
            <video
              ref={videoRef}
              src={videoSrc}
              controls
              className="aspect-video w-full"
              onTimeUpdate={(e) => setTime(e.currentTarget.currentTime)}
              onSeeked={(e) => setTime(e.currentTarget.currentTime)}
            />
            {activeText && (
              <div className="pointer-events-none absolute inset-x-0 bottom-14 flex justify-center px-4">
                <span className="whitespace-pre-line rounded bg-black/75 px-2 py-1 text-center text-sm text-white">
                  {activeText}
                </span>
              </div>
            )}
          </div>
          <Button variant="outline" className="rounded-full" onClick={addAtCurrentTime} disabled={loadingVtt}>
            <Plus className="mr-1 size-4" />
            Thêm câu ở {formatCueTime(time)}
          </Button>
        </div>

        <div className="max-h-[60vh] space-y-2 overflow-y-auto pr-1">
          {loadingVtt ? (
            <div className="flex h-40 items-center justify-center">
              <Loader2 className="size-6 animate-spin text-gray-500" />
            </div>
          ) : parsed.length === 0 ? (
            <p className="py-10 text-center text-sm text-gray-500">Chưa có câu nào. Tua video rồi bấm “Thêm câu”.</p>
          ) : (
            parsed.map((r, i) => (
              <div
                key={r.key}
                onClick={() => seek(r.startSec)}
                className={cn(
                  "space-y-2 rounded-lg border p-3",
                  i === activeIndex ? "border-blue-500 bg-blue-50" : "border-gray-200"
                )}
              >
                <div className="flex items-center gap-2">
                  <input
                    value={r.start}
                    onChange={(e) => update(r.key, { start: e.target.value })}
                    aria-label="Giờ bắt đầu"
                    className={cn(
                      "w-24 rounded border px-2 py-1 font-mono text-sm",
                      r.error && "border-red-500"
                    )}
                  />
                  <span className="text-gray-400">–</span>
                  <input
                    value={r.end}
                    onChange={(e) => update(r.key, { end: e.target.value })}
                    aria-label="Giờ kết thúc"
                    className={cn(
                      "w-24 rounded border px-2 py-1 font-mono text-sm",
                      r.error && "border-red-500"
                    )}
                  />
                  <Button
                    variant="ghost"
                    size="icon"
                    className="ml-auto size-8"
                    aria-label="Xoá câu"
                    title="Xoá câu"
                    onClick={(e) => {
                      e.stopPropagation();
                      setRows((rs) => rs.filter((x) => x.key !== r.key));
                    }}
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </div>
                <textarea
                  value={r.text}
                  onChange={(e) => update(r.key, { text: e.target.value })}
                  rows={2}
                  maxLength={500}
                  aria-label="Câu phụ đề"
                  className="w-full resize-none rounded border px-2 py-1 text-sm"
                />
                {r.original && <p className="text-xs text-gray-500">Câu gốc: {r.original}</p>}
                {r.error && <p className="text-xs text-red-600">{r.error}</p>}
              </div>
            ))
          )}
        </div>
      </div>

      <DialogFooter>
        <Button variant="ghost" className="rounded-full" onClick={onCancel} disabled={saving}>
          Huỷ
        </Button>
        <Button
          className="rounded-full bg-blue-600 px-5 text-white hover:bg-blue-700"
          onClick={save}
          disabled={!dirty || hasErrors || saving || loadingVtt}
        >
          {saving && <Loader2 className="mr-2 size-4 animate-spin" />}
          Lưu
        </Button>
      </DialogFooter>
    </>
  );
}
