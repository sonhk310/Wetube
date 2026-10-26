// Chạy: node scripts/check-roomSync.ts   (Node 22.6+; không cần cài gì)
import assert from "node:assert/strict";
import {
  correction,
  estimateOffset,
  expectedPosition,
  formatClock,
  RESUME_SEEK_THRESHOLD,
  parseRoomCode,
} from "../src/lib/roomSync.ts";

let count = 0;
function check(name: string, fn: () => void) {
  fn();
  count++;
  console.log(`ok  ${name}`);
}

check("đang phát: vị trí chạy theo giờ server", () => {
  const state = { playing: true, position: 30, updatedAt: 1_000, version: 1 };
  assert.equal(expectedPosition(state, 1_000), 30);
  assert.equal(expectedPosition(state, 3_500), 32.5);
});

check("đang dừng: vị trí đứng yên", () => {
  const state = { playing: false, position: 30, updatedAt: 1_000, version: 1 };
  assert.equal(expectedPosition(state, 9_000), 30);
});

check("giờ máy chạy lùi so với updatedAt không làm vị trí lùi", () => {
  const state = { playing: true, position: 30, updatedAt: 5_000, version: 1 };
  assert.equal(expectedPosition(state, 4_000), 30);
});

check("độ lệch đồng hồ: lấy mẫu khứ hồi ngắn nhất", () => {
  const offset = estimateOffset([
    { clientSent: 0, serverTime: 5_500, clientReceived: 1_000 }, // rtt 1000, bất đối xứng: offset 5000
    { clientSent: 2_000, serverTime: 7_060, clientReceived: 2_120 }, // rtt 120: offset 5000
  ]);
  assert.equal(offset, 5_000);
});

check("độ lệch đồng hồ: không có mẫu hợp lệ thì 0", () => {
  assert.equal(estimateOffset([]), 0);
  assert.equal(estimateOffset([{ clientSent: 100, serverTime: 1, clientReceived: 50 }]), 0);
});

check("sửa lệch: nhỏ thì để yên", () => {
  assert.deepEqual(correction(10.05, 10, true), { kind: "none" });
  assert.deepEqual(correction(9.95, 10, false), { kind: "none" });
});

check("sửa lệch: lệch ít thì chỉnh tốc độ 5%, lệch vừa thì 10%", () => {
  assert.deepEqual(correction(10.15, 10, true), { kind: "rate", rate: 0.95 }); // nhanh hơn: chậm lại
  assert.deepEqual(correction(9.85, 10, true), { kind: "rate", rate: 1.05 }); // chậm hơn: nhanh lên
  assert.deepEqual(correction(10.25, 10, true), { kind: "rate", rate: 0.9 });
  assert.deepEqual(correction(9.75, 10, true), { kind: "rate", rate: 1.1 });
});

check("sửa lệch: vừa bắt đầu chạy thì ngưỡng tua thấp hơn", () => {
  assert.deepEqual(correction(9.8, 10, true, RESUME_SEEK_THRESHOLD), { kind: "seek", to: 10 });
  assert.deepEqual(correction(9.9, 10, true, RESUME_SEEK_THRESHOLD), { kind: "rate", rate: 1.05 });
});

check("sửa lệch: lớn thì tua thẳng", () => {
  assert.deepEqual(correction(12, 10, true), { kind: "seek", to: 10 });
  assert.deepEqual(correction(9.6, 10, true), { kind: "seek", to: 10 });
  assert.deepEqual(correction(5, 10, false), { kind: "seek", to: 10 });
});

check("sửa lệch: đang dừng thì không chỉnh tốc độ, lệch vừa cũng tua", () => {
  assert.deepEqual(correction(10.3, 10, false), { kind: "seek", to: 10 });
});

check("định dạng giờ", () => {
  assert.equal(formatClock(200), "3:20");
  assert.equal(formatClock(3725), "1:02:05");
  assert.equal(formatClock(-4), "0:00");
});

check("tách mã phòng từ mã trần và link mời", () => {
  assert.equal(parseRoomCode("abC-d3fG-h1J"), "abC-d3fG-h1J");
  assert.equal(parseRoomCode("  https://wetube.vibe.tinhgon.xyz/rooms/abC-d3fG-h1J  "), "abC-d3fG-h1J");
  assert.equal(parseRoomCode("https://x.y/rooms/abC-d3fG-h1J?from=1"), "abC-d3fG-h1J");
  assert.equal(parseRoomCode("abc"), null);
  assert.equal(parseRoomCode("https://x.y/watch/123"), null);
});

console.log(`\n${count} phép thử đều qua`);
