# WeTube → "YouTube của riêng mình": đã làm, còn dở, sẽ làm

Cập nhật: 2026-10-25. Không chứa mật khẩu hay khoá nào.

- Client: https://wetube.vibe.tinhgon.xyz (Vibe Host `cmvkg1ilo084w0i5fhrmxxz90`)
- Backend: https://wetube-service.vibe.tinhgon.xyz (Vibe Host `cmvkfhypr07ra0i5f5b51wed1`)
- Cách làm việc, kiểm tra trước khi commit, deploy, bẫy đã gặp: `CLAUDE.md`. Yêu cầu gốc: `PRD.md`, `BRD.md`. Việc tự test khi đăng nhập: `TEST_CHECKLIST.md`.
- File này chỉ tóm tắt. Chi tiết từng thay đổi nằm trong `git log` (commit ghi ở mỗi dòng); thiết kế các gói lớn nằm ở `docs/superpowers/specs/`.

---

## 1. ĐÃ LÀM (tất cả đã deploy)

✅ = anh đã thử trên trình duyệt: OK. Không có dấu = mới kiểm bằng build / test / curl / log.

### Trước 23/10
- Quyền: đọc công khai, ghi cần đăng nhập, danh tính lấy từ JWT, kiểm tra chủ sở hữu. Việt hoá giao diện, phụ đề AI (Gemini), Shorts, báo cáo video và trang duyệt của admin.

### 23/10
- **Gói 1, nền tảng** (`7f2a0b8`): lỗi trả 400 / 404 / 405 / 413 thay vì 500. Một quy tắc hiển thị video cho mọi nơi (`security/VideoAccess.java`): công khai; Không công khai (có link mới xem); Riêng tư và bị admin ẩn (chỉ chủ + admin); đã xoá (không ai). Elasticsearch chỉ giữ video công khai. Đếm lượt xem `POST /api/videos/{id}/view`: mỗi người 1 lượt / video / 30 phút, trang xem tính sau 5 giây, Shorts sau 3 giây.
- **Gói 2, giống YouTube** (`7f2a0b8`): chế độ hiển thị khi tải lên và trong Studio, xoá video (xoá mềm, xoá cả file Cloudinary), Không thích, Trả lời / Sửa bình luận, Chia sẻ (`?t=`), Lưu vào danh sách phát, chip trang chủ theo thẻ, gợi ý tìm kiếm theo tiền tố, đăng ký / huỷ ở trang kênh.
- **Gói 3, Redis** (`4a68f5d`): chống đếm trùng lượt xem, cache gợi ý trang chủ 5 phút, giới hạn tần suất (`security/RateLimiter.java`: AI mô tả 20 lần/giờ, phụ đề AI 5/giờ, lượt xem 120 / 10 phút / IP, bình luận 10/phút, báo cáo 20/giờ; vượt thì 429). Redis lỗi thì tự chạy không cần Redis.
- **Gói 4, giờ UTC + trình phát mới** (`fdb1ae9`, `435b5d7`): mọi thời gian trong JSON có dạng `…Z`. Trình phát tự viết trên thẻ `<video>` (`modules/watch/components/video/`): thanh tua, âm lượng, phụ đề, tốc độ, PiP, toàn màn hình, phím tắt kiểu YouTube.
- **Gói 5, giao diện trang xem** (`d9a7ec5`): thanh bên dạng drawer, chế độ rạp (phím T), menu cài đặt nền kính mờ.

