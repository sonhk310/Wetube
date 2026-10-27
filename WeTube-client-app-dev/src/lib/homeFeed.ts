/** Số video thường hiện trước hàng Shorts ở trang chủ (đủ hai hàng lưới 3 cột). */
export const SHORTS_SHELF_AFTER = 6;
/** Màn hình nhỏ chỉ có một cột nên hàng Shorts chen sớm hơn, nếu không phải cuộn rất xa mới thấy. */
export const SHORTS_SHELF_AFTER_MOBILE = 3;
/** Tối đa bao nhiêu Shorts trong hàng Shorts của trang chủ. */
export const SHORTS_SHELF_MAX = 20;

/**
 * Chia danh sách gợi ý của trang chủ: video thường nằm ở lưới, Shorts gom vào một hàng riêng chen sau
 * `shelfAfter` video thường đầu tiên (ít video thì hàng Shorts nằm ngay sau chúng). Giữ nguyên thứ tự gợi ý.
 */
export function splitHomeFeed<T extends { isShort?: boolean }>(
  videos: T[],
  shelfAfter = SHORTS_SHELF_AFTER,
  shelfMax = SHORTS_SHELF_MAX
): { before: T[]; shorts: T[]; after: T[] } {
  const regular = videos.filter((v) => !v.isShort);
  return {
    before: regular.slice(0, shelfAfter),
    shorts: videos.filter((v) => v.isShort).slice(0, shelfMax),
    after: regular.slice(shelfAfter),
  };
}
