// Chạy: node scripts/check-homeFeed.ts   (Node 22.6+; không cần cài gì)
import assert from "node:assert/strict";
import { splitHomeFeed } from "../src/lib/homeFeed.ts";

let count = 0;
function check(name: string, fn: () => void) {
  fn();
  count++;
  console.log(`ok  ${name}`);
}

const v = (id: string, isShort?: boolean) => ({ id, isShort });
const ids = (list: { id: string }[]) => list.map((x) => x.id);

check("Shorts tách khỏi lưới, giữ thứ tự gợi ý", () => {
  const feed = [v("a"), v("s1", true), v("b"), v("c"), v("s2", true), v("d")];
  const r = splitHomeFeed(feed, 6);
  assert.deepEqual(ids(r.before), ["a", "b", "c", "d"]);
  assert.deepEqual(ids(r.shorts), ["s1", "s2"]);
  assert.deepEqual(ids(r.after), []);
});

check("hàng Shorts chen sau đúng N video thường đầu", () => {
  const feed = Array.from({ length: 10 }, (_, i) => v(`v${i}`));
  feed.splice(2, 0, v("s1", true)); // Short nằm lẫn giữa không làm lệch số video thường
  const r = splitHomeFeed(feed, 6);
  assert.deepEqual(ids(r.before), ["v0", "v1", "v2", "v3", "v4", "v5"]);
  assert.deepEqual(ids(r.after), ["v6", "v7", "v8", "v9"]);
  assert.deepEqual(ids(r.shorts), ["s1"]);
});

check("không có Shorts: lưới như cũ, hàng Shorts trống", () => {
  const r = splitHomeFeed([v("a"), v("b")]);
  assert.deepEqual(ids(r.before), ["a", "b"]);
  assert.equal(r.shorts.length, 0);
  assert.equal(r.after.length, 0);
});

check("toàn Shorts: không có video thường", () => {
  const r = splitHomeFeed([v("s1", true), v("s2", true)]);
  assert.equal(r.before.length, 0);
  assert.deepEqual(ids(r.shorts), ["s1", "s2"]);
});

check("danh sách rỗng", () => {
  const r = splitHomeFeed([]);
  assert.deepEqual([r.before.length, r.shorts.length, r.after.length], [0, 0, 0]);
});

check("isShort = false hoặc thiếu đều là video thường", () => {
  const r = splitHomeFeed([v("a", false), v("b"), v("s", true)]);
  assert.deepEqual(ids(r.before), ["a", "b"]);
});

check("hàng Shorts bị giới hạn số thẻ", () => {
  const feed = Array.from({ length: 30 }, (_, i) => v(`s${i}`, true));
  assert.equal(splitHomeFeed(feed, 6, 20).shorts.length, 20);
  assert.equal(splitHomeFeed(feed).shorts.length, 20);
});

console.log(`\n${count} phép thử qua`);