### 24/10
- **Gói 6, A1–A5** (`e4a1e8d`, `c06b722`): nhớ vị trí đang xem (Redis `wp:` + MySQL) và thanh đỏ trên thumbnail ✅; ghi lịch sử khi thực sự xem; màn hình hết video + Tự phát; thông báo (chuông); bảng tin Đăng ký `/feed/subscribed`.
- **Gói 7, sửa sau khi anh test** (`d72a347`, `35fe326`, `88b24f4`): thẻ video kiểu YouTube (rê chuột xem trước, menu ⋮), bỏ cache chi tiết video, trang không còn bị thu hẹp khi mở menu.
- **Gói 8, bình luận A6 + A7** (`c876b45`): sắp xếp Hàng đầu / Mới nhất, ghim, tim của chủ kênh, chủ video gỡ bình luận, báo cáo bình luận (admin xoá được), toast khi lỗi (gồm 429). Sửa thêm: ô chế độ hiển thị trong Studio bị trống (`84c2b08`) ✅; hộp thoại Chia sẻ kiểu YouTube (`2b5b805`, `b64b242`).
- **Gói 9, F1 + B4** (`de09110`, `2f932f1`): thẻ chia sẻ `og:*` (trang xem là server component có `generateMetadata`); link và mốc thời gian bấm được trong mô tả / bình luận ✅. Sửa thêm: số video của danh sách phát chỉ tính video xem được (`301c58d`); nút Đăng ký, số người đăng ký, số lượt thích đổi ngay (`adf49f4`, `7820cbe`) ✅.
- **Gói 10, C2 + B7** (`559d8e4`): lịch sử tìm kiếm (bảng `search_history`; khách lưu trong trình duyệt); lặp video và menu chuột phải trên trình phát.
- **Gói 11, D1 lịch sử xem** (`46591f7`, `7ecc951`, `ea17640`, `3ab8a69`, `cd266c0`): xoá từng mục / xoá hết, tạm dừng lưu (`users.history_paused`), tìm trong lịch sử; thanh bên có "Kênh của bạn", ẩn Cài đặt / Hỗ trợ / Gửi phản hồi ✅.
- **Gói 12, flow tải lên E3** (`563721b`…`5cf82cc` và 6 lần sửa tới `92ee69d`): bản nháp (trạng thái `DRAFT`), hộp thoại 4 bước, tiến trình tải lên thật; Shorts = dài tối đa 180 giây và rộng ≤ cao (`ShortsRule`); Studio chia hàng Video / Shorts, hộp thoại xoá kiểu YouTube; job `DraftCleanupJob` lúc 03:00 giờ VN xoá bản nháp quá 30 ngày; toast chuyển sang Sonner.
- **Gói 13, E1 số liệu phân tích** (`aa1b7ae`…`8073a1d`): trang `/studio/analytics` (lượt xem, thời gian xem, người đăng ký mới, bình luận theo ngày; 7 / 28 / 90 ngày), bảng `video_daily_stats` ✅.
- **Gói 14, C1 bộ lọc tìm kiếm** (`55b0056`, `f69b1b3`, `a8da83e`): Tất cả / Video / Shorts / Kênh và hộp thoại Bộ lọc (thời lượng, ngày tải lên, sắp xếp). Sửa kèm: `apiFetch` và RTK Query tự làm mới phiên khi gặp 401 (`39e39f6`, `42b01b3`) ✅.
- **Gói 15, tìm kiếm rộng** (`22b1cd6`, `90007ab`): chỉ mục `videos_v2` và `channels` tìm được khi không dấu, viết dính, sai chính tả, gõ dở ✅.

