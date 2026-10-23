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

### Gói 3: Redis (commit `4a68f5d`, deploy 2026-10-23 20:03, chỉ backend)
- Bật Spring Data Redis, đọc `REDIS_URL`. Không có `REDIS_URL` thì không kết nối (chạy local vẫn được). Redis lỗi thì tạm bỏ qua Redis 30 giây và chạy theo cách cũ, không làm sập site (`service/impl/RedisServiceImpl.java`).
- **Chống đếm trùng lượt xem** bằng `SET view:{video}:{người xem} NX EX 1800`: không còn mất khi deploy lại. Dự phòng: bộ nhớ trong như trước.
- **Cache gợi ý trang chủ** 5 phút cho mỗi người (`rec:{userId}`), xoá khi người đó bấm Không thích. Bỏ `@Cacheable` cũ (chưa bao giờ chạy vì gọi nội bộ trong cùng class).
- **Giới hạn tần suất** (`security/RateLimiter.java`, vượt thì trả 429 kèm thông báo tiếng Việt; Redis lỗi thì đếm tạm trong bộ nhớ):

  | Việc | Giới hạn |
  |---|---|
  | AI viết mô tả / dịch transcript | 20 lần/giờ mỗi người |
  | Tạo phụ đề AI | 5 lần/giờ mỗi người (lúc upload mà vượt thì vẫn upload, chỉ bỏ qua phụ đề) |
  | Gửi lượt xem | 120 lần/10 phút mỗi IP |
  | Bình luận | 10 lần/phút mỗi người |
  | Báo cáo video | 20 lần/giờ mỗi người |

**Đã kiểm tra trên server:** log có "Kết nối Redis OK (PONG)", không có ERROR; lượt xem tính 1 lần rồi gửi lại không tăng (video `be49c4b5…` lên 10 lượt do lần thử này); gửi 121 lần thì từ lần thứ 121 trả 429; gọi gợi ý 3 lần chỉ tính 1 lần (lần đầu 1,4 giây, sau đó khoảng 0,6 giây). Header `X-Forwarded-For` giả không lách được giới hạn theo IP (proxy của Vibe Host ghi đè).

### Gói 4: giờ đăng + trình phát mới (commit `fdb1ae9`, `435b5d7`, deploy 2026-10-23 20:47)
- **Giờ đăng lệch:** trang chủ "6 giờ trước", trang xem "13 giờ trước". Nguyên nhân: DB lưu giờ UTC nhưng API trả không kèm múi giờ (trình duyệt hiểu là giờ VN), còn trang xem chỉ trả ngày (`2026-10-23`, bị hiểu là 7h sáng). Sửa: mọi thời gian trong JSON có dạng `…Z` (`configuration/JacksonConfig.java`), JVM cố định UTC, trang xem / playlist / Studio trả đủ giờ. Danh sách video bên phải trang xem hiện "x giờ trước" thay vì chuỗi thô.
- **Trình phát viết lại** (`modules/watch/components/video/`), dùng thẻ `<video>` của trình duyệt, bỏ `react-player`:
  - Lỗi cũ đã bỏ: một trình phát ẩn (miniplayer) tải song song cùng video và giành thanh tiến trình; kéo thanh tua là video dừng luôn; hết video thì hiện lưới ảnh giả; trạng thái dính sang video sau.
  - Điều khiển giống YouTube: thanh tua (phần đã tải, rê chuột xem thời điểm, kéo bằng chuột/ngón tay), phát/xem lại, âm lượng (rê để chỉnh, nhớ giữa các lần), thời gian, nút phụ đề, cài đặt (tốc độ, chọn ngôn ngữ phụ đề), trình phát thu nhỏ (PiP), toàn màn hình thật. Tự ẩn khi đang phát, vòng xoay khi tải, tự phát, `?t=`.
  - Phím tắt: Space/K, J/L (±10s), ←/→ (±5s), ↑/↓ (âm lượng, khi đang ở trên video), M, F, C, 0–9, Home/End, `<` `>` (tốc độ).

**Đã kiểm tra:** 3 API (danh sách, chi tiết, gợi ý) trả cùng `2026-10-23T06:17:16.742Z` (13:17 giờ VN); `next build` và kiểm tra kiểu qua; các trang trả 200; log không lỗi. **Chưa kiểm tra:** bấm thử trình phát trên trình duyệt thật (em không có trình duyệt), anh cần thử.

