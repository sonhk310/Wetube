// src/lib/subtitles/useSubtitles.ts
"use client";

import { useEffect, useState } from "react";
import { Cue, parseVtt } from "./subtitle-utils";

export function useSubtitles(url?: string) {
  const [cues, setCues] = useState<Cue[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!url) return;

    setLoading(true);
    fetch(url, { mode: "cors" })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.text();
      })
      .then((vtt) => {
        setCues(parseVtt(vtt));
        setLoading(false);
      })
      .catch((err) => {
        setError(err);
        setLoading(false);
      });
  }, [url]);

  return { cues, loading, error };
}
