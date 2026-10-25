# WeTube → "YouTube của riêng mình": đã làm, đang làm, sẽ làm

Cập nhật: 2026-10-24. File này để anh đọc ở nhà rồi làm tiếp. Không chứa mật khẩu hay khoá nào.

- Client: https://wetube.vibe.tinhgon.xyz
- Backend: https://wetube-service.vibe.tinhgon.xyz
- Vibe Host: backend `cmvkfhypr07ra0i5f5b51wed1`, client `cmvkg1ilo084w0i5fhrmxxz90`
- Tài liệu liên quan: `PRD.md` (yêu cầu gốc), `TEST_CHECKLIST.md` (việc cần tự test khi đăng nhập)
- Quy tắc làm việc: sửa gộp một lượt rồi deploy một lần (backend trước, client sau); biên dịch backend bằng `./gradlew --no-daemon compileJava compileTestJava -q` (wrapper 8.14.3, giống production; xem mục 4), kiểm tra client bằng `npx tsc --noEmit` (bỏ qua lỗi TS7006 cũ).

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

**Đã kiểm tra:** deploy 2026-10-23 ~22:40, log không lỗi, các API mới trả 401 khi chưa đăng nhập, `/detail` có `resumeAt`, các trang trả 200. Anh đã thấy thanh đỏ hoạt động trên trình duyệt thật.

### Gói 7: sửa sau khi anh test (commit `d72a347`, `35fe326`, `88b24f4`, deploy 2026-10-24 sáng)
- **Thanh đỏ rơi xuống đáy trang:** `VideoOverlay` dùng `absolute` nhưng khung trong `VideoCard` không `relative`. Giờ `VideoThumbnail` nhận `progress` và tự vẽ thanh.
- **Bấm lại video xem dở mà phát từ đầu:** RTK Query giữ cache `/detail` 60 giây (bản cũ không có `resumeAt`). Sửa: `keepUnusedDataFor: 0` cho `getVideoDetail`; trình phát ưu tiên vị trí trong `useWatchProgressStore` (trình duyệt) rồi mới tới `resumeAt`.
- **Thẻ video kiểu YouTube** (`modules/home/ui/components/video/videoCard.tsx`): rê chuột ~0,6 giây thì phát xem trước không tiếng (từ chỗ đã xem), thanh đỏ chạy, thời gian đếm ngược, nút tắt/bật tiếng, nền xám nhạt quanh thẻ; link thật tới video và kênh; menu ⋮ (Xem sau, Lưu vào danh sách phát, Chia sẻ). Backend thêm `channelId` vào `RecommendVideoDto` và `VideoDocument` (Elasticsearch tự đồng bộ lại lúc khởi động).
- **Trang bị thu hẹp khi mở menu / chuông:** Radix khoá cuộn và chèn `body[data-scroll-locked] { margin-right: … !important }`; thanh cuộn nằm ở `html` nên khoản bù đó thừa. Ghi đè bằng `html body[data-scroll-locked]` trong `globals.css`.
- **Lỗi từ gói 5** (đã sửa): `html, body { overflow-y: scroll }` làm `body` chừa rãnh cuộn trống → chỉ còn `html`. `tw-animate-css` chưa được import (Tailwind v4 không đọc `tailwind.config.ts`) → đã import, drawer / dialog / dropdown có animation.
- **Thanh chip trang chủ** (`components/filter-carousel.tsx`): chip sát mép trái, mũi tên + dải mờ chỉ hiện ở phía còn cuộn được, dời lên một chút. Sidebar: "Dịch vụ khác của WeTube" → "Thêm từ WeTube".

**Chưa kiểm tra trên trình duyệt thật:** xem trước khi rê chuột, menu ⋮, thanh chip, hết thu hẹp trang (anh sẽ thử).

### Gói 8: A6 + A7, bình luận kiểu YouTube (commit `c876b45`, deploy 2026-10-24 07:52 backend, 07:56 client giờ VN)
- **Sắp xếp** "Bình luận hàng đầu" (thích + trả lời nhiều trước) / "Mới nhất xếp trước": `GET /api/comments/VIDEO/{id}?sort=top|newest` (mặc định `top`). Bình luận đã ghim luôn ở đầu. Số bình luận tính cả phản hồi.
- **Ghim** (chủ video, chỉ bình luận gốc, mỗi video 1 cái): `POST|DELETE /api/comments/{id}/pin`. Hiện "📌 Đã ghim bởi {kênh}".
- **Tim của chủ kênh**: `POST|DELETE /api/comments/{id}/heart`. Hiện ảnh kênh kèm tim đỏ; người viết nhận thông báo `COMMENT_HEART` (mỗi bình luận một lần).
- Ghim / tim: chỉ đúng chủ video (admin cũng không, vì hiển thị là của chủ kênh). Cập nhật bằng câu `UPDATE` thẳng để không đổi `updatedAt`.
- **Chủ video gỡ được bình luận** của người khác dưới video mình (`OwnershipGuard.commentRemovable`); sửa vẫn chỉ người viết.
- **Báo cáo bình luận**: `POST /api/reports` nhận `commentId` (bảng `reports` thêm cột `comment_id`). Admin có nút "Xoá bình luận" (`action=DELETE_COMMENT`), Nhật ký báo cáo hiện nội dung bình luận. Không báo cáo được bình luận của chính mình. Hộp thoại báo cáo dùng chung (`modules/reports/report-dialog.tsx`).
- Thẻ bình luận: menu ⋮ (Ghim / Chỉnh sửa / Xoá hoặc Gỡ bỏ / Báo cáo; màn hình lớn chỉ hiện khi rê chuột), nhãn tên xám cho chủ kênh, "x giờ trước" + "(đã chỉnh sửa)", số thích hiện cả khi chưa đăng nhập.
- **A7**: lỗi khi gửi bình luận / phản hồi / sửa / xoá / ghim / tim hiện toast (lấy `message` của backend, vd. 429 "Bạn bình luận quá nhanh..."), chữ vẫn giữ trong ô (`lib/apiError.ts`).
- Schema: `job/CommentSchemaInitializer.java` thêm `comments.pinned`, `comments.hearted`, `reports.comment_id` nếu chưa có (mỗi câu một transaction riêng).

**Đã kiểm tra:** biên dịch backend, `tsc`, eslint, `next build`; trên server: log không có ERROR, `GET /api/comments/VIDEO/{id}?sort=top|newest` trả 200 (câu SQL có cột `pinned`, `hearted` nên các cột đã có; runner không ghi log nghĩa là cả 3 cột có sẵn từ trước), pin / heart / báo cáo bình luận trả 401 khi chưa đăng nhập, các trang client trả 200. **Chưa kiểm tra:** giao diện trên trình duyệt và luồng có đăng nhập (ghim, tim, báo cáo, admin xoá bình luận).

**Sửa sau đó (commit `84c2b08`, deploy client 2026-10-24 09:00):** Studio, mở lại video "Không công khai" / "Riêng tư" thì ô "Chế độ hiển thị" trống và nhãn đỏ. Nguyên nhân: Radix Select gọi `onValueChange("")` khi `form.reset()` đổi giá trị khỏi giá trị khởi tạo `ACTIVE`. Sửa: bỏ qua giá trị rỗng (`modules/studio/ui/sections/form-section.tsx`). **Anh đã thử trên trình duyệt: OK.**

**Sửa hộp thoại Chia sẻ (commit `2b5b805`, deploy client 2026-10-24 10:20):** ô "Bắt đầu tại" bị khoá khi thời điểm là 0:00 (mở trước khi phát, hoặc từ menu ⋮ của thẻ video) → giờ luôn tick được và sửa được giờ (nhập `m:ss`, `h:mm:ss` hoặc số giây). Thêm hàng biểu tượng tròn kiểu YouTube: Facebook, WhatsApp, X, Telegram, LinkedIn, Email, và "Khác" (bảng chia sẻ của điện thoại, gồm Zalo / Messenger) khi trình duyệt hỗ trợ; mũi tên cuộn chỉ hiện khi còn cuộn được; ô link + nút "Sao chép" xanh. Chưa có "Nhúng" (cần trang `/embed`, mục F2). Sau đó (commit `b64b242`, 10:26) sửa nội dung tràn khỏi hộp thoại: `DialogContent` là grid, cột tự rộng theo hàng biểu tượng → `grid-cols-[minmax(0,1fr)]`. **Chưa thử trên trình duyệt.**