### Gói 5: giao diện trang xem (commit `d9a7ec5`, deploy 2026-10-23 21:06, chỉ client)
- Thanh bên trên trang xem đóng sẵn, bấm ☰ thì mở dạng drawer đè lên trang (có logo, tự đóng khi chuyển trang). Làm bằng chế độ `drawer` mới của `SidebarProvider`.
- Chế độ rạp: nút trên trình phát + phím T, nhớ lựa chọn; bật/tắt không làm video tải lại.
- Menu cài đặt của trình phát: nền kính mờ thay cho nền đen.
- Nút "Tạo" trên thanh trên cùng: gọn lại thành dạng viên thuốc (màn hình nhỏ chỉ còn nút tròn).

**Chưa kiểm tra trên trình duyệt thật** (chỉ build, kiểm tra kiểu, các trang trả 200).

### Gói 6: A1–A5 (commit `e4a1e8d`, `c06b722`)
- **A1 Nhớ vị trí đang xem:** trình phát gửi vị trí mỗi 15 giây khi đang phát, và gửi ngay khi dừng / hết video / ẩn tab / rời trang (`POST /api/videos/{id}/progress?t=&final=`). Vị trí mới nhất nằm trong Redis (`wp:{user}:{video}`); MySQL (`history_duration`) ghi tối đa mỗi phút, hoặc ngay khi `final`. Mở lại video thì phát tiếp (trừ khi mới xem dưới 5 giây hoặc đã gần hết; `?t=` được ưu tiên). Thanh đỏ trên thumbnail ở trang chủ, trang kênh, lịch sử, playlist, danh sách bên phải trang xem; mỗi trang tải vị trí bằng một request (`GET /api/videos/progress?ids=`). Xoá video khỏi lịch sử thì quên luôn vị trí. Người chưa đăng nhập: không lưu (giống YouTube).
- **A2** Lịch sử được ghi khi thực sự xem, không còn ghi lúc bấm thumbnail; mở bằng link ngoài cũng được ghi.
- **A3** Hết video: thẻ "Tiếp theo trong 5" (Huỷ / Phát ngay) khi bật Tự phát, ngược lại là lưới gợi ý + Xem lại. Công tắc Tự phát trên thanh điều khiển, có nhớ.
- **A4 Thông báo:** bảng `notifications` (tạo lúc khởi động bởi `NotificationTableInitializer`). Video công khai (lúc upload, hoặc chuyển sang công khai) báo cho người đăng ký còn bật thông báo, mỗi video một lần; trả lời bình luận báo người viết bình luận gốc; bình luận mới báo chủ video. Chuông trên thanh trên cùng: số chưa đọc (hỏi mỗi 60 giây), danh sách, đánh dấu đã đọc.
- **A5 Bảng tin Đăng ký** (`/feed/subscribed`): video công khai mới nhất của các kênh đã đăng ký, cuộn vô hạn (`GET /api/feed/subscriptions`). Giao diện bong bóng kênh cũ (`VideoBubblePack`) vẫn còn trong code, không còn dùng ở trang này.

**Đã kiểm tra:** biên dịch backend, kiểm tra kiểu + lint + `next build` client. **Chưa kiểm tra trên server / trình duyệt thật** cho tới khi deploy.

---

## 2. ĐANG LÀM / DỞ DANG

### 2.1 Anh test khi đăng nhập (chưa ai kiểm tra)
Làm theo `TEST_CHECKLIST.md` mục **11–15**, dùng 2 tài khoản (A và B):
- Riêng tư / không công khai (cần cột `videos_status` ghi được giá trị mới: nên thử đầu tiên).
- Xoá video, không thích, trả lời/sửa bình luận, chia sẻ, lưu playlist, chip theo tag, đăng ký và huỷ đăng ký.
- Mục 1–10 của checklist (đăng nhập, báo cáo, phụ đề AI, phòng xem chung, MoMo) vẫn chưa test hết.

Nếu có lỗi: ghi giờ + màn hình, em tra log (`get_runtime_logs`).