### 25/10
- **Loading toàn site + phát tiếp video** (`a3288cd`…`3a39fa6`): thanh đỏ chuyển trang chạy cả khi chuyển bằng code (`hooks/use-progress-router.ts`); skeleton / spinner thống nhất; phát tiếp đúng với video ngắn và sau khi tua rồi thoát; Cloudinary: MP4 H.264 phát file gốc, định dạng khác xin chuyển mã sẵn (`f12b015`); trang danh sách phát hiện đúng tên kênh và thời lượng (`68d0554`) ✅. Khách bấm "Lưu vào Xem sau" có toast mời đăng nhập (`4b17e08`, chưa thử, xem 2.1).
- **Gói 16, B6 phát theo danh sách phát** (`a5dc292`…`10ee516`; thiết kế `2026-10-25-playlist-playback-design.md`): thẻ danh sách kiểu YouTube; trang danh sách (5 kiểu sắp xếp, chip Video / Shorts, ⋮ mỗi hàng); `/watch/{id}?list=…&shuffle=1` có khung danh sách, Lặp lại, Trộn bài, Shift+N / Shift+P; `PUT /api/playlists/{id}`, chặn sửa / xoá danh sách hệ thống. Sửa 5 lỗi nhỏ sau review (`ab29735`, `7df4692`, `47eb1e6`, `522443d`) ✅.
- **Mục Shorts ở trang xem** (`54149ff`, `940cbcb`): dữ liệu gợi ý có `isShort`; mục Shorts chỉ còn Shorts, danh sách gợi ý và tự phát bỏ Shorts ✅.
- **Lưu kiểu YouTube** (`c86430d`, `4fd3b78`, `65b9143`): nút icon Xem sau / Lưu / Chia sẻ; modal "Lưu vào..." (Xem sau + danh sách tự tạo, bật / tắt từng danh sách, tạo mới thì lưu luôn và đóng); icon Xem sau đọc đúng trạng thái; trang `/shorts` trống có khung giữa màn hình ✅.
- **Trang danh sách phát trống** (`5ddede0`): như YouTube, không có ảnh bìa; tên, "của {kênh}", "Không có video", nút Thêm video (về trang chủ) / Chỉnh sửa / Chia sẻ (sao chép link) / ⋮ Xoá. Anh quyết không cần avatar kênh ✅.
- **Gói 17, D3 trình phát Shorts kiểu YouTube** (`0d4e4e1`, anh duyệt, deploy client 25/10 11:46 giờ VN): góc trên có phát / dừng và âm lượng (rê chuột ra thanh kéo); góc dưới có kênh, Đăng ký, tiêu đề; thanh tiến trình đỏ tua được. Cột nút Thích / Không thích / Bình luận (có số) / Chia sẻ (hộp thoại có sẵn) / ⋮ (Lưu vào danh sách phát, Báo cáo); trên điện thoại nằm đè lên video. Bình luận mở cạnh video (máy tính) hoặc trượt từ dưới lên (điện thoại), dùng lại bộ bình luận của trang xem. Nút lên / xuống, phím ↑ / ↓, Space / K, M. Nhớ âm lượng / tắt tiếng (`useShortsVolume`, localStorage riêng của Shorts, mặc định tắt tiếng). URL đổi theo Short đang xem (`/shorts/{id}`, trước đây trang này chỉ có chữ "Trang Shorts"); mở link đó thì Short đó đứng đầu. Kiểm tra: `tsc` (12 lỗi cũ), eslint, `next build`; bản build ở máy: `/shorts`, `/shorts/{id}`, `/shorts/<id không tồn tại>` trả 200; sau deploy 3 trang này trên production trả 200. Chưa thử trên trình duyệt.
- **Sửa Shorts ở Studio và tìm kiếm** (anh báo 25/10; deploy backend 12:17, client 12:20 giờ VN): `6d63144` mọi ảnh thu nhỏ bị cắt thành 300x200 nên ảnh dọc của Short thành ảnh ngang → Shorts dùng 360x640 (`CloudinaryService.thumbnailUrl`, test trong `CloudinaryPlaybackTest`); runner `ShortsThumbnailInitializer` đổi link ảnh Short cũ (ảnh gốc vẫn còn trên Cloudinary, chạy trước đồng bộ Elasticsearch); `form-details` trả `isShort`, trang sửa video của Short có khung xem trước 9:16, ô ảnh thu nhỏ dọc, link `/shorts/{id}`. `b617714` tìm kiếm: `VideoDto.isShort`; Shorts gom thành hàng thẻ dọc "Shorts" sau 2 video đầu, bấm mở `/shorts/{id}`. Kiểm tra: cả bộ test backend xanh (16 lớp), `tsc` (12 lỗi cũ), eslint, `next build`; sau deploy: log không có Exception, tìm "Ambessa" trả `isShort: true` và ảnh `c_fill,h_640,w_360` (ảnh tải được), video "view" `isShort: false`. Lỗi do chính bản sửa này gây ra (anh báo kèm ảnh): khung xem trước 9:16 cao hơn nên cột phải trang Chi tiết bị cắt mất "Thời lượng" và ô "Chế độ hiển thị" (trang cao cố định, cột phải không cuộn) → `0d99cea` cho cột phải cuộn, deploy client 12:34. Chưa thử trên trình duyệt.
- **Gói 18, sửa video trong Studio như YouTube** (anh chọn 25/10; `0194133` backend, `d174519` client; deploy backend 16:32, client 16:35 giờ VN): bấm bản nháp trong Studio mở lại hộp thoại tải lên (`DraftEditDialog`, `UploadWizard` nhận `draft`), không tải lại tệp; bản nháp chưa có ảnh thu nhỏ thì lấy khung hình Cloudinary (`.jpg`) làm ảnh mặc định; nhãn "Đã lưu ở dạng bản nháp". Đóng hộp thoại (tải lên mới hoặc bản nháp) thì lưu nội dung đã điền vào bản nháp (`PUT /videos/{id}` với `status=DRAFT`: backend giữ DRAFT và giữ ngày, test `VideoPublishTest`) rồi toast "Đã lưu bản nháp trong Studio" (trước đây có toast nhưng không lưu gì); bước Chi tiết có lỗi thì hỏi lại trước khi đóng. Trang Chi tiết video đã đăng dùng lại `StepDetails` (thẻ + AI gợi ý thẻ, danh sách phát tick sẵn những danh sách đang chứa video, bỏ tick là gỡ), `StepElements` (phụ đề AI), `StepVisibility`; một khung cuộn, khung xem trước đứng yên; có "Huỷ thay đổi"; lưu xong ở lại trang. Tiêu đề giờ tối đa 100 ký tự (trang cũ 250). Link Short trong Studio / khung xem trước mở `/shorts/{id}`. Kiểm tra: cả bộ test backend xanh (16 lớp), `tsc` (12 lỗi cũ), eslint, `next build`; sau deploy: log backend không có Exception, `PUT /api/videos/{id}` 401 khi chưa đăng nhập, `/studio` và `/studio/videos/{id}` trả 200. **Anh đã thử trên trình duyệt: OK** (25/10), kèm 2 chỉnh sửa (deploy client 16:49 giờ VN): `5a72c75` ảnh thu nhỏ video ngang rộng cố định 16rem thay vì cả cột; `ac5e35a` bỏ nút "Gợi ý thẻ bằng AI" (anh thấy không cần; API `/ai/description` ở backend vẫn còn). 2 chỉnh sửa này anh đã thử: OK. **Còn mở:** anh báo bản nháp video dài trong Studio không bị làm tối như bản nháp Short; code của hai hàng giống nhau (`ContentCard`, `brightness-50`) và ảnh khung hình tải được, chưa tìm ra nguyên nhân, chờ ảnh chụp.

