"use client";

import { useEffect, useState } from "react";
import { Loader2, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { toastEmitter } from "@/lib/toastEmitter";
import { groupIdOf, loadTopics, type TopicGroup } from "@/lib/topics";
import { cn } from "@/lib/utils";
import type { StepProps } from "./types";

/**
 * Chủ đề của video (C8): đúng một chủ đề, chọn nhóm rồi chọn chủ đề trong nhóm ("Chung" = cả nhóm).
 * "Gợi ý chủ đề" nhờ Gemini điền sẵn hai ô, chủ video đổi được trước khi lưu.
 */
export function TopicField({ form, setForm, error }: StepProps & { error?: string }) {
  const [groups, setGroups] = useState<TopicGroup[] | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [suggesting, setSuggesting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    loadTopics()
      .then((list) => !cancelled && setGroups(list))
      .catch(() => !cancelled && setLoadFailed(true));
    return () => {
      cancelled = true;
    };
  }, []);

  const groupId = form.topicId ? groupIdOf(form.topicId) : "";
  const group = groups?.find((g) => g.id === groupId);

  const suggest = async () => {
    if (!form.title.trim()) {
      toastEmitter.error("Hãy nhập tiêu đề trước.");
      return;
    }
    setSuggesting(true);
    try {
      const res = await apiFetch(`${API_PREFIX}/ai/topic`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          description: form.description,
          tags: form.tags,
          searchKeywords: form.searchKeywords,
        }),
      });
      const body = await res.json().catch(() => null);
      if (!res.ok) {
        toastEmitter.error(body?.message || "Không gợi ý được chủ đề, vui lòng thử lại.");
        return;
      }
      if (typeof body?.topicId !== "string" || !body.topicId) {
        toastEmitter.error("Chưa gợi ý được chủ đề, bạn hãy tự chọn.");
        return;
      }
      setForm({ topicId: body.topicId });
    } catch {
      toastEmitter.error("Không kết nối được máy chủ, vui lòng thử lại.");
    } finally {
      setSuggesting(false);
    }
  };

  return (
    <div>
      <div className="mb-2 flex items-center justify-between gap-2">
        <p className="font-medium">Chủ đề (bắt buộc)</p>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="rounded-full"
          onClick={suggest}
          disabled={suggesting || !groups}
        >
          {suggesting ? <Loader2 className="mr-1.5 h-4 w-4 animate-spin" /> : <Sparkles className="mr-1.5 h-4 w-4" />}
          Gợi ý chủ đề
        </Button>
      </div>
      <div className="flex flex-col gap-2 sm:flex-row">
        <Select
          value={groupId}
          // Radix có thể gọi onValueChange("") khi đổi giá trị từ ngoài: bỏ qua
          onValueChange={(value) => value && value !== groupId && setForm({ topicId: value })}
          disabled={!groups}
        >
          <SelectTrigger aria-label="Nhóm chủ đề" className={cn("w-full sm:w-1/2", error && "border-red-500")}>
            <SelectValue placeholder={groups ? "Chọn nhóm" : "Đang tải…"} />
          </SelectTrigger>
          <SelectContent className="max-h-80">
            {groups?.map((g) => (
              <SelectItem key={g.id} value={g.id}>
                {g.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={group ? form.topicId : ""}
          onValueChange={(value) => value && setForm({ topicId: value })}
          disabled={!group}
        >
          <SelectTrigger aria-label="Chủ đề" className="w-full sm:w-1/2">
            <SelectValue placeholder="Chọn nhóm trước" />
          </SelectTrigger>
          <SelectContent className="max-h-80">
            {group && (
              <>
                <SelectItem value={group.id}>Chung (cả nhóm)</SelectItem>
                {group.topics.map((t) => (
                  <SelectItem key={t.id} value={t.id}>
                    {t.name}
                  </SelectItem>
                ))}
              </>
            )}
          </SelectContent>
        </Select>
      </div>
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
      {loadFailed && <p className="mt-1 text-sm text-red-600">Không tải được danh sách chủ đề, hãy tải lại trang.</p>}
      <p className="mt-1 text-xs text-gray-500">
        Chọn một chủ đề đúng nhất với video. Người xem tìm thấy video qua các chip chủ đề ở trang chủ.
      </p>
    </div>
  );
}