### 2.2 Redis: phần còn lại
- Code đã xong (xem gói 3 ở trên). Cấu hình Redis trên Vibe Host đề xuất: 0,25 core (tối đa 0,5), RAM 64–128 MB, `maxmemory-policy allkeys-lru`, tắt lưu đĩa (chỉ dùng làm cache). Em chưa kiểm tra Redis đang chạy với cấu hình nào.
- Client chưa hiện thông báo khi bị 429 ở ô bình luận (bình luận không gửi được nhưng chữ vẫn còn trong ô, lỗi chỉ ghi ra console). Phụ đề AI và báo cáo thì đã hiện thông báo lỗi của backend.
- Sau này nếu cần: gộp lượt xem rồi ghi MySQL theo lô; lưu trạng thái phòng xem chung vào Redis.

---

## 3. SẼ LÀM: danh sách đầy đủ để giống YouTube

Cột "Ở đâu": BE = backend, FE = client. Cột "Cỡ": S = vài giờ, M = khoảng 1 ngày, L = vài ngày.
Những dòng ghi "chưa xem kỹ" là em chưa đọc code phần đó, hiện trạng có thể khác.

### A. Ưu tiên cao: người xem thấy ngay
| # | Việc | Ở đâu | Cỡ | Hiện trạng / ghi chú |
|---|---|---|---|---|
| A1 | ✅ **Nhớ vị trí đang xem**: phát tiếp từ chỗ dừng, thanh đỏ trên thumbnail | BE+FE | M | Cột `historyDuration` có sẵn nhưng luôn 0; thanh đỏ đã viết trong `VideoCard` nhưng không bao giờ hiện. Ghi vị trí vào Redis, định kỳ ghi MySQL |
| A2 | ✅ **Ghi lịch sử khi thực sự xem** (không phải lúc bấm thumbnail) | FE | S | Mở bằng link chia sẻ hiện không được ghi lịch sử. Làm cùng A1 |
| A3 | ✅ **Màn hình hết video + tự phát video tiếp theo** (đếm ngược, nút huỷ, công tắc Tự phát) | FE | M | Hiện chỉ có nút xem lại |
| A4 | ✅ **Thông báo (chuông)**: kênh đã đăng ký có video mới, có người trả lời / thích bình luận | BE+FE | L | Đã ẩn nút; cần bảng thông báo, API, đếm chưa đọc |
| A5 | ✅ **Trang Đăng ký dạng bảng tin**: video mới của các kênh đã đăng ký | BE+FE | M | Hiện là giao diện bong bóng kênh |
| A6 | **Bình luận**: sắp xếp Nổi bật / Mới nhất, ghim, tim của chủ kênh, báo cáo bình luận | BE+FE | M | Chưa có |
| A7 | **Báo lỗi khi bị giới hạn (429)** ở ô bình luận | FE | S | Hiện bình luận không gửi được mà không báo gì |
| A8 | **Gắn tag cho video hiện có** để chip trang chủ có dữ liệu | dữ liệu | S | Hiện chỉ có chip "Tất cả" |

### B. Trình phát
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| B1 | Chọn chất lượng (360p/720p/1080p, Tự động) | BE+FE | L | Cần Cloudinary tạo sẵn các bản hoặc phát HLS; video dài cần xử lý nền (eager/async) |
| B2 | Ảnh xem trước khi rê trên thanh tua | FE | M | Cloudinary cắt được khung hình theo giây (`so_`) |
| B3 | Chương (chapters) lấy từ mốc thời gian trong mô tả | FE | M | Thanh tua chia đoạn, tên chương |
| B4 | Mốc thời gian / link trong mô tả bấm được | FE | S | |
| B5 | Miniplayer khi rời trang xem (video vẫn chạy ở góc) | FE | L | Hiện chỉ có PiP của trình duyệt |
| B6 | Phát theo playlist (`?list=`): hàng chờ, tự sang video sau, lặp / trộn | FE | M | |
| B7 | Lặp video, chuột phải ra menu (sao chép link tại thời điểm…) | FE | S | |

### C. Khám phá & tìm kiếm
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| C1 | Bộ lọc tìm kiếm: thời lượng, ngày đăng, loại (video/Shorts/kênh), sắp xếp | BE+FE | M | |
| C2 | Lịch sử tìm kiếm trong ô gợi ý (xoá từng mục) | BE+FE | S | |
| C3 | Gợi ý dùng cả lịch sử xem, ẩn video đã xem hết, nút "Không quan tâm" | BE+FE | M | Hiện chỉ dựa vào điểm tag |
| C4 | Trang Khám phá (Âm nhạc, Thể thao, Tin tức, Trò chơi) dùng dữ liệu thật | BE+FE | M | Chưa xem kỹ: các trang này có banner, có thể còn dữ liệu mẫu |
| C5 | Cuộn vô hạn ở trang chủ / kênh / tìm kiếm | FE | S | Chưa xem kỹ |