### Gói 9: F1 + B4 + sửa đếm playlist (24/10)
- **Sửa đếm playlist** (commit `301c58d`, đã deploy backend 2026-10-24 10:01): danh sách playlist (`/api/playlists/{userId}`, `/created/{userId}`, `/{userId}/playlistType`, `/all/{userId}`) đếm số video và lấy ảnh bìa chỉ từ video người xem được thấy (`PlaylistServiceImpl.toVisibleDtos`). Đã kiểm tra: API trả 200, log không lỗi; chưa thử với playlist có video bị ẩn.
- **F1 Thẻ chia sẻ** (commit `de09110` + `2f932f1`, deploy backend 11:22, client 11:26): `app/watch/[videoId]/page.tsx` thành server component có `generateMetadata` (gọi `/detail` phía server, cache 5 phút): `<title>`, `og:title`, `og:description` (mô tả hoặc "Video của {kênh} trên WeTube"), `og:image` (thumbnail Cloudinary đổi sang JPG 1200x630, vì bản lưu là webp 300x200), `og:type=video.other`, canonical, thẻ Twitter. Phần giao diện chuyển sang `watch-page.tsx`. Root layout có `metadataBase` (`NEXT_PUBLIC_SITE_URL`, mặc định domain hiện tại). Backend: `/detail` trả thêm `thumbnailUrl`, `duration`. Video riêng tư / không tồn tại: chỉ có tiêu đề "WeTube".
- **B4 Link và mốc thời gian bấm được** (deploy cùng F1) trong mô tả video và bình luận (`modules/watch/components/linkified-text.tsx`): link http(s) mở tab mới (`nofollow ugc`), mốc `m:ss` / `h:mm:ss` tua trình phát, phát tiếp và cuộn lên trình phát (sự kiện `wetube:seek`, `lib/watchSeek.ts`).

**Đã kiểm tra:** biên dịch backend, `tsc`, eslint, `next build`; chạy bản build ở máy và đọc HTML trang xem: có `og:title`, `og:description`, `og:url`, canonical (chưa có `og:image` vì backend production chưa trả `thumbnailUrl`); regex nhận đúng `0:00`, `1:02:03`, link có dấu chấm / ngoặc ở cuối, bỏ qua `12:345`, `10:60`, `3:07pm`. Sau deploy: HTML trang xem trên production (giả làm bot Facebook) có đủ `og:title`, `og:description`, `og:image` 1200x630 (ảnh tải được, 200 `image/jpeg`), `og:url`, canonical, `twitter:card=summary_large_image`; video không tồn tại chỉ có tiêu đề "WeTube"; log backend không có Exception. **Anh đã thử trên trình duyệt và duyệt (24/10).**

**Sửa thông báo không hiện (commit `2098a78`, deploy client 2026-10-24 11:44):** Studio, upload, nút Lưu playlist, nút thích video và hộp thoại Chia sẻ gọi `react-hot-toast` nhưng chưa có `<Toaster>` nào được gắn, nên các thông báo đó không bao giờ hiện. Đã gắn `<Toaster position="top-center" />` ở `app/layout.tsx`; hộp thoại Chia sẻ chuyển sang `toastEmitter`.

**Sửa Đăng ký / số người đăng ký / số lượt thích (commit `adf49f4` client, `7820cbe` backend, deploy 2026-10-24 12:13–12:16):**
- Nút Đăng ký không đổi: backend trả chữ thường ("Subscribed successfully") nhưng client đọc như JSON → báo lỗi dù đã đăng ký xong; bấm lại thì backend ném `IllegalStateException` (500). Sửa: `responseHandler: "text"` (trang xem + trang kênh), backend đăng ký / huỷ gọi lại nhiều lần vẫn OK. Huỷ đăng ký trước đây luôn lỗi (chỉ nhận `tierId`, client không gửi) → giờ nhận `channelId` (gói mặc định của kênh).
- Dưới tên kênh hiện "N người đăng ký" (số gọn kiểu "1,2 N"), đổi ngay khi bấm; video của chính mình thì hiện "Chỉnh sửa video" thay cho Đăng ký; khách bấm Đăng ký thì báo cần đăng nhập.
- Số lượt thích đổi ngay khi bấm (optimistic) và tự cập nhật khi quay lại tab + mỗi 30 giây (`setupListeners` để `refetchOnFocus` chạy toàn app).
- Không làm thông báo cho chủ video khi có người thích video (YouTube cũng không có).
- **Anh đã thử trên trình duyệt: OK (24/10).**

### Gói 10: C2 + B7 (commit `559d8e4`, deploy backend 2026-10-24 11:53, client 11:56)
- **C2 Lịch sử tìm kiếm:** bảng `search_history` (`job/SearchHistoryTableInitializer.java`, không có entity JPA; `service/SearchHistoryService.java` dùng SQL thẳng). Mỗi người một dòng cho mỗi từ khoá (tìm lại thì đưa lên đầu), giữ 100 từ khoá gần nhất. API (cần đăng nhập): `GET /api/search-history?prefix=&limit=`, `POST /api/search-history {query}`, `DELETE /api/search-history?query=` hoặc `?all=true`. Khách: lưu trong trình duyệt (`localStorage`, 20 mục). Ô tìm kiếm (`lib/searchHistory.ts`, `search-input.tsx`): ô trống thì hiện 10 từ khoá gần nhất, đang gõ thì 3 từ khoá khớp đứng trước gợi ý (biểu tượng đồng hồ, chữ tím, nút "Xoá"); ghi lịch sử khi bấm tìm hoặc bấm gợi ý. Bỏ khung debug (`value=… open=… fetching=…`) đã lọt lên production.
- **B7 Lặp lại + menu chuột phải:** chuột phải trên trình phát mở menu "Lặp lại" / "Sao chép URL video" / "Sao chép URL video tại m:ss" (`video/context-menu.tsx`); chuột phải lần nữa thì ra menu của trình duyệt (như YouTube). "Lặp lại" cũng có trong menu Cài đặt (công tắc). Lặp dùng thuộc tính `loop` của thẻ video, nên hết video không hiện màn hình kết thúc / không tự sang video sau; sang video khác thì tắt lặp.

**Đã kiểm tra:** biên dịch backend, `tsc`, eslint, `next build`; sau deploy: log không có lỗi tạo bảng `search_history` / Exception, 3 API `/api/search-history` trả 401 khi chưa đăng nhập, các trang client trả 200. **Chưa kiểm tra:** lưu / xoá lịch sử khi đã đăng nhập (em không có tài khoản), giao diện trên trình duyệt.

### Gói 11: D1 quản lý lịch sử xem (commit `46591f7`, `7ecc951`, deploy 2026-10-24 backend 14:07, client 14:10 giờ VN)
- **API mới (cần đăng nhập):** `DELETE /api/playlists/me/history/{videoId}` (xoá 1 mục), `DELETE /api/playlists/me/history` (xoá hết), `GET` / `PUT /api/playlists/me/history/paused?paused=`. Xoá thì xoá luôn vị trí đang xem trong Redis (`wp:`).
- **Tạm dừng lưu:** cột `users.history_paused` (`AppUser.historyPaused`, null = không tạm dừng; runner `job/UserSchemaInitializer.java`, `@Order(0)`). Đang tạm dừng thì `WatchProgressServiceImpl.record` và `addVideoToHistory` không ghi gì.
- **Trang Lịch sử** (`history-list.tsx`, `SideBar.tsx`, zustand `store/zustand/useHistoryStore.ts`): ô tìm lọc theo tiêu đề / tên kênh; nút "Xóa tất cả nhật ký xem" (hỏi lại); nút "Tạm dừng / Bật lưu nhật ký xem"; nút "Xóa khỏi nhật ký xem" trước đây hỏng (gọi `/playlists/history/{id}`), giờ dùng API mới. DTO lịch sử có thêm `channelName` (trước hiện "Unknown"). Các nút "Quản lý toàn bộ nhật ký hoạt động", "Bình luận", "Bài đăng", "Trò chuyện trực tiếp" vẫn là nút chết, chưa động tới.
- **Sự cố deploy:** lần deploy backend đầu (13:52 giờ VN, job `cmvm1jvl…`) bị crash-loop, Vibe Host giữ bản cũ. Em cho runner chạy trước `AdminSeeder` (`@Order(0)`), deploy lại thì chạy. Nhưng log lần sau không có dòng "Đã thêm cột", tức cột đã có sẵn (có lẽ Hibernate thêm ở lần hỏng), nên **nguyên nhân thật của lần crash đầu chưa xác nhận** (log health check bị cắt mất dòng lỗi).

