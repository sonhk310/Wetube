"use client";

import { useState } from "react";
import { Loader2, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { ThumbnailSelector } from "@/components/ThumbnailSelector";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { toastEmitter } from "@/lib/toastEmitter";
import { PlaylistPicker } from "./playlist-picker";
import type { FormErrors, StepProps } from "./types";

interface StepDetailsProps extends StepProps {
  errors: FormErrors;
  thumbnailPreview: string;
  generatingThumbnail: boolean;
  isShort: boolean;
}

/** Gộp thẻ AI gợi ý vào thẻ đã có, bỏ trùng (không phân biệt hoa thường). */
function mergeTags(current: string, suggested: string): string {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const raw of `${current} ${suggested}`.split(/[\s,]+/)) {
    if (!raw) continue;
    const tag = raw.startsWith("#") ? raw : `#${raw}`;
    if (seen.has(tag.toLowerCase())) continue;
    seen.add(tag.toLowerCase());
    out.push(tag);
  }
  return out.join(" ");
}

function FieldError({ message }: { message?: string }) {
  return message ? <p className="mt-1 text-sm text-red-600">{message}</p> : null;
}

export function StepDetails({
  form,
  setForm,
  errors,
  thumbnailPreview,
  generatingThumbnail,
  isShort,
}: StepDetailsProps) {
  const [suggesting, setSuggesting] = useState(false);

  const suggestTags = async () => {
    const title = form.title.trim();
    if (!title) {
      toastEmitter.error("Vui lòng nhập tiêu đề trước.");
      return;
    }
    setSuggesting(true);
    try {
      const res = await apiFetch(`${API_PREFIX}/ai/description`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title }),
      });
      if (!res.ok) throw new Error();
      const data = (await res.json()) as { tags?: string };
      if (!data.tags) throw new Error();
      setForm({ tags: mergeTags(form.tags, data.tags) });
    } catch {
      toastEmitter.error("Không gợi ý được thẻ, vui lòng thử lại.");
    } finally {
      setSuggesting(false);
    }
  };

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold">Chi tiết</h2>

      <div>
        <label htmlFor="upload-title" className="mb-2 block font-medium">
          Tiêu đề (bắt buộc)
        </label>
        <Textarea
          id="upload-title"
          rows={2}
          value={form.title}
          onChange={(e) => setForm({ title: e.target.value })}
          className={errors.title ? "border-red-500" : ""}
        />
        <div className="flex justify-between">
          <FieldError message={errors.title} />
          <span className="ml-auto mt-1 text-xs text-gray-500">{form.title.length}/100</span>
        </div>
      </div>

      <div>
        <label htmlFor="upload-description" className="mb-2 block font-medium">
          Mô tả
        </label>
        <Textarea
          id="upload-description"
          rows={5}
          value={form.description}
          onChange={(e) => setForm({ description: e.target.value })}
          placeholder="Giới thiệu về video của bạn cho người xem"
        />
        <FieldError message={errors.description} />
      </div>

      <div>
        <label htmlFor="upload-tags" className="mb-2 block font-medium">
          Thẻ
        </label>
        <div className="flex gap-2">
          <Input
            id="upload-tags"
            value={form.tags}
            onChange={(e) => setForm({ tags: e.target.value })}
            placeholder="#the1 #the2"
            className={errors.tags ? "border-red-500" : ""}
          />
          <Button type="button" variant="secondary" className="rounded-full" onClick={suggestTags} disabled={suggesting}>
            {suggesting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Sparkles className="mr-2 h-4 w-4" />}
            Gợi ý thẻ bằng AI
          </Button>
        </div>
        <FieldError message={errors.tags} />
        <p className="mt-1 text-xs text-gray-500">Thẻ giúp người khác dễ tìm thấy video của bạn.</p>
      </div>

      <div>
        <ThumbnailSelector
          thumbnailPreview={thumbnailPreview}
          onThumbnailChange={(file) => setForm({ thumbnailFile: file })}
          isGenerating={generatingThumbnail}
          vertical={isShort}
        />
        <FieldError message={errors.thumbnailFile} />
      </div>

      <div>
        <p className="mb-1 font-medium">Danh sách phát</p>
        <p className="mb-2 text-sm text-gray-500">Thêm video vào một hoặc nhiều danh sách phát của bạn.</p>
        <PlaylistPicker value={form.playlistIds} onChange={(playlistIds) => setForm({ playlistIds })} />
      </div>
    </div>
  );
}
