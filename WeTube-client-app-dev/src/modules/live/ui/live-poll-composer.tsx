"use client";

import { useState } from "react";
import { Plus, X } from "lucide-react";
import { useClosePollMutation, useOpenPollMutation } from "@/app/api/liveApi";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { apiErrorMessage } from "@/lib/apiError";
import { toastEmitter } from "@/lib/toastEmitter";
import type { PollView } from "../types";
import LivePollCard from "./live-poll-card";

const QUESTION_MAX = 150;
const OPTION_MAX = 60;
const OPTIONS_MIN = 2;
const OPTIONS_MAX = 4;

/** Chủ kênh mở / đóng Poll. `poll` là Poll hiện tại (đang mở hoặc vừa đóng) nếu có; `onChanged` nhận Poll mới nhất. */
export default function LivePollComposer({
  liveId,
  poll,
  onChanged,
}: {
  liveId: string;
  poll: PollView | null | undefined;
  onChanged: (poll: PollView) => void;
}) {
  const [openPoll, { isLoading: opening }] = useOpenPollMutation();
  const [closePoll, { isLoading: closing }] = useClosePollMutation();
  const [creating, setCreating] = useState(false);
  const [question, setQuestion] = useState("");
  const [options, setOptions] = useState(["", ""]);

  const submit = async () => {
    try {
      const created = await openPoll({
        id: liveId,
        question: question.trim(),
        options: options.map((o) => o.trim()),
      }).unwrap();
      onChanged(created);
      setCreating(false);
      setQuestion("");
      setOptions(["", ""]);
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không mở được Poll, vui lòng thử lại."));
    }
  };

  const close = async () => {
    if (!poll) return;
    try {
      onChanged(await closePoll({ id: liveId, pollId: poll.id }).unwrap());
    } catch (err) {
      toastEmitter.error(apiErrorMessage(err, "Không đóng được Poll, vui lòng thử lại."));
    }
  };

  if (poll?.open) {
    return (
      <div className="space-y-2">
        <LivePollCard poll={poll} />
        <Button variant="outline" className="w-full" disabled={closing} onClick={() => void close()}>
          Đóng Poll
        </Button>
      </div>
    );
  }

  if (!creating) {
    return (
      <div className="space-y-2">
        {poll ? <LivePollCard poll={poll} /> : null}
        <Button variant="secondary" className="w-full" onClick={() => setCreating(true)}>
          Tạo Poll
        </Button>
      </div>
    );
  }

  const valid = question.trim() !== "" && options.every((o) => o.trim() !== "");
  return (
    <div className="space-y-2 rounded-xl border border-gray-200 p-3">
      <Input
        value={question}
        maxLength={QUESTION_MAX}
        onChange={(e) => setQuestion(e.target.value)}
        placeholder="Câu hỏi cho người xem"
      />
      {options.map((option, index) => (
        <div key={index} className="flex gap-2">
          <Input
            value={option}
            maxLength={OPTION_MAX}
            onChange={(e) => setOptions((prev) => prev.map((o, i) => (i === index ? e.target.value : o)))}
            placeholder={`Lựa chọn ${index + 1}`}
          />
          {options.length > OPTIONS_MIN ? (
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label="Bỏ lựa chọn"
              onClick={() => setOptions((prev) => prev.filter((_, i) => i !== index))}
            >
              <X className="size-4" />
            </Button>
          ) : null}
        </div>
      ))}
      {options.length < OPTIONS_MAX ? (
        <Button type="button" variant="ghost" size="sm" onClick={() => setOptions((prev) => [...prev, ""])}>
          <Plus className="size-4" />
          Thêm lựa chọn
        </Button>
      ) : null}
      <div className="flex justify-end gap-2">
        <Button variant="outline" onClick={() => setCreating(false)} disabled={opening}>
          Huỷ
        </Button>
        <Button disabled={!valid || opening} onClick={() => void submit()}>
          Mở Poll
        </Button>
      </div>
    </div>
  );
}
