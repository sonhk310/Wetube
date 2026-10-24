"use client";

import { Check } from "lucide-react";
import { cn } from "@/lib/utils";
import type { StepId } from "./types";

interface UploadStepperProps {
  /** disabled: chưa được tới bước này (chưa qua các bước trước, hoặc bước Chi tiết còn lỗi). */
  steps: { id: StepId; label: string; error: boolean; disabled: boolean }[];
  current: StepId;
  onSelect: (id: StepId) => void;
}

/** Thanh bước kiểu YouTube: tên bước ở trên, chấm tròn nối bằng đường kẻ ở dưới. */
export function UploadStepper({ steps, current, onSelect }: UploadStepperProps) {
  const currentIndex = steps.findIndex((s) => s.id === current);
  return (
    <div className="flex items-start px-6 pt-3 pb-4 border-b border-gray-200">
      {steps.map((step, i) => {
        const active = i === currentIndex;
        const done = i < currentIndex && !step.error;
        return (
          <button
            key={step.id}
            type="button"
            onClick={() => onSelect(step.id)}
            disabled={step.disabled}
            className="group flex min-w-0 flex-1 flex-col items-center gap-2 disabled:cursor-not-allowed"
          >
            <span
              className={cn(
                "max-w-full truncate px-1 text-sm",
                step.error
                  ? "text-red-600"
                  : active
                    ? "font-medium text-black"
                    : step.disabled
                      ? "text-gray-400"
                      : "text-gray-500 group-hover:text-black"
              )}
            >
              {step.label}
            </span>
            <span className="relative flex h-5 w-full items-center justify-center">
              {i > 0 && <span className="absolute left-0 right-1/2 h-0.5 bg-gray-300" />}
              {i < steps.length - 1 && <span className="absolute left-1/2 right-0 h-0.5 bg-gray-300" />}
              <span
                className={cn(
                  "relative z-10 flex h-5 w-5 items-center justify-center rounded-full border-2 text-[11px] font-bold",
                  step.error
                    ? "border-red-600 bg-red-600 text-white"
                    : active
                      ? "border-blue-600 bg-white ring-4 ring-blue-100"
                      : done
                        ? "border-blue-600 bg-blue-600 text-white"
                        : "border-gray-400 bg-white"
                )}
              >
                {step.error ? "!" : done ? <Check className="h-3 w-3" /> : null}
              </span>
            </span>
          </button>
        );
      })}
    </div>
  );
}