### D. Lịch sử, playlist, Shorts
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| D1 | Lịch sử xem: xoá từng mục, xoá hết, tạm dừng ghi, tìm trong lịch sử | BE+FE | M | |
| D2 | Playlist: kéo thả sắp xếp, đặt ảnh bìa, sửa tên / mô tả | BE+FE | M | |
| D3 | Shorts: bình luận ngay trong Shorts, vuốt lên/xuống, nhớ âm lượng | FE | M | Hiện nút bình luận mở trang xem |

### E. Cho người làm video (Studio)
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| E1 | Thống kê: lượt xem theo ngày, thời gian xem, người đăng ký mới, video nổi bật | BE+FE | L | |
| E2 | Duyệt / sửa phụ đề AI trước khi đăng | BE+FE | M | PRD có yêu cầu |
| E3 | Thanh tiến trình upload + trạng thái "đang xử lý" | FE | S | Chưa xem kỹ |
| E4 | Đặt lịch công khai video | BE+FE | M | |
| E5 | Quản lý bình luận của kênh trong Studio | BE+FE | M | Chưa xem kỹ (đã có kiểm duyệt bình luận ở trang xem) |

### F. Nền tảng
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| F1 | Thẻ chia sẻ og:title / og:image, sitemap | FE | S | Link dán vào Facebook/Zalo hiện ảnh + tiêu đề |
| F2 | Trang nhúng `/embed/{id}` + mã nhúng trong hộp thoại Chia sẻ | FE | M | |
| F3 | Giao diện tối | FE | M | Anh chọn để dành |
| F4 | Kiểm tra giao diện điện thoại toàn site | FE | M | Trình phát mới / drawer / chế độ rạp chưa thử trên máy thật |

### G. Dọn dẹp / kỹ thuật
- Xoá project wetube-admin trên Vibe Host (không còn dùng; anh làm trong giao diện).
- `JWT_PRIVATE_KEY` và `JWT_PUBLIC_KEY` đang **không** được đánh dấu secret trong Vibe Host. Khoá riêng nên đánh dấu secret.
- Xác nhận chữ ký MoMo với giao dịch sandbox thật (chưa từng thử).
- Số đếm video trong playlist vẫn tính cả video đã bị ẩn (danh sách thì đã lọc đúng).
- Quyết định với Gradle wrapper 9.7.1 trong máy (xem mục 4).
- Test hết `TEST_CHECKLIST.md` mục 1–15 (mục 2.1).

---

## 4. GHI NHỚ KHI LÀM TIẾP

- **Deploy:** `git push` lên `main`, rồi `redeploy_project` (backend `cmvkfhypr...`, client `cmvkg1ilo...`), chờ job xong, đọc log. Chờ 60–90 giây nếu báo cooldown. Backend khởi động mất khoảng 30 giây.
- **Quyền:** bộ phân quyền tự động chặn `redeploy_project` cho tới khi anh nói rõ trong chat. Muốn khỏi phải nói: thêm `"mcp__vibehost__redeploy_project"` vào `permissions.allow` trong `.claude/settings.local.json`.
- **Schema DB:** production chạy `ddl-auto=none`, nên mọi thay đổi bảng phải làm bằng runner lúc khởi động (mẫu: `job/VideoSchemaInitializer.java`, `job/ReportTableInitializer.java`). Cột `videos_status` là ENUM, chỉ nối thêm giá trị vào cuối.
- **Backup:** nên backup MySQL trước mỗi lần thay đổi schema.
- **Cách nói chuyện:** tiếng Việt; nói rõ cái gì đã kiểm tra thật, cái gì chỉ đoán; sửa gộp rồi deploy một lần.
- **Gradle wrapper trong máy đã bị đổi sang Gradle 9.7.1** (`gradle/wrapper/*`, `gradlew`, `gradlew.bat`, sửa lúc 19:23 ngày 23/10, không phải em đổi). Em chưa commit các file này. Docker build dùng Gradle 8.14 của image nên không bị ảnh hưởng. Biên dịch local cho giống production: dùng Gradle 8.14.3 trong `~/.gradle/wrapper/dists/gradle-8.14.3-bin/.../bin/gradle compileJava`.
