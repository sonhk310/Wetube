// ------------------------------------------------------------------
// THAY đổi BASE_URL theo môi trường của bạn (.env cũng được)
const BASE_URL = "https://687076887ca4d06b34b6db53.mockapi.io/api/v1/users";

export type TimeRange = "day" | "month" | "year";

export interface ApiRow {
  // 1 dòng dữ liệu trả về từ server
  /** ISO-string hoặc label tuỳ endpoint */
  label: string;
  normal: number;
  pro: number;
}

/**
 * Gọi endpoint thích hợp theo timeRange rồi convert về object “dataRaw”
 */
export async function fetchUserInsights(
  timeRange: TimeRange,
  params: { year?: number; month?: number } = {}
): Promise<{ "normal users": number[]; "pro users": number[] }> {
  // Xây URL đơn giản (bạn có thể sửa thành kiểu REST path-param tuỳ server)
  const qs = new URLSearchParams(
    Object.entries(params).reduce(
      (acc, [k, v]) => (v === undefined ? acc : { ...acc, [k]: String(v) }),
      {}
    )
  ).toString();

  // Ví dụ endpoint: /users/daily, /users/weekly, /users/monthly, /users/yearly
  const endpoint =
    timeRange === "day"
      ? "/users/daily"

      : timeRange === "month"
      ? "/users/monthly"
      : "/users/yearly";

  const url = `${BASE_URL}${endpoint}${qs ? `?${qs}` : ""}`;

  const res = await fetch(url);
  if (!res.ok) throw new Error(`Error ${res.status} when calling ${url}`);

  const rows = (await res.json()) as ApiRow[]; 

  return {
    "normal users": rows.map((r) => r.normal),
    "pro users": rows.map((r) => r.pro),
  };
}
