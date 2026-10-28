"use client";

import { TopicField } from "@/components/upload/topic-field";
import type { WizardForm } from "@/components/upload/types";
import type { LiveRecordingInfo } from "../types";

/** "còn 12 phút" / "còn dưới 1 phút". */
function leftText(seconds: number): string {
  return seconds < 60 ? "dưới 1 phút" : `${Math.floor(seconds / 60)} phút`;
}

/**
 * Ô "Ghi hình để xem lại" của form chuẩn bị (chỉ buổi phát từ trình duyệt). Bật thì phải chọn chủ đề cho video xem lại.
 * Hết phần ghi hình của kênh hoặc của WeTube trong tháng thì ô bị khoá kèm lý do.
 */
export default function LiveRecordingOption({
  info,
  checked,
  onChecked,
  topicId,
  onTopic,
  title,
  description,
}: {
  info: LiveRecordingInfo;
  checked: boolean;
  onChecked: (value: boolean) => void;
  topicId: string;
  onTopic: (topicId: string) => void;
  title: string;
  description: string;
}) {
  const exhausted = info.secondsLeft <= 0;
  // TopicField dùng chung với luồng đăng video nên nhận cả biểu mẫu đăng video; ở đây chỉ chủ đề (và tiêu đề / mô tả
  // để Gợi ý chủ đề) có nghĩa
  const topicForm: WizardForm = {
    title,
    description,
    tags: "",
    searchKeywords: "",
    topicId,
    thumbnailFile: null,
    playlistIds: [],
    targetLang: "",
    visibility: "ACTIVE",
  };

  return (
    <div className="space-y-3 rounded-xl border p-4">
      <label className={`flex items-start gap-2 text-sm ${exhausted ? "opacity-60" : ""}`}>
        <input
          type="checkbox"
          className="mt-1"
          checked={checked && !exhausted}
          disabled={exhausted}
          onChange={(e) => onChecked(e.target.checked)}
        />
        <span>
          <span className="block font-medium">Ghi hình để xem lại</span>
          <span className="block text-gray-500">
            {exhausted
              ? info.limitedBy === "CHANNEL"
                ? "Kênh đã dùng hết phần ghi hình của tháng này. Phần này đặt lại vào đầu tháng sau."
                : "WeTube đã dùng hết hạn mức ghi hình của tháng này. Phần này đặt lại vào đầu tháng sau."
              : `Kênh còn ${leftText(info.secondsLeft)} ghi hình trong tháng này. Kết thúc buổi, bản ghi thành một video công khai của kênh. Hết phần ghi thì buổi vẫn phát nhưng không có video xem lại.`}
          </span>
        </span>
      </label>
      {checked && !exhausted ? (
        <TopicField form={topicForm} setForm={(patch) => patch.topicId !== undefined && onTopic(patch.topicId)} />
      ) : null}
    </div>
  );
}