- **Sửa sau khi anh test (commit `ea17640`, `3ab8a69`, deploy backend 14:29, client 14:32 giờ VN):**
  - Phải xem hết video mới thấy trong lịch sử: trình phát chỉ gửi vị trí mỗi 15 giây hoặc khi dừng / rời trang, nên xem ngắn rồi mở Lịch sử thì trang tải trước khi kịp ghi. Giờ xem được 3 giây là gửi ngay một lần (`video/index.tsx`, `firstReportedFor`).
  - Lượt xem trong lịch sử luôn "0": client đọc `totalViews`, DTO là `totalView`.
  - Tên kênh trong lịch sử bấm được (DTO thêm `channelId`, `VideoItem` có `Link` tới `/channel/{id}`).
  - Sidebar: thêm "Kênh của bạn" (đầu mục "Bạn", `/channel/{user.channelId}`), bỏ "WeTube Studio", "Thêm từ WeTube" thành "Dịch vụ khác của WeTube".
  - "Hỗ trợ" trỏ nhầm `/otherservices/reportdiary` nên sáng cùng "Nhật ký báo cáo"; "Cài đặt" trỏ `/playlists/history`. Các trang hỗ trợ / phản hồi / cài đặt chưa có, anh chọn **ẩn "Cài đặt", "Hỗ trợ", "Gửi phản hồi"** cho tới khi có trang thật (commit `cd266c0`, deploy client 14:40).
- **Anh đã thử trên trình duyệt: OK (24/10).**

**Đã kiểm tra (gói 11):** `./gradlew compileJava compileTestJava`; test tạm trên H2 cho 2 truy vấn mới của `UserRepository` (đã xoá test); khởi động cả app trên H2 ở máy (context lên được, chỉ hỏng ở SQL riêng MySQL của `VideoSchemaInitializer`, không liên quan); `tsc` (chỉ 12 lỗi TS7006 cũ), eslint, `next build`. Sau deploy: log backend không có Exception, 4 API mới trả 401 khi chưa đăng nhập, `/`, `/playlists/history`, `/watch/…` trả 200. **Chưa kiểm tra:** các thao tác khi đã đăng nhập, giao diện trên trình duyệt.

### Gói 12: làm lại flow tải video lên, E3 (commit `563721b`…`5cf82cc`, deploy 2026-10-24 backend 15:24, client 15:27 giờ VN)
Thiết kế: `docs/superpowers/specs/2026-10-24-upload-flow-design.md`; kế hoạch: `docs/superpowers/plans/2026-10-24-upload-flow.md`.
- **Bản nháp:** trạng thái mới `DRAFT` (nối cuối ENUM, `VideoSchemaInitializer`); `POST /api/videos/drafts` tải lên Cloudinary, tạo bản nháp (tiêu đề = tên tệp), không vào Elasticsearch, không báo người đăng ký. Đăng = `PUT /api/videos/{id}` có sẵn.
- **Shorts:** ≤ 180 giây và rộng ≤ cao (`service/video/ShortsRule.java`, client `lib/shorts.ts`), ưu tiên số đo Cloudinary trả về. Trang Shorts chỉ lấy video có cờ `isShort` (bỏ điều kiện ≤ 60 giây cũ).
- **Client:** chọn tệp → icon Lottie chạy một lượt → hộp thoại 4 bước (`components/upload/`): Chi tiết (thẻ + "Gợi ý thẻ bằng AI", ảnh thu nhỏ, chọn nhiều danh sách phát / tạo mới), Các thành phần (phụ đề AI, `Select` shadcn), Kiểm tra, Chế độ hiển thị (mặc định Công khai). Tiến trình thật bằng axios (`lib/uploadDraft.ts`), sau 100% là "Đang xử lý…", lỗi có "Thử lại"; bấm Đăng trước khi tải xong thì tự đăng khi xong; đóng giữa chừng thì hỏi lại. Bỏ nút "AI Generate Description" và `video-details-modal.tsx`.
- **Studio:** nhãn "Bản nháp"; trang sửa video cho chọn chế độ hiển thị để đăng bản nháp.
- **Sửa sau khi anh test (commit `a7d384b`, deploy client 15:47):** modal bị vỡ khi tiêu đề dài (`DialogContent` là grid → thêm `grid-cols-[minmax(0,1fr)]`); các bước chỉ bấm được khi đã tới và khi bước Chi tiết không còn lỗi; ô ảnh thu nhỏ khung dọc 9:16 cho Shorts; khung xem trước có "Đường liên kết video" (`/watch/{id}`) + nút copy sau khi tải lên xong.
- **Sửa lần 2 (commit `1b52a6a`, deploy client 15:59):** phần ảnh thu nhỏ tràn khỏi modal và mất footer (hàng của grid chưa giới hạn → thêm `grid-rows-[minmax(0,1fr)]`); footer kiểu YouTube (icon tải lên / HD-SD / kiểm tra + "Đang tải lên 45% … Còn N phút" ước theo tốc độ gửi, `upload-status.tsx`); thanh tiến trình mảnh dưới tiêu đề, nhấp nháy khi đang xử lý, ẩn khi xong.
- **Sửa lần 3 (commit `0805c40`, `fca106c`, deploy backend 16:15, client 16:18):** trang Nội dung (Studio) chia 2 hàng cuộn ngang "Video" / "Shorts" (`content-row.tsx`, `content-card.tsx`; DTO playlist có thêm `isShort`), có mũi tên và "Xem tất cả"; bản nháp hiện khung hình Cloudinary (đổi `.mp4` → `.jpg`, `lib/videoMedia.ts`) tối lại thay vì ô trống; hộp thoại xoá kiểu YouTube (`delete-video-dialog.tsx`): thẻ video, ô xác nhận, "Tải video xuống" (`fl_attachment`), nút "Xoá vĩnh viễn" / "Xoá video nháp". Tải lên: chọn tệp xong chỉ làm mờ chữ (không hiện tên tệp), khung xem trước đứng yên khi cuộn (`sticky`), nút bo tròn. Ghi chú: xoá vẫn là xoá mềm (trạng thái DELETED) như trước.
- **Sửa lần 4 (commit `383e3c4`, deploy backend 19:35):** bản nháp dọc nằm ở hàng Video thay vì Shorts. Hộp thoại tính Shorts theo kích thước trình duyệt đo (đã xoay theo metadata) nhưng backend ưu tiên kích thước Cloudinary, nên hai bên có thể lệch. Giờ backend ưu tiên kích thước client gửi, thời lượng vẫn lấy của Cloudinary; log ghi cả hai (`Bản nháp …: client WxH, Cloudinary WxH`) để xác nhận nguyên nhân ở lần tải lên sau. Chưa xác nhận được bằng dữ liệu thật (không đọc được CSDL). Bản nháp đã tạo trước đó vẫn giữ cờ cũ.
- **Sửa lần 5 (commit `20a6a55` deploy backend 19:50; `92ee69d` deploy client 19:58):** log lần tải sau cho thấy `client 720x960, Cloudinary 720x960` → giả thuyết "xoay" ở lần 4 **sai**. Nguyên nhân thật: `PlaylistServiceImpl.getPlaylistDetailedById` tự dựng DTO bằng builder, không gán `isShort` → mọi video rơi vào hàng Video (test `PlaylistDetailTest`). Thêm: đóng hộp thoại sau khi lưu bản nháp giờ làm mới cả cache `Playlist` (trước đó Studio không thấy bản nháp mới cho tới khi tải lại trang).
- **Toast:** bỏ `react-toastify` và `react-hot-toast`, dùng toast của shadcn (Sonner, `components/ui/sonner.tsx`), một `<Toaster position="bottom-center" />` trong `app/layout.tsx` (màu đơn điệu, không đổi màu theo trạng thái; anh chọn 24/10, commit `03735c9`, deploy client 20:27); `lib/toastEmitter.ts` giữ nguyên các hàm, chạy trên Sonner.
- **Sửa lần 6 (commit `dbfba78`, deploy client 20:10):** toast bỏ nút đóng (chỉ tự đóng); thẻ Studio khi rê chuột: nền sáng lên + nút tròn (Chi tiết, Xem trên WeTube, ⋮: sao chép link / tải xuống / xoá) — hàng ngang trên ảnh với video, cột dọc bên phải với Shorts; bỏ nút thùng rác riêng. Mũi tên hàng cuộn tắt khi đã tới đầu / cuối (theo dõi `scroll` + `ResizeObserver`). Chưa có "Số liệu phân tích" / "Quảng bá" như YouTube vì WeTube chưa có trang đó.
- **Đề xuất còn mở (24/10 tối, anh duyệt):**
  - Xoá file trên Cloudinary: **đã có sẵn** từ trước (`deleteVideo` xoá file video + ảnh sau khi DB commit; log 20:21 "Xoá video thành công trên Cloudinary"). Dòng trong CSDL vẫn giữ trạng thái DELETED vì bình luận / báo cáo / lịch sử còn trỏ tới.
  - Sửa lỗi (commit `8ee867a`, deploy backend 20:38): đăng bản nháp thì ngày đăng = lúc đăng (trước là lúc tải lên). `isShort` không cần tính lại (tính từ file, file không đổi). Test `VideoPublishTest`.
  - Sửa lỗi (commit `2099442`, deploy client 20:42): thẻ nhận chữ có dấu (`#âmnhạc`), regex `\p{L}` ở `upload-wizard.tsx` và `form-section.tsx`. Backend vốn nhận sẵn.
  - Tính năng (commit `337d131`, anh duyệt, deploy backend 20:54): job `DraftCleanupJob` 03:00 giờ VN mỗi đêm xoá bản nháp không sửa quá 30 ngày (theo `updatedAt`), xoá cả file Cloudinary qua `deleteVideo`. Test `DraftCleanupJobTest`.
  - Tính năng (commit `7b45b6b`, anh duyệt, deploy backend 20:54, client 20:57): thẻ Studio hiện số bình luận (gồm trả lời) cạnh lượt xem; `commentCount` trong API chi tiết playlist theo id, đếm bằng một truy vấn `GROUP BY`. Test trong `PlaylistDetailTest`.
  - Số liệu phân tích (E1): để gói riêng, cần thiết kế; hiện chỉ lưu tổng lượt xem, chưa có dữ liệu theo ngày.
