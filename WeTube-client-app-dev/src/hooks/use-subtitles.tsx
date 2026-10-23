"use client";
import { useEffect, useState } from "react";

export interface Cue {
  start: number;
  end: number;
  text: string;
}

/** "HH:MM:SS.mmm", "MM:SS.mmm" hoặc dấu phẩy kiểu SRT. */
function toSeconds(t: string) {
  return t
    .trim()
    .replace(",", ".")
    .split(":")
    .reduce((total, part) => total * 60 + parseFloat(part), 0);
}

function parseVtt(vtt: string): Cue[] {
  const cues: Cue[] = [];
  const blocks = vtt.replace(/\r\n?/g, "\n").split(/\n{2,}/);
  for (const block of blocks) {
    const lines = block.split("\n").map((l) => l.trim()).filter(Boolean);
    // Dòng thời gian có thể đứng sau một dòng số thứ tự / id
    const timingIndex = lines.findIndex((l) => l.includes("-->"));
    if (timingIndex < 0 || timingIndex === lines.length - 1) continue;
    const [start, rest] = lines[timingIndex].split("-->");
    const end = rest.trim().split(/\s+/)[0]; // bỏ các thiết lập vị trí phía sau
    const startSec = toSeconds(start);
    const endSec = toSeconds(end);
    if (Number.isNaN(startSec) || Number.isNaN(endSec)) continue;
    cues.push({
      start: startSec,
      end: endSec,
      text: lines.slice(timingIndex + 1).join("\n").replace(/<[^>]+>/g, ""),
    });
  }
  return cues;
}

export function useSubtitles(url?: string) {
  const [cues, setCues] = useState<Cue[]>([]);
  useEffect(() => {
    setCues([]);
    if (!url) return;
    let cancelled = false;
    fetch(url)
      .then((r) => r.text())
      .then((txt) => {
        if (!cancelled) setCues(parseVtt(txt));
      })
      .catch((err) => console.error("Subtitle error:", err));
    return () => {
      cancelled = true;
    };
  }, [url]);
  return cues;
}
