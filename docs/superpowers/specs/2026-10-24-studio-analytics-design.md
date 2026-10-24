# Thiết kế: Số liệu phân tích trong Studio (E1)

Ngày: 2026-10-24. Trạng thái: anh đã duyệt hướng chung (chat 24/10 tối), chờ duyệt bản này.

## 1. Vì sao làm

Studio chỉ có danh sách video kèm tổng lượt xem / số bình luận. Anh muốn xem kênh phát triển thế nào **theo từng ngày**, và "show toàn bộ dữ liệu hiện có".

**Thành công nghĩa là:** vào Studio → "Số liệu phân tích", thấy 4 con số của khoảng đã chọn và một biểu đồ theo ngày cho số đang chọn; số liệu khớp với những gì xảy ra trên site (xem 1 video → hôm nay +1 lượt xem; có người đăng ký → +1 người đăng ký).

## 2. Phạm vi

### Có làm (anh đã duyệt)
1. Mục **"Số liệu phân tích"** ở thanh bên Studio, giữa "Nội dung" và "Tuỳ chỉnh"; trang `/studio/analytics`.
2. **4 ô số** (thẻ bấm được, kiểu YouTube): Lượt xem, Thời gian xem (giờ), Người đăng ký mới, Bình luận. Mỗi ô hiện tổng của khoảng đã chọn. Bấm ô nào thì biểu đồ bên dưới vẽ số đó.
3. **Biểu đồ đường theo ngày**, mỗi ngày một điểm, ngày không có gì là 0.
4. **Chọn khoảng:** 7 ngày, 28 ngày (mặc định), 90 ngày, tính tới hết hôm nay.
5. **Ghi chú nguồn dữ liệu:** Lượt xem và Thời gian xem chỉ có từ ngày deploy (`trackingSince`); trước ngày đó biểu đồ hiện vùng mờ / chú thích "Chưa ghi dữ liệu trước dd/MM/yyyy" để không hiểu nhầm là 0 người xem.

### Em tự quyết (anh xem lại khi đọc)
6. **Chỉ số liệu cả kênh.** Anh chưa trả lời câu "có cần biểu đồ riêng từng video không"; bảng dữ liệu vẫn ghi theo từng video nên thêm sau không phải đổi dữ liệu.
7. **Ngày tính theo giờ Việt Nam** (`Asia/Ho_Chi_Minh`), dù JVM và JSON chạy UTC.
8. **Thư viện biểu đồ:** `recharts@3.10.1` (bộ shadcn dùng cho Chart; bản 25/07/2026, hỗ trợ React 19). Là phụ thuộc mới duy nhất.
9. **Người đăng ký mới** = số bản ghi đăng ký tạo trong ngày (của mọi gói hội viên thuộc kênh). Huỷ đăng ký xoá bản ghi nên **không trừ được** và đăng ký rồi huỷ trong khoảng thì không còn đếm.
10. **Bình luận** = bình luận + trả lời trên video của kênh (trừ video đã xoá), theo ngày tạo, kể cả của chính chủ kênh (như số trên thẻ Studio).
11. **Lượt xem** theo đúng quy tắc đếm hiện có (`registerView`: mỗi người / máy tối đa 1 lượt mỗi khoảng chống trùng), kể cả lượt chủ kênh tự xem — giống tổng lượt xem đang hiện.
12. **Thời gian xem** chỉ tính người **đã đăng nhập** (trình phát chỉ báo vị trí khi đã đăng nhập, API `/progress` cần đăng nhập). Người xem ẩn danh vẫn có lượt xem nhưng không có thời gian xem. Xem mục 6 "Còn mở".

### Không làm
- Doanh thu, nguồn lưu lượng, độ tuổi / vị trí người xem, thời gian thực, so sánh với kỳ trước, xuất CSV.
- Biểu đồ riêng từng video (mục 6).
- Điền ngược lượt xem / thời gian xem quá khứ: không có dữ liệu.

## 3. Dữ liệu

### Bảng mới `video_daily_stats` (runner idempotent, mẫu `job/SearchHistoryTableInitializer.java`, `@Order(0)`)
```sql
CREATE TABLE IF NOT EXISTS video_daily_stats (
    video_id   VARCHAR(36) NOT NULL,
    day        DATE        NOT NULL,
    owner_id   VARCHAR(36) NOT NULL,   -- user sở hữu video, để lọc theo kênh không cần join videos
    views      INT         NOT NULL DEFAULT 0,
    watch_seconds BIGINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (video_id, day),
    INDEX idx_vds_owner_day (owner_id, day)
)
```
- Ghi bằng SQL thẳng `INSERT … ON DUPLICATE KEY UPDATE views = views + ?, watch_seconds = watch_seconds + ?` (cộng nguyên tử, không cần entity). Ghi lỗi thì log `warn` rồi bỏ qua — không làm hỏng việc xem video. Câu ghi chạy trong transaction riêng (`REQUIRES_NEW`) để lỗi của nó không đánh dấu rollback transaction của `registerView`.
- `trackingSince` = ngày nhỏ nhất trong bảng của kênh; chưa có dòng nào thì là hôm nay.

