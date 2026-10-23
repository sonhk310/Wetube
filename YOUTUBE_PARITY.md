# WeTube → "YouTube của riêng mình": đã làm, đang làm, sẽ làm

Cập nhật: 2026-10-23. File này để anh đọc ở nhà rồi làm tiếp. Không chứa mật khẩu hay khoá nào.

- Client: https://wetube.vibe.tinhgon.xyz
- Backend: https://wetube-service.vibe.tinhgon.xyz
- Vibe Host: backend `cmvkfhypr07ra0i5f5b51wed1`, client `cmvkg1ilo084w0i5fhrmxxz90`
- Tài liệu liên quan: `PRD.md` (yêu cầu gốc), `TEST_CHECKLIST.md` (việc cần tự test khi đăng nhập)
- Quy tắc làm việc: sửa gộp một lượt rồi deploy một lần (backend trước, client sau); biên dịch backend bằng `./gradlew compileJava -q --offline`, kiểm tra client bằng `npx tsc --noEmit` (bỏ qua lỗi TS7006 cũ).

---

## 1. ĐÃ LÀM VÀ ĐÃ DEPLOY

### Nền tảng (trước hôm nay)
- Quyền: đọc công khai, mọi thao tác ghi cần đăng nhập, danh tính lấy từ JWT, kiểm tra chủ sở hữu (bình luận, bài đăng, playlist, kênh...).
- Việt hoá toàn bộ giao diện; Gemini tạo phụ đề AI; Shorts thật; báo cáo video và duyệt báo cáo cho admin; JWT key lấy từ biến môi trường.

### Gói 1 + gói 2 (commit `7f2a0b8`, deploy 2026-10-23, backend 09:45, client 09:50)
**Gói 1: nền tảng**
- **Xử lý lỗi:** thiếu/sai tham số trả 400, sai phương thức 405, file quá lớn 413; video không tồn tại trả 404 (trước là 500); lỗi 500 thật được ghi log.
- **Một quy tắc hiển thị video cho mọi nơi** (`security/VideoAccess.java`):
  - `ACTIVE`/`PUBLIC`: có trong danh sách và ai cũng xem được.
  - `UNLISTED`: không có trong danh sách, xem được khi có link.
  - `PRIVATE` và `INACTIVE` (admin ẩn): chỉ chủ video và admin xem được, người khác nhận 404.
  - `DELETED`: không ai xem được.
- Elasticsearch chỉ giữ video công khai. Sửa lỗi cũ: mỗi lần khởi động đã đưa video bị admin ẩn quay lại tìm kiếm.
- **Đếm lượt xem đúng:** `POST /api/videos/{id}/view` (khách chưa đăng nhập cũng được), mỗi người một lượt cho mỗi video trong 30 phút. Trang xem tính sau 5 giây phát, Shorts sau 3 giây hiện trên màn hình.

**Gói 2: giống YouTube**
- Chọn chế độ hiển thị (Công khai / Không công khai / Riêng tư) khi upload và trong Studio; Studio có nhãn hiển thị.
- Xoá video (xoá mềm) trong Studio, kèm xoá file trên Cloudinary.
- Nút Không thích (chỉ hiện trạng thái của mình, không hiện số lượng).
- Bình luận: "Trả lời" và "Sửa".
- Chia sẻ: link, "Bắt đầu tại m:ss", Facebook, X; trang xem đọc `?t=` để tua.
- Nút "Lưu" vào playlist (tích chọn playlist, tạo playlist mới ngay trong hộp thoại).
- Chip trang chủ lấy từ tag thật; gợi ý tìm kiếm khớp theo tiền tố ("vi" ra "view").
- Trang kênh nhớ trạng thái đăng ký và huỷ đăng ký được.
- Đã ẩn nút "Cảm ơn" và chuông thông báo.

**Đã kiểm tra trên server:** lỗi 400/404, endpoint mới trả 401 khi chưa đăng nhập, đếm lượt xem (7 → 9, gửi lại không tăng), gợi ý "vi", log không có ERROR, các trang client trả 200.

---

## 2. ĐANG LÀM / DỞ DANG

### 2.1 Anh test khi đăng nhập (chưa ai kiểm tra)
Làm theo `TEST_CHECKLIST.md` mục **11–15**, dùng 2 tài khoản (A và B):
- Riêng tư / không công khai (cần cột `videos_status` ghi được giá trị mới: nên thử đầu tiên).
- Xoá video, không thích, trả lời/sửa bình luận, chia sẻ, lưu playlist, chip theo tag, đăng ký và huỷ đăng ký.
- Mục 1–10 của checklist (đăng nhập, báo cáo, phụ đề AI, phòng xem chung, MoMo) vẫn chưa test hết.

Nếu có lỗi: ghi giờ + màn hình, em tra log (`get_runtime_logs`).

