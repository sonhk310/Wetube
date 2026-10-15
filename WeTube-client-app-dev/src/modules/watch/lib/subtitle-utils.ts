// src/lib/subtitles/subtitle-utils.ts

export interface Cue {
  start: number;
  end: number;
  text: string;
}

export function parseVtt(vtt: string): Cue[] {
  const cues: Cue[] = [];
  const blocks = vtt
    .split(/\n\n+/)
    .map((b) => b.trim())
    .filter(Boolean);

  for (const block of blocks) {
    const lines = block.split("\n").filter(Boolean);

    if (lines[0].includes("-->")) {
      const [start, end] = lines[0].split("-->");
      cues.push({
        start: toSeconds(start.trim()),
        end: toSeconds(end.trim()),
        text: lines.slice(1).join("\n").trim(),
      });
    } else if (lines[1] && lines[1].includes("-->")) {
      const [start, end] = lines[1].split("-->");
      cues.push({
        start: toSeconds(start.trim()),
        end: toSeconds(end.trim()),
        text: lines.slice(2).join("\n").trim(),
      });
    }
  }

  return cues;
}

function toSeconds(ts: string): number {
  const [h, m, s] = ts.split(":");
  const [sec, ms] = (s || "0").split(".");
  return (
    (parseInt(h || "0") || 0) * 3600 +
    (parseInt(m || "0") || 0) * 60 +
    (parseInt(sec || "0") || 0) +
    (parseInt(ms || "0") || 0) / 1000
  );
}