---

## 2. CÒN DỞ / CHƯA KIỂM CHỨNG

### 2.1 Cần anh thử
- `TEST_CHECKLIST.md` mục 1–15, dùng 2 tài khoản. Nên thử Riêng tư / Không công khai trước. Mục 1–10 (đăng nhập, báo cáo, phụ đề AI, phòng xem chung, MoMo) chưa thử hết.
- Gói 16: chưa xác nhận đã thử đủ danh sách gốc, nhất là đếm ngược 5 giây giảm đều rồi sang video sau (kể cả khi tắt Tự phát), Lặp lại ở video cuối, Shift+N / Shift+P, và người khác mở danh sách công khai của anh thì không thấy nút sửa / xoá.
- Đăng xuất, menu ⋮ của thẻ video → "Lưu vào Xem sau": phải có toast "Hãy đăng nhập để lưu video" kèm nút Đăng nhập.
- Gói 17 (trình phát Shorts) và mục Shorts ở trang xem: production đã có Short đầu tiên ("Xiaochaomeng và trick nâng cao Ambessa", `b0203ec5…`, dọc 720x960, 38 giây).

### 2.2 Em theo dõi
- ~~Cloudinary có trả `video.codec` không~~: đã xác nhận 25/10. Lần tải lên Short trên (11:13 giờ VN), log ghi `mp4 {codec=h264, …} -> phát file gốc`.
- Lần deploy backend lỗi đầu tiên của gói 11 (24/10 13:52) chưa rõ nguyên nhân, vì log bị cắt mất dòng lỗi.
- Khách đổi `anonId` liên tục có thể thổi phồng thời gian xem. Chấp nhận, vì đã chặn 60 lượt / phút / IP.

### 2.3 Redis
Code đã xong, mọi key đều có TTL. Cấu hình đề xuất trên Vibe Host: 0,25 core, 64–128 MB, `allkeys-lru`, tắt lưu đĩa. Sau này nếu cần: gộp lượt xem rồi ghi MySQL theo lô; lưu trạng thái phòng xem chung.