- **Phát hiện thêm:** nút AI cũ gọi `/api/ai/description` bằng axios không kèm cookie nên nhiều khả năng luôn 401 (chưa kiểm chứng); nút mới dùng `apiFetch`.

**Đã kiểm tra:** test backend `ShortsRuleTest` (3) và `VideoDraftTest` (3) qua, cả bộ test backend xanh; `tsc` (chỉ 12 lỗi TS7006 cũ), eslint các tệp đã sửa, `next build`. Sau deploy: log backend không có Exception, ENUM `videos_status` đã có `DRAFT`, `POST /api/videos/drafts` trả 401 khi chưa đăng nhập, `/api/videos/shorts` trả 200 (rỗng: video công khai duy nhất không đạt quy tắc Shorts mới), các trang `/`, `/studio`, `/shorts`, `/playlists/history` trả 200. **Chưa kiểm tra:** thử trên trình duyệt (tải lên thật, tiến trình, Shorts, danh sách phát, phụ đề AI).


- **Lỗi em tự phát hiện (commit `d1b11c5`, deploy client 21:45):** bấm kết quả tìm kiếm thì video vào ngay Nhật ký xem qua API thêm-vào-playlist chung, bỏ qua quy tắc 3 giây và "tạm dừng lưu nhật ký" của gói 11. Bỏ đoạn đó; trình phát vẫn tự ghi lịch sử đúng quy tắc.

### Gói 13: E1 Số liệu phân tích (commit `aa1b7ae`…`8073a1d`, anh duyệt, deploy backend 21:29, client 21:34 giờ VN)
Thiết kế: `docs/superpowers/specs/2026-10-24-studio-analytics-design.md`; kế hoạch: `docs/superpowers/plans/2026-10-24-studio-analytics.md`.
- **Trang `/studio/analytics`** (mục "Số liệu phân tích" ở thanh bên Studio): 4 ô số (Lượt xem, Thời gian xem (giờ), Người đăng ký mới, Bình luận), bấm ô nào thì biểu đồ đường theo ngày vẽ số đó; chọn 7 / 28 / 90 ngày (mặc định 28). Chart của shadcn (`components/ui/chart.tsx`, `recharts` 2.15.4). Lượt xem / thời gian xem có vùng xám + chú thích "Chưa ghi dữ liệu trước …".
- **Bảng `video_daily_stats`** (runner `VideoDailyStatsTableInitializer`, SQL thẳng qua `service/analytics/VideoStatsStore.java`, upsert cộng dồn, transaction riêng): lượt xem cộng trong `registerView`; thời gian xem cộng ở `/progress` (đã đăng nhập) và API mới công khai `POST /api/videos/{id}/watch-time?t=&anonId=` (khách, giới hạn 60 lần/phút/IP). Thời gian xem = phần vị trí tăng so với lần báo trước (Redis `wt:`), chỉ cộng khi 0 < delta ≤ 35 giây (đủ cho tốc độ 2x), tua thì lần đó 0.
- **API `GET /api/studio/analytics?days=7|28|90`** (`StudioAnalyticsService`): ngày theo giờ VN, đủ mọi ngày; người đăng ký mới từ `subscriptions.created_at` (không trừ huỷ đăng ký), bình luận + trả lời trên video chưa xoá từ `comments.created_at` (có số liệu quá khứ); lượt xem / thời gian xem chỉ có từ ngày deploy.
- **Em tự quyết:** chỉ số liệu cả kênh (chưa có biểu đồ riêng từng video); màu đường xanh `#065fd4`; lượt xem / bình luận của chính chủ kênh vẫn tính (giống số tổng hiện có).

**Đã kiểm tra:** test backend `ViewStatsTest` (3), `WatchTimeTrackerTest` (4), `StudioAnalyticsServiceTest` (6: khoảng ngày sai → 400, đủ ngày, chia ngày giờ VN, tổng, `trackingSince`, chưa có kênh → 0), cả bộ xanh (10 lớp); client `tsc` (chỉ 12 lỗi TS7006 cũ), eslint, `next build` (có route `/studio/analytics`). Sau deploy: log không có Exception / cảnh báo ghi số liệu; `GET /api/studio/analytics` 401 khi chưa đăng nhập; `POST /watch-time?t=5` 204 (em gọi thử với anonId `claude-check`, đã cộng 5 giây vào video `be49c4b5…`), `t=-1` 400, video không tồn tại 404; `/studio/analytics` 200. **Chưa kiểm tra:** giao diện trên trình duyệt, số liệu khi đã đăng nhập.
- **Anh đã thử trên trình duyệt: OK (24/10).**
- **Còn để ý:** khách có thể đổi `anonId` liên tục để thổi phồng thời gian xem (bị chặn ở 60 lần/phút/IP × 35 giây).
### Gói 14: C1 Bộ lọc tìm kiếm (commit `55b0056`, `f69b1b3`, anh duyệt, deploy backend 22:02, client 22:06 giờ VN)
- **Trang `/search`:** hàng nút Tất cả / Video / Shorts / Kênh; nút "Bộ lọc" mở bảng 3 cột: Thời lượng (dưới 4 phút / 4–20 / trên 20), Ngày tải lên (hôm nay / tuần này / tháng này / năm nay = 1 / 7 / 30 / 365 ngày gần nhất), Sắp xếp theo (mức độ liên quan / ngày tải lên / lượt xem). Bộ lọc nằm trên URL (`?query=&type=&duration=&uploaded=&sort=`), bấm lại lựa chọn đang chọn là bỏ; có "Xoá bộ lọc". "Khoảng N kết quả" giờ là tổng của Elasticsearch (trước là số dòng trên trang).
- **Backend:** `GET /api/videos/search/full` nhận thêm `type` (video | short), `duration`, `uploaded`, `sort` (`search/SearchFilters.java`, giá trị lạ → 400); câu truy vấn thêm `minimumShouldMatch("1")` để filter không làm should thành tuỳ chọn. Chỉ mục ES có thêm `isShort` (đồng bộ lại lúc khởi động). Mới: `GET /api/channels/search?q=` (tối đa 20 kênh, theo số người đăng ký, bỏ kênh bị ẩn / xoá).