### Nguồn từng số
| Số | Nguồn | Từ khi nào |
|---|---|---|
| Lượt xem | `video_daily_stats.views`, cộng trong `VideoServiceImpl.registerView` khi `counted` | từ lúc deploy |
| Thời gian xem | `video_daily_stats.watch_seconds`, cộng trong `POST /api/videos/{id}/progress` | từ lúc deploy |
| Người đăng ký mới | `subscriptions.created_at`, lọc theo `tier.channel` của kênh | toàn bộ |
| Bình luận | `comments.created_at`, `target_type = VIDEO`, video thuộc chủ kênh | toàn bộ |

### Cách tính thời gian xem
- Trình phát báo vị trí khoảng mỗi 15 giây (lần đầu ở giây thứ 3, và khi dừng / rời trang).
- Mỗi lần báo: lấy vị trí lần báo trước từ Redis key riêng `wt:{userId}:{videoId}` (TTL 1 ngày), `delta = vị trí mới − vị trí trước`. Chỉ cộng khi `0 < delta ≤ 20` giây; tua tới / tua lui / xem lại từ đầu thì lần đó cộng 0. Chưa có vị trí trước thì cộng `min(vị trí, 20)`.
- Key riêng, không dùng key `wp:` của lịch sử, và cộng **trước** kiểm tra "tạm dừng lịch sử": người tạm dừng lịch sử vẫn được tính thời gian xem cho chủ kênh (không lưu gì gắn với họ ngoài key Redis tạm).
- Redis lỗi: lần đó cộng 0 (không chặn việc xem).

## 4. API

`GET /api/studio/analytics?days=7|28|90` — cần đăng nhập, số liệu kênh của người gọi. `days` khác 3 giá trị đó → 400.
```json
{
  "from": "2026-09-27", "to": "2026-10-24",
  "trackingSince": "2026-10-24",
  "totals": { "views": 12, "watchSeconds": 3600, "subscribers": 3, "comments": 5 },
  "daily": [ { "date": "2026-09-27", "views": 0, "watchSeconds": 0, "subscribers": 1, "comments": 0 }, … ]
}
```
- `daily` luôn đủ `days` phần tử, theo thứ tự ngày tăng dần, ngày trống = 0. Ngày là chuỗi `yyyy-MM-dd` theo giờ VN (không phải thời điểm UTC).
- Người dùng chưa có kênh → mọi số 0 (không lỗi).
- Đếm đăng ký / bình luận: lấy `created_at` trong khoảng (đổi biên ngày VN sang UTC) rồi chia ngày ở Java — dữ liệu tối đa 90 ngày, nhỏ.

## 5. Giao diện (`/studio/analytics`)
- Tiêu đề "Số liệu phân tích của kênh", bên phải là `Select` shadcn chọn khoảng.
- Hàng 4 thẻ số; thẻ đang chọn có viền / gạch dưới màu chính. Thời gian xem hiện theo giờ, 1 chữ số thập phân (`0,5`).
- Biểu đồ đường `recharts` (`ResponsiveContainer`, `LineChart`), trục X là ngày `dd/MM`, tooltip hiện ngày + số. Lượt xem / Thời gian xem: phần trước `trackingSince` tô nền xám nhạt (`ReferenceArea`) + chú thích.
- Đang tải: khung xương; lỗi: thông báo + nút thử lại (theo kiểu trang Studio hiện có).
- Dữ liệu qua RTK Query (API mới đăng ký trong `store/store.ts`), `keepUnusedDataFor` ngắn để vào lại trang thấy số mới.

## 6. Còn mở
- **Thời gian xem của người chưa đăng nhập** (mục 12): muốn tính thì phải cho trình phát gửi vị trí cả khi chưa đăng nhập (endpoint mới, không cần đăng nhập, có giới hạn tần suất, khoá Redis theo `anonId`). Em đề xuất để sau; cần anh chọn.

## 7. Kiểm tra
- Test backend: upsert cộng dồn đúng; tính `delta` (bình thường, tua tới, tua lui, chưa có vị trí trước, Redis lỗi); API trả đủ `days` ngày, ngày trống = 0, chia ngày theo giờ VN (bản ghi 23:30 UTC rơi vào ngày hôm sau ở VN); `days` sai → 400; chưa có kênh → 0.
- Client: tsc / eslint / `next build`.
- Sau deploy: log có dòng tạo bảng; `GET /api/studio/analytics` trả 401 khi chưa đăng nhập; `/studio/analytics` trả 200. Anh thử: xem một video → tải lại trang phân tích thấy Lượt xem hôm nay +1.
