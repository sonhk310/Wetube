"use client";

import { useState } from "react";
import { Loader2, Sparkles, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { API_PREFIX } from "@/constants/appConstant";
import { apiFetch } from "@/lib/apiFetch";
import { toastEmitter } from "@/lib/toastEmitter";
import type { StepProps } from "./types";

// Cùng giới hạn với backend (utility/SearchKeywords)
const MAX_KEYWORDS = 15;
const MAX_LENGTH = 40;

const toList = (value: string) => value.split(",").map((k) => k.trim()).filter(Boolean);

/** Thêm từ khóa mới vào chuỗi (phân cách dấu phẩy), bỏ trùng, tối đa MAX_KEYWORDS. */
function addKeywords(current: string, incoming: string[]): string {
  const list = toList(current);
  for (const raw of incoming) {
    const word = raw.replace(/[#,;\n"]/g, " ").replace(/\s+/g, " ").trim().toLowerCase().slice(0, MAX_LENGTH).trim();
    if (word && !list.includes(word) && list.length < MAX_KEYWORDS) list.push(word);
  }
  return list.join(",");
}

/**
 * Từ khóa tìm kiếm của video: chỉ dùng để video được tìm thấy bằng cách gõ khác (tên dịch, không dấu...),
 * người xem không thấy. "Gợi ý từ khóa" nhờ Gemini gợi ý, chủ video xem và xoá trước khi lưu.
 */
export function SearchKeywordsField({ form, setForm }: StepProps) {
  const [draft, setDraft] = useState("");
  const [suggesting, setSuggesting] = useState(false);
  const keywords = toList(form.searchKeywords);

  const commitDraft = () => {
    if (draft.trim()) setForm({ searchKeywords: addKeywords(form.searchKeywords, draft.split(/[,;\n]/)) });
    setDraft("");
  };

  const suggest = async () => {
    if (!form.title.trim()) {
      toastEmitter.error("Hãy nhập tiêu đề trước.");
      return;
    }
    setSuggesting(true);
    try {
      const res = await apiFetch(`${API_PREFIX}/ai/keywords`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: form.title.trim(), description: form.description, tags: form.tags }),
      });
      const body = await res.json().catch(() => null);
      if (!res.ok) {
        toastEmitter.error(body?.message || "Không gợi ý được từ khóa, vui lòng thử lại.");
        return;
      }
      const suggested = typeof body?.keywords === "string" ? toList(body.keywords) : [];
      if (suggested.length === 0) {
        toastEmitter.error("Chưa gợi ý được từ khóa nào, bạn có thể tự nhập.");
        return;
      }
      setForm({ searchKeywords: addKeywords(form.searchKeywords, suggested) });
    } catch {
      toastEmitter.error("Không kết nối được máy chủ, vui lòng thử lại.");
    } finally {
      setSuggesting(false);
    }
  };

  return (
    <div>
      <div className="mb-2 flex items-center justify-between gap-2">
        <label htmlFor="upload-keywords" className="block font-medium">
          Từ khóa tìm kiếm
        </label>
        <Button type="button" variant="outline" size="sm" className="rounded-full" onClick={suggest} disabled={suggesting}>
          {suggesting ? <Loader2 className="mr-1.5 h-4 w-4 animate-spin" /> : <Sparkles className="mr-1.5 h-4 w-4" />}
          Gợi ý từ khóa
        </Button>
      </div>
      {keywords.length > 0 && (
        <div className="mb-2 flex flex-wrap gap-2">
          {keywords.map((k) => (
            <span key={k} className="flex items-center gap-1 rounded-full bg-gray-100 py-1 pl-3 pr-1.5 text-sm">
              {k}
              <button
                type="button"
                aria-label={`Xoá từ khóa ${k}`}
                onClick={() => setForm({ searchKeywords: keywords.filter((x) => x !== k).join(",") })}
                className="rounded-full p-0.5 hover:bg-gray-200"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </span>
          ))}
        </div>
      )}
      <Input
        id="upload-keywords"
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === ",") {
            e.preventDefault();
            commitDraft();
          }
        }}
        onBlur={commitDraft}
        placeholder="Gõ từ khóa rồi nhấn Enter"
        disabled={keywords.length >= MAX_KEYWORDS}
      />
      <p className="mt-1 text-xs text-gray-500">
        Giúp video được tìm thấy bằng cách gõ khác, như tên dịch hoặc không dấu (tối đa {MAX_KEYWORDS} từ khóa). Chỉ dùng
        để tìm kiếm, người xem không thấy. Bạn nên xem lại gợi ý của AI trước khi lưu.
      </p>
    </div>
  );
}