- **Sửa sau khi anh test (deploy client 22:22 giờ VN):**
  - "Bộ lọc" mở hộp thoại riêng "Bộ lọc tìm kiếm" (Dialog shadcn), chọn xong thì đóng (commit `a8da83e`).
  - "Kênh không search ra": API tìm kênh chạy đúng (không dấu cũng khớp: "huynh" → "Kỳ Sơn Huỳnh", "Huỳnh Hoàng Thái"); không có kênh nào tên "view" — bước thử em đưa ("tìm view rồi bấm Kênh") sai, không phải lỗi.
  - **Nút "Tạm dừng lưu nhật ký xem" bị khoá — nguyên nhân thật:** access token sống 1 giờ; `apiFetch` không làm mới phiên, nên khi token hết hạn, lượt gọi `GET /me/history/paused` lúc mở trang bị 401 (chạy song song với lượt axios đang làm mới) → trạng thái `null` → nút khoá. Cùng lỗi làm vị trí đang xem không được lưu sau 1 giờ (chuỗi 401 mỗi ~15 giây trong log). Sửa: `apiFetch` gặp 401 thì làm mới phiên rồi gửi lại 1 lần; axios và `apiFetch` dùng chung một lượt làm mới (`refreshSession` trong `lib/apiClient.ts`) để refresh token chỉ xoay một lần (commit `39e39f6`).
  - **Lỗi em tự phát hiện cùng gốc:** 11 RTK Query slice (`fetchBaseQuery`) cũng không làm mới phiên → thích / bình luận / đăng ký… lỗi 401 sau 1 giờ. Giờ đi qua `apiFetch` (`fetchFn: apiFetch`, commit `42b01b3`).

**Đã kiểm tra:** test `SearchFiltersTest` (6), `ChannelSearchServiceTest` (2), cả bộ backend xanh (12 lớp); client `tsc` (12 lỗi cũ), eslint, `next build`. Sau deploy: ES đồng bộ không lỗi (trường `isShort` được nhận); curl từng bộ lọc trên production khớp với video công khai duy nhất ("view": ngang, ngắn, đăng trong năm): Tất cả / Video / dưới 4 phút / năm nay / sắp xếp ngày / lượt xem = 1, Shorts / trên 20 phút / hôm nay = 0, `type=movie` → 400; `/api/channels/search` trả kênh; các trang `/search?...` trả 200. **Chưa kiểm tra:** giao diện trên trình duyệt.

### Gói 15: tìm kiếm "rộng" như YouTube (mở rộng C1; commit `22b1cd6`, `90007ab`, anh duyệt, deploy backend 22:46, client 22:51 giờ VN)
Thiết kế: `docs/superpowers/specs/2026-10-24-search-relevance-design.md`.
- **Chỉ mục mới do backend tự quản** (`search/SearchIndexes.java`, cấu hình ở `resources/elastic/`): `videos_v2` và `channels`, tạo lúc khởi động nếu chưa có rồi đồng bộ từ MySQL. Mỗi tên (tiêu đề, tên kênh) được ghi theo 3 cách: bỏ dấu (`folded`), mẩu 3 chữ sau khi bỏ khoảng trắng (`compact`: viết dính, thiếu ký tự), tiền tố (`autocomplete`: gợi ý). Chỉ mục cũ `videos` để nguyên; không tạo được `videos_v2` thì quay về `videos`, không tạo được `channels` thì tìm kênh bằng MySQL.
- **Video** có thêm tên kênh + thẻ để tìm; câu truy vấn: khớp cả cụm (boost 6) → khớp từ chịu sai 1–2 ký tự (`title^3`, `channelName^2`, `tags.text^2`, `description`) → khớp mẩu 3 chữ ≥ 60% ở tiêu đề / tên kênh. Bộ lọc C1 giữ nguyên.
- **Kênh**: `GET /api/channels/search?q=&limit=&strict=`; tab "Tất cả" (khi chưa lọc) hiện tối đa 2 kênh khớp chặt ở đầu, tab "Kênh" tối đa 20 kênh khớp rộng. Dữ liệu hiển thị lấy từ MySQL. Kênh mới được ghi vào chỉ mục khi tạo (`initiateChannel`).
- **Code chết thấy được (chưa đụng):** `events/VideoSearchSyncListener` nghe các sự kiện video không ai phát, vẫn ghi vào chỉ mục cũ `videos`.

- **Anh đã thử trên trình duyệt: OK (24/10)** (cả gói 14 bộ lọc / hộp thoại Bộ lọc và gói 15).

**Đã kiểm tra:** test `SearchIndexesTest` (4: tạo chỉ mục với đủ analyzer, dùng lại chỉ mục có sẵn, lỗi thì quay về `videos`), `ChannelSearchServiceTest` (5: ES giữ thứ tự + lấy dữ liệu MySQL, ES lỗi → MySQL, ẩn kênh bị ẩn, limit), `VideoIndexerTest` (2), cả bộ backend xanh (14 lớp); client tsc / eslint / `next build`. Sau deploy: log "Chỉ mục tìm kiếm: video = videos_v2, kênh = channels", đồng bộ 1 video + 3 kênh, không lỗi. curl trên production: video "view" ra với `veiw` / `vieww` / `VIEW`, và với tên kênh `kysonhuynh` / `ky son huynh` / `son huynh`; `abcxyz` → 0; kênh: `kysonhuynh`, `hoangthai` (dính chữ), `huyng` (sai chính tả), `ky son huynh` (chặt) đều ra đúng kênh; bộ lọc C1 và gợi ý vẫn đúng; các trang `/search` trả 200. (Log không lọc ra được dòng "Đã tạo chỉ mục", nhưng ca dính chữ chỉ khớp được khi chỉ mục có cấu hình mới, nên cấu hình đã có hiệu lực.) **Chưa kiểm tra:** giao diện trên trình duyệt.

### Sửa loading toàn site + dọn code chết + phát tiếp video (24–25/10; commit `a3288cd`…`3a39fa6`; deploy 25/10 backend 08:37, client 08:40 giờ VN)
- **Dọn code chết** (`a3288cd`): xoá `events/VideoSearchSyncListener` + `events/videoEvent` (không ai phát sự kiện, vẫn ghi chỉ mục cũ `videos`), 2 import thừa trong `videoApi.ts`; sửa dòng đầu file này tự mâu thuẫn về `./gradlew`.
- **Thanh đỏ trên cùng — nguyên nhân thật:** `NavigationEvents` chỉ chạy *sau khi* URL đã đổi rồi giả 1 giây, nên trang chờ server (trang xem, kênh, Studio…) bấm xong không thấy gì; chuyển trang bằng `router.push` (Lịch sử, kết quả tìm kiếm, chuông, tự phát…) không được bắt. `finish()` cũ còn tắt nhầm thanh của lượt chuyển trang ngay sau. Giờ thanh chạy khi bấm link (bắt click ở `document`), khi `router.push` / `replace` (hook mới `hooks/use-progress-router.ts`, thay `useRouter` ở 19 file) và Back / Forward; xong khi URL mới hiện; tối đa 10 giây nếu chuyển trang bị huỷ.
- **Một kiểu loading:** nội dung trang dùng skeleton đúng hình nội dung (Lịch sử, chi tiết / Đã thích / Xem sau, Tất cả danh sách phát (trước không có loading), tab Danh sách phát + Bài đăng của kênh, Nhật ký báo cáo, admin báo cáo, Premium, trang xem video (thay vòng xoay xanh), Shorts, khung `Suspense` của /search); chỗ nhỏ (chuông, hộp thoại Lưu, phản hồi bình luận, bản ghi lời thoại, nút thanh toán, phát trực tiếp, `ProtectedRoute` của Studio / Lịch sử) dùng `components/ui/spinner.tsx`. Không còn chữ "Đang tải..." / "Loading..." / "Processing..." trên giao diện.
- **Lỗi cùng chỗ:** Đã thích / Xem sau nháy "Không có playlist" trước khi phiên đăng nhập tải xong; Shorts nháy "Chưa có Shorts nào" trước khi tải trang đầu. Lỗi em tự phát hiện (`45c7083`, commit riêng): chữ trang Shorts trống còn ghi "tối đa 60 giây" (quy tắc gói 12: dọc / vuông, ≤ 3 phút).
- **Thấy nhưng chưa đụng:** `videoCard.tsx` đưa khách sang `/login` nhưng không có trang `/login` (404); 2 file `VideoBubblePack` (`modules/subscription/`, `modules/subscribe/ui/components/video-bubble-pack.tsx`) không còn ai dùng; nút Premium còn chữ tiếng Anh ("Select …", "Duration: … days", "Try it for 1 month 0 VND").

