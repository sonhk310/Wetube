/** "30" -> "1 tháng", "90" -> "3 tháng", "180" -> "6 tháng", "365" -> "1 năm"; số ngày lạ thì ghi "N ngày". */
export function durationLabel(days: number): string {
  if (days === 365) return "1 năm";
  if (days > 0 && days % 30 === 0 && days <= 360) return `${days / 30} tháng`;
  return `${days} ngày`;
}

/** Giá quy về 30 ngày, làm tròn đến nghìn đồng. */
export function pricePerMonth(price: number, days: number): number {
  return Math.round(((price / days) * 30) / 1000) * 1000;
}

/** Phần trăm rẻ hơn so với mua liên tiếp gói gốc (theo giá mỗi ngày); không rẻ hơn thì 0. */
export function savingPercent(price: number, days: number, basePrice: number, baseDays: number): number {
  const percent = Math.round((1 - price / days / (basePrice / baseDays)) * 100);
  return percent > 0 ? percent : 0;
}

export const formatVnd = (amount: number) => amount.toLocaleString("vi-VN", { style: "currency", currency: "VND" });