### 2.2 Redis (anh đã tạo và gắn vào wetube-service)
- Biến môi trường `REDIS_URL` (secret) đã có trong project backend. **Chưa có dòng code nào dùng Redis** — `RedisConfig`, `RedisService` và cấu hình prod vẫn đang bị comment.
- Cấu hình đề xuất: 0,25 core (tối đa 0,5), RAM 64–128 MB, `maxmemory-policy allkeys-lru`, tắt lưu đĩa (chỉ dùng làm cache).
- Kế hoạch code (một lượt, làm sau khi anh test xong gói 2):
  1. Bật lại Spring Data Redis, đọc `REDIS_URL`. Mọi chỗ dùng đều có dự phòng: Redis lỗi thì chạy như cũ, không làm sập site.
  2. Chống đếm trùng lượt xem bằng `SET key NX EX 1800` (hiện nằm trong RAM nên mất mỗi lần deploy).
  3. Cache gợi ý trang chủ (TTL 5–10 phút), xoá cache của user khi họ bấm Không thích.
  4. Giới hạn tần suất cho API gọi Gemini (tốn tiền) và cho `/view`, bình luận, báo cáo.
  5. Sau này nếu cần: gộp lượt xem rồi ghi MySQL theo lô; lưu trạng thái phòng xem chung.

---

## 3. SẼ LÀM (xếp theo mức ưu tiên em đề xuất)

### A. Làm tiếp ngay sau khi test xong
1. Redis (mục 2.2).
2. Sửa các lỗi anh tìm thấy từ checklist.
3. Thêm tag cho video hiện có để chip trang chủ có dữ liệu (hiện chỉ có "Tất cả").

### B. Thiếu so với YouTube, nên làm
| Việc | Ghi chú |
|---|---|
| Thông báo (chuông) | Ví dụ: video mới của kênh đã đăng ký, có người trả lời bình luận. Đã ẩn nút, cần backend + bảng thông báo |
| Trang Đăng ký dạng bảng tin video mới | Hiện là giao diện bong bóng kênh, chưa có danh sách video mới |
| Thống kê trong Studio | Lượt xem theo ngày, thời gian xem, người đăng ký mới |
| SEO / thẻ chia sẻ (og:title, og:image) | Để link WeTube hiện đẹp khi dán vào Facebook/Zalo |
| Giao diện tối | Chưa làm, anh chọn để dành |
| Trình phát: chất lượng video, tự phát tiếp, picture-in-picture, chế độ rạp, phím tắt | Hiện chỉ có phát/tạm dừng/tua/âm lượng |
| Tìm kiếm: bộ lọc (thời lượng, ngày đăng, sắp xếp) | |
| Bình luận: sắp xếp (mới nhất/nổi bật), ghim, báo cáo bình luận | |
| Lịch sử xem: xoá từng mục / tạm dừng ghi lịch sử | |
| Playlist: sắp xếp lại thứ tự video | |
| Shorts: bình luận ngay trong Shorts | Hiện nút bình luận chỉ mở trang xem |
| Creator duyệt/sửa phụ đề AI trước khi đăng | PRD có yêu cầu |
| Gợi ý: dùng cả lịch sử xem, không chỉ điểm tag | Điểm không thích đã trừ điểm tag |

### C. Dọn dẹp / kỹ thuật
- Xoá project wetube-admin trên Vibe Host (không còn dùng; anh làm trong giao diện).
- `JWT_PRIVATE_KEY` và `JWT_PUBLIC_KEY` đang **không** được đánh dấu secret trong Vibe Host. Khoá riêng nên đánh dấu secret.
- Xác nhận chữ ký MoMo với giao dịch sandbox thật (chưa từng thử).
- Số đếm video trong playlist vẫn tính cả video đã bị ẩn (danh sách thì đã lọc đúng).

---

## 4. GHI NHỚ KHI LÀM TIẾP

- **Deploy:** `git push` lên `main`, rồi `redeploy_project` (backend `cmvkfhypr...`, client `cmvkg1ilo...`), chờ job xong, đọc log. Chờ 60–90 giây nếu báo cooldown. Backend khởi động mất khoảng 30 giây.
- **Quyền:** bộ phân quyền tự động chặn `redeploy_project` cho tới khi anh nói rõ trong chat. Muốn khỏi phải nói: thêm `"mcp__vibehost__redeploy_project"` vào `permissions.allow` trong `.claude/settings.local.json`.
- **Schema DB:** production chạy `ddl-auto=none`, nên mọi thay đổi bảng phải làm bằng runner lúc khởi động (mẫu: `job/VideoSchemaInitializer.java`, `job/ReportTableInitializer.java`). Cột `videos_status` là ENUM, chỉ nối thêm giá trị vào cuối.
- **Backup:** nên backup MySQL trước mỗi lần thay đổi schema.
- **Cách nói chuyện:** tiếng Việt; nói rõ cái gì đã kiểm tra thật, cái gì chỉ đoán; sửa gộp rồi deploy một lần.
- Mã nguồn cũ chưa commit: `.gitignore` (thêm `.mcp.json`) vẫn đang sửa dở, chưa nằm trong commit nào.
