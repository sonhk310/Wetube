"use client";
import { useEffect, useState } from "react";

export interface Cue {
  start: number;
  end: number;
  text: string;
}

function toSeconds(t: string) {
  const [h, m, s] = t.replace(",", ".").split(":").map(parseFloat);
  return h * 3600 + m * 60 + s;
}

function parseVtt(vtt: string): Cue[] {
  const cues: Cue[] = [];
  const blocks = vtt.split("\n\n");
  for (const block of blocks) {
    const lines = block.split("\n").map(l => l.trim()).filter(Boolean);
    if (lines.length >= 2 && lines[0].includes("-->")) {
      const [start, end] = lines[0].split("-->").map(s => s.trim());
      cues.push({
        start: toSeconds(start),
        end: toSeconds(end),
        text: lines.slice(1).join("\n"),
      });
    }
  }
  return cues;
}

export function useSubtitles(url?: string) {
  const [cues, setCues] = useState<Cue[]>([]);
  useEffect(() => {
    if (!url) return;
    fetch(url)
      .then(r => r.text())
      .then(txt => setCues(parseVtt(txt)))
      .catch(err => console.error("Subtitle error:", err));
  }, [url]);
  return cues;
}
