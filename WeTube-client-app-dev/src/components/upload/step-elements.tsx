"use client";

import { SubtitlePanel } from "./subtitle-panel";

interface StepElementsProps {
  /** Chưa có khi tệp đang tải lên. */
  videoId?: string;
  videoSrc: string;
  videoIsDraft: boolean;
}

export function StepElements({ videoId, videoSrc, videoIsDraft }: StepElementsProps) {
  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold">Các thành phần của video</h2>
      <SubtitlePanel videoId={videoId} videoSrc={videoSrc} videoIsDraft={videoIsDraft} />
    </div>
  );
}
