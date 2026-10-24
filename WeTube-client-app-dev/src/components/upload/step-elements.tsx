"use client";

import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { StepProps } from "./types";

const LANGUAGES = [
  { value: "none", label: "Không tạo phụ đề" },
  { value: "en", label: "English" },
  { value: "vi", label: "Tiếng Việt" },
  { value: "ko", label: "한국어" },
  { value: "ja", label: "日本語" },
  { value: "zh", label: "中文" },
];

export function StepElements({ form, setForm }: StepProps) {
  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold">Các thành phần của video</h2>
      <div>
        <p className="mb-1 font-medium">Phụ đề AI</p>
        <p className="mb-2 text-sm text-gray-500">
          AI nghe video và tạo phụ đề dịch sang ngôn ngữ bạn chọn. Phụ đề được tạo sau khi đăng, mất vài phút.
        </p>
        <Select
          value={form.targetLang || "none"}
          // Radix có thể gọi onValueChange("") khi đổi giá trị từ ngoài: bỏ qua
          onValueChange={(value) => value && setForm({ targetLang: value === "none" ? "" : value })}
        >
          <SelectTrigger className="w-full sm:w-72">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {LANGUAGES.map((l) => (
              <SelectItem key={l.value} value={l.value}>
                {l.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