---

## 3. SẼ LÀM

Đã xong: A1–A7, B4, B6, B7, C1, C2, D1, D3, E1, E3, F1 (xem mục 1). Xong việc nào thì chuyển mã việc đó lên dòng này và bỏ khỏi bảng.
Cỡ: S = vài giờ, M = khoảng 1 ngày, L = vài ngày. "Chưa xem kỹ" = em chưa đọc code phần đó.

| # | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|
| A8 | Gắn thẻ cho video có sẵn để chip trang chủ có dữ liệu | dữ liệu | S | Không cần code: anh gắn thẻ trong Studio |
| B1 | Chọn chất lượng (360p / 720p / 1080p, Tự động) | BE+FE | L | Cloudinary tạo sẵn các bản hoặc phát HLS |
| B2 | Ảnh xem trước khi rê chuột trên thanh tua | FE | M | Cloudinary cắt được khung hình theo giây (`so_`) |
| B3 | Chương lấy từ mốc thời gian trong mô tả | FE | M | Thanh tua chia đoạn, hiện tên chương |
| B5 | Trình phát thu nhỏ ở góc khi rời trang xem | FE | L | Hiện chỉ có PiP của trình duyệt |
| B8 | Hàng chờ phát ("Thêm vào hàng chờ") | FE | M | Phần còn lại của B6 |
| C3 | Gợi ý dùng cả lịch sử xem, ẩn video đã xem hết, nút "Không quan tâm" | BE+FE | M | Hiện chỉ dựa vào điểm thẻ |
| C4 | Trang Khám phá (Âm nhạc, Thể thao, Tin tức, Trò chơi) dùng dữ liệu thật | BE+FE | M | Chưa xem kỹ, có thể còn dữ liệu mẫu |
| C5 | Cuộn vô hạn ở trang chủ / kênh / tìm kiếm | FE | S | Chưa xem kỹ |
| D2 | Danh sách phát: kéo thả, Chuyển lên đầu / xuống dưới, sắp xếp "Thủ công", ảnh bìa, mô tả | BE+FE | M | Nối tiếp gói 16 |
| D4 | Hộp thoại "Thêm video" vào danh sách (tìm kiếm, Xem sau, video của kênh) | BE+FE | M | Hiện nút này về trang chủ |
| E2 | Duyệt / sửa phụ đề AI trước khi đăng | BE+FE | M | PRD có yêu cầu |
| E4 | Đặt lịch công khai video | BE+FE | M | |
| E5 | Quản lý bình luận của kênh trong Studio | BE+FE | M | Chưa xem kỹ |
| E6 | Số liệu từng video, video nổi bật | BE+FE | M | Phần còn lại của E1 |
| F2 | Trang nhúng `/embed/{id}` + mã nhúng trong hộp thoại Chia sẻ | FE | M | |
| F3 | Giao diện tối | FE | M | Anh để dành |
| F4 | Kiểm tra giao diện điện thoại toàn site | FE | M | Trình phát, drawer, chế độ rạp chưa thử trên máy thật |
| F5 | `sitemap.xml` | FE | S | Phần còn lại của F1 |

**G. Kỹ thuật:**
- `JWT_PRIVATE_KEY` (và `JWT_PUBLIC_KEY`) chưa được đánh dấu secret trên Vibe Host.
- Chưa từng thử chữ ký MoMo với giao dịch sandbox thật.

---

## 4. GHI NHỚ RIÊNG (ngoài CLAUDE.md)
- Muốn khỏi phải nói "deploy đi": thêm `mcp__vibehost__redeploy_project` vào `permissions.allow` trong `.claude/settings.local.json`.
- Nên backup MySQL trước mỗi lần đổi schema.
- Client không có framework test: hàm thuần chạy bằng `node file.ts` (Node 24 tự bỏ kiểu). File có import `@/` cần một resolve hook đổi `@/x` thành `file:///E:/Wetube/WeTube-client-app-dev/src/x.ts`, nạp bằng `node --import <register.mjs>`.
- Heredoc / `sed` trong Bash dễ làm hỏng dấu `\` và `**`; sửa file có nhiều ký tự đặc biệt thì dùng Edit / Write. Nhiều file client dùng CRLF.