- **Sửa lỗi anh báo 25/10: video ngắn không phát tiếp, luôn về 0** dù thanh đỏ ở đúng chỗ dừng. Nguyên nhân: `resumePosition` (`video/index.tsx`) chỉ phát tiếp khi `5 < vị trí < độ dài − max(10 giây, 5%)`; video "view" dài 12 giây → cần `5 < vị trí < 2`, không bao giờ đúng. Giờ hai ngưỡng tối đa 20% độ dài (video 12 giây: phát tiếp nếu vị trí trong 2,4–9,6 giây). Video từ 50 giây trở lên giữ nguyên quy tắc cũ (so bằng script: 0 trường hợp khác nhau từ 50 giây tới 2 giờ). Backend vốn trả vị trí thô, không cần sửa. Log backend không có lỗi `/progress`.

- **Sửa lỗi anh báo 25/10: video 7:30 hiện "0:11", thanh tiến trình sai, phát tiếp luôn về 0** (commit `f12b015`, backend). Nguyên nhân thật: link phát luôn là bản chuyển mã `q_auto,vc_h264` mà Cloudinary chỉ tạo khi có người mở lần đầu. Trong lúc tạo, Cloudinary trả `200` + `Transfer-Encoding: chunked`, bỏ qua `Range` (đã tái hiện bằng một bản `w_160` mới), nên trình duyệt chỉ thấy phần đã mã hoá (0:11 tăng dần), không tua được, vị trí 1 phút lớn hơn "thời lượng" nên phát từ 0. Video "Ông Putin…" (`72f08e7d…`): tạo lúc 00:14:10Z, bản chuyển mã `Last-Modified` 00:16:53Z; bây giờ file đó (ffprobe) dài đúng 450 giây và trả `206`. Sửa (anh chọn): tải lên MP4 H.264 8-bit thì phát thẳng file gốc (không còn nén `q_auto`, file của anh 90 MB thay vì 70 MB); định dạng khác vẫn chuyển mã nhưng xin Cloudinary tạo sẵn ngay sau khi tải lên (`explicit`, `eager_async`). Log mới khi tải lên: `Video <id>: <format> <video> -> phát file gốc | chuyển mã H.264`. Test `CloudinaryPlaybackTest` (5). Video đã có giữ link cũ (bản chuyển mã của chúng đã được tạo khi có người xem). **Chưa xác nhận:** Cloudinary có trả `video.codec` trong kết quả tải lên không (không có thì vẫn chuyển mã như trước): đọc dòng log trên ở lần tải lên đầu tiên sau deploy.

- **Sửa lỗi anh báo 25/10: xem tới phút 1, tua tới phút 3, thoát ra rồi vào lại thì phát tiếp ở phút 1** (client, `video/index.tsx`). Hai nguyên nhân, đều xác nhận bằng code: (1) tua không gửi vị trí: kéo thanh tua làm video tạm dừng nhưng lượt gửi lúc tạm dừng bị bỏ vì đang kéo; tua bằng phím / mốc thời gian không gửi gì, chỉ còn lượt gửi định kỳ 15 giây. (2) Rời trang trong app không gửi vị trí cuối: React đặt `ref` của thẻ `<video>` về `null` trước khi hàm dọn dẹp chạy (đã đọc trong `react-dom-client`: `case 5 → safelyDetachRef` ở pha xoá), nên `reportProgress(true)` lúc unmount luôn thoát sớm (lỗi có từ gói 6). Sửa: `onSeeked` gửi vị trí mới sau khi ngừng tua 1 giây (gom các lần tua liên tiếp, giới hạn `/progress` là 60 lượt/phút); `reportProgress` dùng thời điểm ghi lại ở `onTimeUpdate` khi thẻ video đã bị gỡ.

- **Anh đã thử trên trình duyệt: OK (25/10)** (loading toàn site, phát tiếp video ngắn / video dài, tua rồi thoát ra).
- **Sau deploy 08:37 / 08:40:** log backend không có Exception; `/detail`, tìm kiếm video / kênh trả 200; `POST /progress`, `POST /drafts` trả 401 khi chưa đăng nhập; 10 trang client trả 200, HTML /studio đã là bản mới (spinner, không còn "Đang tải...").
- **Sửa các lỗi em thấy (25/10, anh bảo sửa luôn):** `4b17e08` thẻ video: khách bấm "Lưu vào Xem sau" / "Lưu vào danh sách phát" bị đưa tới `/login` (không có trang này, 404) → toast "Hãy đăng nhập để lưu video" có nút "Đăng nhập" (luồng Google sẵn có). `504d903` chữ tiếng Anh ở Premium ("Chọn …", "Thời hạn: N ngày", "Dùng thử 1 tháng với giá 0 ₫") và trang Đã thích / Xem sau trống. `74f878a` xoá `VideoBubblePack` (`modules/subscription/`, `subscribe/ui/components/video-bubble-pack.tsx`), không còn ai dùng. `68d0554` trang danh sách phát: mọi video hiện "Unknown Channel" (builder DTO tự viết thiếu `channelName` / `channelId`, client đọc trường `channel` không tồn tại) và thời lượng số thô "450.049" → tên kênh thật + `m:ss` (test mới trong `PlaylistDetailTest`). **Không sửa:** khách đổi `anonId` để thổi phồng thời gian xem (bỏ `anonId` thì nhiều người chung IP bị gộp làm một; đã bị chặn 60 lượt/phút/IP).
- Job `DraftCleanupJob` đã chạy thật lần đầu: log 2026-10-24 20:00 UTC (03:00 giờ VN 25/10) "Dọn bản nháp: 0/0 đã xoá".

**Đã kiểm tra:** backend `compileJava compileTestJava`, cả bộ test (15 lớp); client `tsc` (chỉ 12 lỗi TS7006 cũ), eslint các file đã sửa (0 lỗi, 8 cảnh báo có từ trước), `next build`; HTML dựng sẵn của mọi trang tĩnh không còn chữ "Đang tải" (chỉ còn `aria-label` của spinner). **Chưa kiểm tra:** thanh đỏ và skeleton trên trình duyệt thật.

