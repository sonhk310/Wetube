export interface Chapter {
  start: number;
  /** Mốc của chương sau, hoặc hết video. */
  end: number;
  title: string;
}

// Cùng dạng mốc thời gian mà mô tả bấm được (linkified-text.tsx): m:ss / h:mm:ss
const STAMP = /(?<![\w:])((?:\d{1,2}:)?[0-5]?\d:[0-5]\d)(?![\w:])/;
// Dấu nối giữa mốc và tên: "0:00 - Mở đầu", "Mở đầu | 0:00", "(0:00) Mở đầu"
const EDGE = /^[\s\-–—|:•·.,()[\]]+|[\s\-–—|:•·.,()[\]]+$/g;

const LINK = /https?:\/\/[^\s<>"]+/g;

const MIN_LENGTH = 10;

/** id của hàng "Chương" dưới mô tả (nút tên chương trên trình phát cuộn tới đây). */
export const CHAPTERS_ID = "video-chapters";

function toSeconds(stamp: string) {
  return stamp.split(":").reduce((total, part) => total * 60 + Number(part), 0);
}

/**
 * Đọc chương từ mô tả theo quy tắc của YouTube: mỗi dòng có một mốc thời gian, mốc đầu là 0:00,
 * ít nhất 3 mốc, tăng dần, mỗi chương dài ít nhất 10 giây. Sai một điều thì không có chương ([]).
 * `duration` <= 0 (chưa biết thời lượng) thì bỏ qua kiểm tra chương cuối.
 */
export function parseChapters(description: string | null | undefined, duration: number): Chapter[] {
  if (!description) return [];
  const marks: { start: number; title: string }[] = [];
  for (const raw of description.split(/\r?\n/)) {
    // Số trong link không phải mốc (mô tả cũng không biến chúng thành mốc bấm được)
    const line = raw.replace(LINK, "");
    const match = line.match(STAMP);
    if (!match || match.index === undefined) continue;
    const rest = `${line.slice(0, match.index)} ${line.slice(match.index + match[0].length)}`;
    marks.push({ start: toSeconds(match[1]), title: rest.replace(EDGE, "").replace(/\s+/g, " ") });
  }

  if (marks.length < 3 || marks[0].start !== 0) return [];
  const chapters: Chapter[] = [];
  for (let i = 0; i < marks.length; i++) {
    const end = i + 1 < marks.length ? marks[i + 1].start : duration;
    if ((i + 1 < marks.length || duration > 0) && end - marks[i].start < MIN_LENGTH) return [];
    chapters.push({ start: marks[i].start, end, title: marks[i].title || `Chương ${i + 1}` });
  }
  return chapters;
}

/** Chỉ số chương chứa giây `time` (-1 nếu không có chương). */
export function chapterIndexAt(chapters: Chapter[], time: number) {
  let index = -1;
  for (let i = 0; i < chapters.length && chapters[i].start <= time; i++) index = i;
  return index;
}
