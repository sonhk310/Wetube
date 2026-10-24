"use client";

import { cn } from "@/lib/utils";
import type { StepProps, Visibility } from "./types";

const OPTIONS: { value: Visibility; label: string; description: string }[] = [
  { value: "ACTIVE", label: "Công khai", description: "Ai cũng có thể tìm và xem video của bạn." },
  { value: "UNLISTED", label: "Không công khai", description: "Chỉ người có đường liên kết mới xem được." },
  { value: "PRIVATE", label: "Riêng tư", description: "Chỉ mình bạn xem được." },
];

export function StepVisibility({ form, setForm }: StepProps) {
  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">Chế độ hiển thị</h2>
      <p className="text-sm text-gray-500">Chọn thời điểm và người có thể xem video của bạn.</p>
      <div role="radiogroup" className="space-y-2 rounded-lg border border-gray-200 p-2">
        {OPTIONS.map((o) => {
          const checked = form.visibility === o.value;
          return (
            <label
              key={o.value}
              className={cn(
                "flex cursor-pointer items-start gap-3 rounded-md p-3",
                checked ? "bg-blue-50" : "hover:bg-gray-50"
              )}
            >
              <input
                type="radio"
                name="visibility"
                value={o.value}
                checked={checked}
                onChange={() => setForm({ visibility: o.value })}
                className="mt-1 h-4 w-4 accent-blue-600"
              />
              <span>
                <span className="block font-medium">{o.label}</span>
                <span className="block text-sm text-gray-600">{o.description}</span>
              </span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