### Gói 16: B6 phát theo danh sách phát + giao diện danh sách phát (commit `a5dc292`…`10ee516`, merge vào `main`; anh duyệt, deploy 25/10 backend 09:56, client 10:00 giờ VN)
Thiết kế: `docs/superpowers/specs/2026-10-25-playlist-playback-design.md`; kế hoạch: `docs/superpowers/plans/2026-10-25-playlist-playback.md`.
- **Thẻ danh sách phát** (`modules/playlists/ui/components/playlist-tile.tsx`, dùng ở `/playlists/allplaylists` và tab Danh sách phát của kênh): vạch xếp chồng, "≡ N video", rê chuột "▶ Phát tất cả", "Riêng tư · Danh sách phát" / "{kênh} · Danh sách phát", "Xem toàn bộ danh sách"; ⋮ Xoá / Chỉnh sửa (tên + Công khai / Riêng tư) cho chủ danh sách tự tạo.
- **Trang danh sách phát** (`playlistdetail-list.tsx`, kể cả Đã thích / Xem sau): cột trái nền theo màu ảnh, tên (tiếng Việt cho danh sách hệ thống), chủ sở hữu, "N video · Cập nhật lần cuối vào …", Phát tất cả / Trộn bài; "Đã ẩn N video không xem được"; sắp xếp (Ngày thêm mới / cũ, Phổ biến nhất, Ngày xuất bản mới / cũ) + chip Tất cả / Video / Shorts; mỗi hàng có thanh đỏ đã xem, "kênh · lượt xem · x trước", ⋮ Lưu vào danh sách phát / Xoá khỏi {tên} / Chia sẻ.
- **Phát theo danh sách** (`/watch/{id}?list=…&shuffle=1`): khung danh sách ở đầu cột phải (`playlist-panel.tsx`): vị trí "3/70", Lặp lại danh sách, Trộn bài, thu gọn, hàng đang phát tô sáng; hết video thì thẻ "Tiếp theo" phát video sau trong danh sách kể cả khi tắt Tự phát; hết danh sách không lặp thì màn hình kết thúc như cũ; `Shift+N` / `Shift+P`. Thứ tự phát = thứ tự đang hiển thị ở trang danh sách (store `usePlaylistPlaybackStore`); mở link từ chỗ khác thì "Ngày thêm (mới nhất)".
- **Backend:** `GET /api/playlists/detail/{id}` thêm `ownerId`, `ownerName`, `privacy`, `hiddenCount`, `lastUpdatedAt`, `videos[].addedAt`, video mới thêm trước; danh sách tóm tắt thêm `ownerName`, ảnh bìa = video mới thêm nhất. Mới `PUT /api/playlists/{id}` (tên 1–150 ký tự, không trùng, `PUBLIC` | `PRIVATE`, chỉ danh sách tự tạo). Chặn xoá danh sách hệ thống (trước đây API cho xoá cả Lịch sử / Đã thích / Xem sau / Video đã tải lên) và chặn xoá video khỏi "Video đã tải lên" / Lịch sử (400).
- **Em quyết** (ghi trong spec): "Xoá khỏi danh sách" có ở danh sách tự tạo và Xem sau, không có ở Video đã thích / Video đã tải lên / Lịch sử; xoá danh sách ở trang chi tiết thì về `/playlists/allplaylists`; danh sách 1 video bật lặp thì phát lại chính nó.
- **Để lượt sau:** hàng chờ phát, Tải xuống, Chuyển lên đầu / xuống dưới, sắp xếp Thủ công, kéo thả (D2), mô tả danh sách.
- **Review cuối (subagent, `10ee516`):** 0 lỗi nghiêm trọng. Đã sửa 2 lỗi quan trọng: đếm ngược "Tiếp theo" ở chế độ danh sách chạy lại liên tục và gọi chuyển trang lặp lại (object video sau được tạo mới mỗi lần render); bật / tắt Trộn bài ở khung danh sách làm mất thứ tự đã chọn ở trang danh sách (giờ store giữ `baseOrder`, kiểm bằng script `node`). **Lỗi nhỏ chưa sửa:** link `?shuffle=1` dán / tải lại không đưa video đang xem lên đầu; bấm trong hộp thoại Lưu / Chia sẻ ở trang danh sách có thể đặt lại thứ tự trong store; trên điện thoại chạm ảnh thẻ là phát luôn thay vì mở trang; khung danh sách không cuộn lại hàng đang phát sau khi trộn; danh sách toàn video bị ẩn hiện "chưa có video nào" dưới "Đã ẩn N video".

**Đã kiểm tra:** test backend mới `PlaylistDetailTest` (thêm 2: thứ tự / chủ sở hữu / số video bị ẩn, danh sách rỗng), `PlaylistEditTest` (6: đổi tên + chế độ, chặn danh sách hệ thống, tên / chế độ sai, trùng tên, chặn xoá danh sách hệ thống, chặn xoá video khỏi Video đã tải lên / Lịch sử), cả bộ backend xanh; hàm thuần client `lib/playlistOrder.ts` kiểm bằng script `node` (sắp xếp 5 kiểu, chip, trộn, video sau / trước, danh sách rỗng, video không có trong danh sách, danh sách 1 video); `tsc` (chỉ 12 lỗi TS7006 cũ), eslint, `next build`; chạy bản build ở máy: `/playlists/{id}`, `/playlists/allplaylists`, `/playlists/liked`, tab danh sách của kênh, `/watch/{id}` và `/watch/{id}?list=khong-ton-tai` trả 200. **Chưa kiểm tra:** giao diện và luồng phát trên trình duyệt, khi đã đăng nhập.
- **Sau deploy:** log backend không có Exception; `PUT` / `DELETE /api/playlists/{id}` trả 401 khi chưa đăng nhập, chi tiết danh sách không tồn tại trả 404; các trang client (`/`, `/watch/{id}`, `/watch/{id}?list=…`, `/playlists/allplaylists`, `/playlists/liked`, `/playlists/watchlater`, `/playlists/{id}`, tab danh sách của kênh, `/search`, `/studio`) trả 200.

### Sửa lỗi anh báo 25/10 (sau gói 16; deploy backend 10:14, client 10:17 giờ VN)
- `c86430d` hộp thoại "Lưu video vào…": ô Riêng tư / Công khai là `<select>` gốc của trình duyệt (menu xanh kiểu Windows) → `Select` của shadcn như hộp thoại Chỉnh sửa danh sách phát.
- `54149ff` mục Shorts ở trang xem hiện mọi video gợi ý (có từ commit đầu tiên), kể cả video ngang 7:30 → `RecommendVideoDto` thêm `isShort` (trang xem, trang chủ, Elasticsearch), mục Shorts chỉ giữ Shorts; không có Shorts thì ẩn mục.
- `940cbcb` danh sách gợi ý bên dưới và "video tiếp theo" (tự phát, màn hình kết thúc) bỏ Shorts, vì Shorts đã có mục riêng.

**Đã kiểm tra:** backend `compileJava compileTestJava`; client `tsc` (chỉ 12 lỗi TS7006 cũ), eslint, `next build`. Sau deploy: log backend không có Exception; `GET /api/videos/{id}/detail` của cả 2 video công khai trả gợi ý kèm `"isShort":false`; các trang client (`/`, `/watch/{id}` của 2 video, `/shorts`, `/playlists/allplaylists`, `/playlists/watchlater`) trả 200. **Chưa kiểm tra:** giao diện trên trình duyệt; mục Shorts khi có Shorts thật (production chưa có video dọc nào, nên mục Shorts ở trang xem hiện đang ẩn).

---

## 2. ĐANG LÀM / DỞ DANG

### 2.1 Anh test khi đăng nhập (chưa ai kiểm tra)
Làm theo `TEST_CHECKLIST.md` mục **11–15**, dùng 2 tài khoản (A và B):
- Riêng tư / không công khai (cần cột `videos_status` ghi được giá trị mới: nên thử đầu tiên).
- Xoá video, không thích, trả lời/sửa bình luận, chia sẻ, lưu playlist, chip theo tag, đăng ký và huỷ đăng ký.
- Mục 1–10 của checklist (đăng nhập, báo cáo, phụ đề AI, phòng xem chung, MoMo) vẫn chưa test hết.

Nếu có lỗi: ghi giờ + màn hình, em tra log (`get_runtime_logs`).

### 2.2 Redis: phần còn lại
- Code đã xong (xem gói 3 ở trên). Cấu hình Redis trên Vibe Host đề xuất: 0,25 core (tối đa 0,5), RAM 64–128 MB, `maxmemory-policy allkeys-lru`, tắt lưu đĩa (chỉ dùng làm cache). 24/10: Redis là CSDL `redis-instance` do Vibe Host quản lý (đang chạy); công cụ không cho xem `maxmemory` / chính sách xoá key, anh xem trong giao diện nếu cần. Mọi key mình ghi đều có TTL (`RedisService` chỉ có hàm ghi kèm TTL), nên Redis không phình dù cấu hình thế nào.
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
| A6 | ✅ **Bình luận**: sắp xếp Nổi bật / Mới nhất, ghim, tim của chủ kênh, báo cáo bình luận | BE+FE | M | Gói 8 |
| A7 | ✅ **Báo lỗi khi bị giới hạn (429)** ở ô bình luận | FE | S | Gói 8 |
| A8 | **Gắn tag cho video hiện có** để chip trang chủ có dữ liệu | dữ liệu | S | 24/10: production chỉ có 1 video công khai ("view"), chưa có tag nào. Không cần code: anh gắn tag khi upload / sửa video trong Studio (hoặc nút AI viết mô tả và thẻ) |

### B. Trình phát
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| B1 | Chọn chất lượng (360p/720p/1080p, Tự động) | BE+FE | L | Cần Cloudinary tạo sẵn các bản hoặc phát HLS; video dài cần xử lý nền (eager/async) |
| B2 | Ảnh xem trước khi rê trên thanh tua | FE | M | Cloudinary cắt được khung hình theo giây (`so_`) |
| B3 | Chương (chapters) lấy từ mốc thời gian trong mô tả | FE | M | Thanh tua chia đoạn, tên chương |
| B4 | ✅ Mốc thời gian / link trong mô tả bấm được | FE | S | Gói 9, cả trong bình luận |
| B5 | Miniplayer khi rời trang xem (video vẫn chạy ở góc) | FE | L | Hiện chỉ có PiP của trình duyệt |
| B6 | ✅ Phát theo playlist (`?list=`): tự sang video sau, lặp / trộn | BE+FE | M | Gói 16 (kèm xoá video khỏi danh sách, thẻ + trang danh sách kiểu YouTube). Hàng chờ phát riêng chưa làm |
| B7 | ✅ Lặp video, chuột phải ra menu (sao chép link tại thời điểm…) | FE | S | |

### C. Khám phá & tìm kiếm
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| C1 | ✅ Bộ lọc tìm kiếm: thời lượng, ngày đăng, loại (video/Shorts/kênh), sắp xếp | BE+FE | M | Gói 14 |
| C2 | ✅ Lịch sử tìm kiếm trong ô gợi ý (xoá từng mục) | BE+FE | S | |
| C3 | Gợi ý dùng cả lịch sử xem, ẩn video đã xem hết, nút "Không quan tâm" | BE+FE | M | Hiện chỉ dựa vào điểm tag |
| C4 | Trang Khám phá (Âm nhạc, Thể thao, Tin tức, Trò chơi) dùng dữ liệu thật | BE+FE | M | Chưa xem kỹ: các trang này có banner, có thể còn dữ liệu mẫu |
| C5 | Cuộn vô hạn ở trang chủ / kênh / tìm kiếm | FE | S | Chưa xem kỹ |

### D. Lịch sử, playlist, Shorts
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| D1 | ✅ Lịch sử xem: xoá từng mục, xoá hết, tạm dừng ghi, tìm trong lịch sử | BE+FE | M | Gói 11 |
| D2 | Playlist: kéo thả sắp xếp, đặt ảnh bìa, sửa tên / mô tả | BE+FE | M | |
| D3 | Shorts: bình luận ngay trong Shorts, vuốt lên/xuống, nhớ âm lượng | FE | M | Hiện nút bình luận mở trang xem |

### E. Cho người làm video (Studio)
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| E1 | ✅ Thống kê: lượt xem theo ngày, thời gian xem, người đăng ký mới, bình luận | BE+FE | L | Gói 13; chưa có video nổi bật / biểu đồ từng video |
| E2 | Duyệt / sửa phụ đề AI trước khi đăng | BE+FE | M | PRD có yêu cầu |
| E3 | ✅ Thanh tiến trình upload + trạng thái "đang xử lý" | BE+FE | M | Gói 12 (làm cùng flow tải lên 4 bước, bản nháp, Shorts) |
| E4 | Đặt lịch công khai video | BE+FE | M | |
| E5 | Quản lý bình luận của kênh trong Studio | BE+FE | M | Chưa xem kỹ (đã có kiểm duyệt bình luận ở trang xem) |

### F. Nền tảng
| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| F1 | ✅ Thẻ chia sẻ og:title / og:image (gói 9); sitemap chưa làm | FE | S | Link dán vào Facebook/Zalo hiện ảnh + tiêu đề |
| F2 | Trang nhúng `/embed/{id}` + mã nhúng trong hộp thoại Chia sẻ | FE | M | |
| F3 | Giao diện tối | FE | M | Anh chọn để dành |
| F4 | Kiểm tra giao diện điện thoại toàn site | FE | M | Trình phát mới / drawer / chế độ rạp chưa thử trên máy thật |

### G. Dọn dẹp / kỹ thuật
- ~~Xoá project wetube-admin~~: 24/10 Vibe Host chỉ còn 2 project (wetube, wetube-service), đã xong.
- `JWT_PRIVATE_KEY` và `JWT_PUBLIC_KEY` đang **không** được đánh dấu secret trong Vibe Host. Khoá riêng nên đánh dấu secret.
- Xác nhận chữ ký MoMo với giao dịch sandbox thật (chưa từng thử).
- ~~Số đếm video trong playlist tính cả video đã bị ẩn~~: đã sửa 24/10 (số video và ảnh bìa ở danh sách playlist chỉ tính video người xem được thấy, `PlaylistServiceImpl.toVisibleDtos`).
- ~~Quyết định với Gradle wrapper 9.7.1 trong máy~~: 24/10 anh chọn trả về bản gốc (8.14.3), đã `git restore`.
- Test hết `TEST_CHECKLIST.md` mục 1–15 (mục 2.1).

---

## 4. GHI NHỚ KHI LÀM TIẾP

- **Deploy:** `git push` lên `main`, rồi `redeploy_project` (backend `cmvkfhypr...`, client `cmvkg1ilo...`), chờ job xong, đọc log. Chờ 60–90 giây nếu báo cooldown. Backend khởi động mất khoảng 30 giây.
- **Quyền:** bộ phân quyền tự động chặn `redeploy_project` cho tới khi anh nói rõ trong chat. Muốn khỏi phải nói: thêm `"mcp__vibehost__redeploy_project"` vào `permissions.allow` trong `.claude/settings.local.json`.
- **Schema DB:** `application-prod.yml` ghi `ddl-auto: update`, và Vibe Host có biến `SPRING_JPA_HIBERNATE_DDL_AUTO` ghi đè (không đọc được giá trị). 24/10: 3 cột của gói 8 đã có trước khi runner chạy, tức Hibernate tự thêm lúc khởi động, nên thực tế gần như chắc là `update` (không phải `none` như ghi trước đây). Hibernate `update` chỉ thêm bảng / cột, không đổi kiểu cột (vd. ENUM) và không thêm index, nên vẫn giữ nếp viết runner idempotent lúc khởi động (mẫu: `job/VideoSchemaInitializer.java`, `job/ReportTableInitializer.java`). Cột `videos_status` là ENUM, chỉ nối thêm giá trị vào cuối.
- **Backup:** nên backup MySQL trước mỗi lần thay đổi schema.
- **Cách nói chuyện:** tiếng Việt; nói rõ cái gì đã kiểm tra thật, cái gì chỉ đoán; sửa gộp rồi deploy một lần.
- **Gradle:** wrapper trong máy đã trả về 8.14.3 (24/10), giống production: biên dịch bằng `./gradlew --no-daemon compileJava compileTestJava -q`.
- **Kiểm tra client trước khi deploy:** `npx tsc --noEmit` (bỏ qua 12 lỗi TS7006 cũ), `npx eslint <file>`, `npx next build` (Vibe Host build với `ignoreBuildErrors: true`, nên lỗi kiểu không chặn build: phải tự chạy tsc). `node_modules` đã cài lại bằng `npm ci` ngày 23/10.
- **Không có trình duyệt** trong session: chỉ kiểm tra được bằng curl / log; giao diện phải nhờ anh thử và gửi ảnh.
- **Nếp làm việc đã thống nhất:** tính năng mới thì commit rồi hỏi anh trước khi push + deploy; sửa lỗi anh vừa báo trên tính năng đã deploy thì push + deploy luôn rồi báo. Mỗi xong một gói thì cập nhật file này.
- **Bẫy CSS đã gặp:** Tailwind v4 chỉ đọc `globals.css` (không đọc `tailwind.config.ts`); thanh cuộn nằm ở `html`; phần tử `absolute` phải nằm trong khung `relative`.
