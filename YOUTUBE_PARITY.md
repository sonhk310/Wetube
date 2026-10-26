# WeTube → "YouTube của riêng mình": đã làm, còn dở, sẽ làm

Cập nhật: 2026-10-26. Không chứa mật khẩu hay khoá nào.

- Client: https://wetube.vibe.tinhgon.xyz (Vibe Host `cmvkg1ilo084w0i5fhrmxxz90`)
- Backend: https://wetube-service.vibe.tinhgon.xyz (Vibe Host `cmvkfhypr07ra0i5f5b51wed1`)
- Trang quản trị Vibe Host: https://vibehost.matbao.ai (đăng nhập Cloudflare Access, tài khoản Mắt Bão)
- Cách làm việc, kiểm tra, deploy, bẫy đã gặp: `CLAUDE.md`. Yêu cầu gốc: `PRD.md`, `BRD.md`. Việc tự test khi đăng nhập: `TEST_CHECKLIST.md`.
- File này chỉ tóm tắt. Chi tiết nằm trong `git log` (commit ghi ở mỗi dòng); thiết kế các gói lớn ở `docs/superpowers/specs/`.

---

## 1. ĐÃ LÀM

✅ = anh đã thử trên trình duyệt: OK. Không có dấu = mới kiểm bằng build / test / curl / log.

### Trước 23/10
- Quyền: đọc công khai, ghi cần đăng nhập, danh tính lấy từ JWT, kiểm tra chủ sở hữu. Việt hoá, phụ đề AI (Gemini, có dịch), Shorts, báo cáo video và trang duyệt của admin.

### 23/10
- **Gói 1, nền tảng** (`7f2a0b8`): lỗi trả 400 / 404 / 405 / 413 thay vì 500; một quy tắc hiển thị video cho mọi nơi (`security/VideoAccess.java`); Elasticsearch chỉ giữ video công khai; đếm lượt xem mỗi người 1 lượt / video / 30 phút.
- **Gói 2, giống YouTube** (`7f2a0b8`): chế độ hiển thị, xoá mềm, Không thích, Trả lời / Sửa bình luận, Chia sẻ `?t=`, Lưu vào danh sách phát, chip theo thẻ, gợi ý tìm kiếm, đăng ký ở trang kênh.
- **Gói 3, Redis** (`4a68f5d`): chống đếm trùng, cache gợi ý 5 phút, giới hạn tần suất (`security/RateLimiter.java`, vượt thì 429).
- **Gói 4, giờ UTC + trình phát tự viết** (`fdb1ae9`, `435b5d7`): JSON giờ `…Z`; trình phát `modules/watch/components/video/` (tua, âm lượng, phụ đề, tốc độ, PiP, toàn màn hình, phím tắt).
- **Gói 5, giao diện trang xem** (`d9a7ec5`): thanh bên drawer, chế độ rạp (T), menu cài đặt kính mờ.

### 24/10
- **Gói 6, A1–A5** (`e4a1e8d`, `c06b722`): nhớ vị trí xem + thanh đỏ trên thumbnail ✅; lịch sử khi thực sự xem; màn hình hết video + Tự phát; chuông thông báo; `/feed/subscribed`.
- **Gói 7** (`d72a347`, `35fe326`, `88b24f4`): thẻ video kiểu YouTube (rê chuột xem trước, ⋮), bỏ cache chi tiết video.
- **Gói 8, bình luận A6 + A7** (`c876b45`): sắp xếp, ghim, tim của chủ kênh, gỡ / báo cáo bình luận; ô chế độ hiển thị Studio (`84c2b08`) ✅; hộp thoại Chia sẻ (`2b5b805`, `b64b242`).
- **Gói 9, F1 + B4** (`de09110`, `2f932f1`): thẻ `og:*`; link và mốc thời gian bấm được ✅; nút Đăng ký / số thích đổi ngay (`adf49f4`, `7820cbe`) ✅.
- **Gói 10, C2 + B7** (`559d8e4`): lịch sử tìm kiếm; lặp video, menu chuột phải.
- **Gói 11, D1 lịch sử xem** (`46591f7`…`cd266c0`): xoá mục / xoá hết, tạm dừng lưu, tìm trong lịch sử ✅.
- **Gói 12, E3 tải lên** (`563721b`…`92ee69d`): bản nháp `DRAFT`, hộp thoại 4 bước, tiến trình thật; Shorts ≤ 180 giây và dọc (`ShortsRule`); `DraftCleanupJob` xoá nháp quá 30 ngày.
- **Gói 13, E1 số liệu** (`aa1b7ae`…`8073a1d`): `/studio/analytics`, bảng `video_daily_stats` ✅.
- **Gói 14, C1 bộ lọc tìm kiếm** (`55b0056`, `f69b1b3`, `a8da83e`); tự làm mới phiên khi 401 (`39e39f6`, `42b01b3`) ✅.
- **Gói 15, tìm kiếm rộng** (`22b1cd6`, `90007ab`): không dấu, viết dính, sai chính tả, gõ dở ✅.

### 25/10
- **Loading toàn site + phát tiếp** (`a3288cd`…`3a39fa6`, `f12b015`, `68d0554`) ✅. Khách "Lưu vào Xem sau" có toast mời đăng nhập (`4b17e08`, chưa thử).
- **Gói 16, B6 phát theo danh sách phát** (`a5dc292`…`522443d`; thiết kế `2026-10-25-playlist-playback-design.md`): `/watch/{id}?list=…&shuffle=1`, Lặp lại, Trộn bài, Shift+N / P ✅ (còn vài mục chưa thử, xem 2.1).
- **Mục Shorts ở trang xem** (`54149ff`, `940cbcb`) ✅. **Lưu kiểu YouTube** (`c86430d`, `4fd3b78`, `65b9143`) ✅. **Danh sách phát trống** (`5ddede0`) ✅.
- **Gói 17, D3 trình phát Shorts** (`0d4e4e1`): nút bên phải, bình luận trong Shorts, ↑ / ↓, nhớ âm lượng, URL `/shorts/{id}`. Chưa thử.
- **Shorts ở Studio và tìm kiếm** (`6d63144`, `b617714`, `0d99cea`): ảnh Short 360x640, hàng "Shorts" trong kết quả tìm kiếm.
- **Gói 18, sửa video trong Studio** (`0194133`, `d174519`, `5a72c75`, `ac5e35a`, `174cb13`): bản nháp mở lại hộp thoại tải lên, đóng thì lưu nháp; trang Chi tiết dùng lại các bước tải lên; bản nháp phủ tối ✅.
- **Gói 19, bảng "Xem tất cả"** (`68550b8`, `0e18730`, `3255a87`): `/studio/content?tab=video|shorts`, lọc, sắp xếp, phân trang 30; chọn nhiều để đổi chế độ (`PATCH /api/videos/visibility`) hoặc xoá ✅ (bảng); đổi chế độ / xoá hàng loạt chưa thử.

- **Ô nhập khi tải lên + B3 chương** (deploy client v63 lúc 19:25 giờ VN, trễ vì Vibe Host lỗi build, xem 2.2):
  - `2242738` ô nhập: Tiêu đề một dòng; Mô tả cao cố định, không kéo giãn, dài thì cuộn trong ô.
  - `fa17adf` B3: đọc chương từ mô tả theo quy tắc YouTube (`modules/watch/lib/chapters.ts`: mốc đầu `0:00`, ≥ 3 mốc, tăng dần, mỗi chương ≥ 10 giây; số trong link không tính). Thanh tua chia đoạn, tên chương khi rê chuột, "• Tên chương ›" cạnh giờ, hàng "Chương" dưới mô tả (khung hình Cloudinary `so_`).
  - Kiểm tra: 15 phép thử bằng `node`, `tsc`, eslint, `next build`; sau deploy: bản build lấy `fa17adf`, các trang chính trả 200, JS trang xem có code chương, CSS có `field-sizing-fixed`, log không lỗi. Chưa thử trên trình duyệt.
  - Anh yêu cầu (25/10): `bb1930d` bỏ hàng "Chương" dưới mô tả; thay bằng khung "Chương" ở cột phải (trên khung danh sách phát, cùng kiểu): mặc định đóng, bấm "• Tên chương ›" trên trình phát để mở / đóng, có ✕; mỗi hàng ảnh + tên + giờ, tô chương đang xem, bấm để tua; sang video khác thì đóng (`components/chapters-panel.tsx`, `store/zustand/useChaptersPanelStore.ts`). Trên điện thoại nút tên chương vẫn ẩn nên không mở được khung. Deploy client 20:54 giờ VN; trang xem / Studio trả 200, log không lỗi. Chưa thử trên trình duyệt.
  - Anh yêu cầu thêm: `c910365` khung Chương trượt xuống + hiện dần (0,2 giây) khi mở; nút tên chương trên trình phát chỉ mở, đóng bằng ✕. Deploy client 21:02 giờ VN; JS / CSS trên production có animation, log không lỗi. Chưa thử trên trình duyệt.

---

## 2. CÒN DỞ / CHƯA KIỂM CHỨNG

### 2.1 Cần anh thử
- Ô nhập khi tải lên (tiêu đề một dòng, mô tả cố định chiều cao).
- Lịch sử xem của Shorts (commit chưa push, xem 2.3): lướt một Short quá 3 giây rồi mở `/playlists/history`.
- `TEST_CHECKLIST.md` mục 1–15 (2 tài khoản; anh để sau).

Anh đã thử trên trình duyệt: OK (26/10): B3 chương (khung Chương bên phải), đổi chế độ hiển thị / xoá hàng loạt (gói 19), trình phát Shorts (gói 17, trừ phần lịch sử ở 2.3), các mục còn lại của gói 16, toast "Hãy đăng nhập để lưu video".

### 2.3 Đang làm: lịch sử xem của Shorts
- Anh báo (26/10): trình phát Shorts chưa lưu lịch sử. Nguyên nhân: `shorts-video-card.tsx` chỉ gọi `useViewCounter`, không gọi `saveWatchProgress` như trình phát trang xem. Backend (`POST /api/videos/{id}/progress`) dùng chung nên không phải sửa.
- Sửa ở client: `modules/shorts/hooks/useShortsHistory.ts`. Short đang hiện gửi vị trí một lần sau 3 giây và một lần cuối khi vuốt sang Short khác / rời trang / ẩn tab (chưa đủ 3 giây thì không gửi, nên vuốt nhanh không tốn hạn mức 60 lượt / phút). Khách chưa đăng nhập chỉ gửi thời gian xem cho chủ kênh, giống trang xem.
- Hệ quả: lịch sử lưu giây đang xem, nên mở lại Short đó bằng `/watch/{id}` sẽ phát tiếp từ giây ấy. Trình phát Shorts luôn phát từ đầu.
- Trạng thái: `9190584` đã push và deploy client 09:22 giờ VN 26/10 (các trang trả 200, log không lỗi). Chưa thử trên trình duyệt.
- Anh yêu cầu thêm (26/10, sau khi đối chiếu với YouTube): trang Lịch sử xem có hàng Shorts riêng. `modules/playlists/ui/sections/history-shorts.tsx`: hàng thẻ dọc 9:16 cuộn ngang (nút ›), có tiêu đề "Shorts", ⋮ → "Xoá khỏi nhật ký xem", bấm thẻ mở `/shorts/{id}`; video thường nằm trong danh sách bên dưới; tab "Shorts" xếp thẻ nhiều hàng. Bỏ tab Podcast / Âm nhạc (backend không trả `type` nên chưa bao giờ hiện). Ô tìm kiếm ở cột phải bỏ khung và viền focus, chỉ còn gạch chân như YouTube (`SideBar.tsx`). Mới commit ở máy, chưa push, chưa thử trên trình duyệt.

### 2.2 Em theo dõi
- **Vibe Host lỗi build 25/10 18:36–19:25:** lượt deploy dừng ở bước Build, không có output Docker, mỗi 5 phút báo `fetch failed`; job `cmvnr4rse15oj0i5fwk24jx1t` hỏng sau 6/6 lần, job deploy lại `cmvnsfwo315u00i5f440xr7gk` thành công ở lần thử lại thứ 2 (v63). Lỗi phía nền tảng, không phải code. Gặp lại thì chờ thử lại / deploy lại, kéo dài thì báo hỗ trợ Mắt Bão.
- Lần deploy backend lỗi của gói 11 (24/10 13:52) chưa rõ nguyên nhân (log bị cắt).
- Khách đổi `anonId` liên tục có thể thổi phồng thời gian xem. Chấp nhận (đã chặn 60 lượt / phút / IP).
- Redis: code xong, mọi key có TTL. Cấu hình đề xuất: 0,25 core, 64–128 MB, `allkeys-lru`, tắt lưu đĩa.

---

## 3. LỘ TRÌNH (anh duyệt thứ tự 25/10)

Đã xong: A1–A7, B3, B4, B6, B7, C1, C2, D1, D3, E1, E3, F1. Xong việc nào thì chuyển mã lên dòng này và bỏ khỏi bảng.
Cỡ: S = vài giờ, M = khoảng 1 ngày, L = vài ngày. Việc cỡ L viết thiết kế cho anh duyệt trước khi code.

### Giai đoạn 1: hoàn thiện 4 luồng chính của PRD
| # | Mã | Việc | Ở đâu | Cỡ | Hiện trạng (em đọc code 25/10, chưa thử) |
|---|---|---|---|---|---|
| 1 | W1 | **Phòng xem chung làm lại** (PRD luồng 2): link mời `/rooms/{mã}`, ai cũng Phát / Dừng / Tua, danh tính từ tài khoản, dùng trình phát mới, thả cảm xúc, tự xoá phòng trống | BE+FE | L | Không có link mời (phải gõ mã); chỉ chủ phòng điều khiển; tên tự gõ; phòng chỉ trong bộ nhớ, không bao giờ xoá, mất khi deploy; nghi lỗi: thông báo "đã tạo phòng" gửi cho mọi người đang mở hộp thoại |
| 2 | L1 | **Livestream** (PRD luồng 3): trang "Đang phát trực tiếp", buổi phát gắn với kênh, thông báo cho người đăng ký, Poll của người phát, lưu lịch sử buổi phát, giao diện cùng kiểu site | BE+FE | L | Phát từ trình duyệt / OBS, chat, cảm xúc chạy được (API LiveKit trả 200); người xem phải tự gõ tên phòng; giao diện Radix Themes nền tối riêng |
| 3 | M1 | **Hội viên kênh trên client** (PRD luồng 4): nút "Tham gia Hội viên", chọn cấp, thanh toán, huy hiệu khi bình luận / chat | FE (+BE) | L | Backend có `/api/tiers`, client chưa dùng |
| 4 | E2 | Duyệt / sửa phụ đề AI trước khi đăng (PRD luồng 1 bước 4) | BE+FE | M | AI tạo + dịch được, chưa sửa được |
| 5 | G2 | Thử MoMo sandbox thật | BE | S | Chưa từng thử chữ ký |

### Giai đoạn 2: trình phát và trải nghiệm xem
| # | Mã | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|---|
| 6 | B2 | Ảnh xem trước khi rê chuột trên thanh tua | FE | M | Đề xuất: tối đa ~100 khung / video, ảnh Cloudinary `so_`, rộng 160px (lần đầu ~1,2 s, sau ~0,2 s). Chờ anh duyệt |
| 7 | B8 | Hàng chờ phát ("Thêm vào hàng chờ") | FE | M | Phần còn lại của B6 |
| 8 | C3 | Gợi ý dùng lịch sử xem, ẩn video đã xem hết, "Không quan tâm" | BE+FE | M | Hiện chỉ dựa vào điểm thẻ |
| 9 | C5 | Cuộn vô hạn ở trang chủ / kênh / tìm kiếm | FE | S | |
| 10 | B1 | Chọn chất lượng 360p / 720p / 1080p, Tự động | BE+FE | L | PRD nhắc HLS |
| 11 | B5 | Trình phát thu nhỏ ở góc khi rời trang xem | FE | L | Hiện chỉ có PiP |

### Giai đoạn 3: Studio và danh sách phát
| # | Mã | Việc | Ở đâu | Cỡ |
|---|---|---|---|---|
| 12 | D2 | Danh sách phát: kéo thả, Chuyển lên đầu / xuống dưới, "Thủ công", ảnh bìa, mô tả | BE+FE | M |
| 13 | D4 | Hộp thoại "Thêm video" vào danh sách (tìm kiếm, Xem sau, video của kênh) | BE+FE | M |
| 14 | E4 | Đặt lịch công khai video | BE+FE | M |
| 15 | E5 | Quản lý bình luận của kênh trong Studio | BE+FE | M |
| 16 | E6 | Số liệu từng video, video nổi bật | BE+FE | M |

### Giai đoạn 4: quản trị, chia sẻ, hoàn thiện
| # | Mã | Việc | Ở đâu | Cỡ | Ghi chú |
|---|---|---|---|---|---|
| 17 | Q1 | Admin: quản lý người dùng và doanh thu (PRD 4.1.6) | BE+FE | M | Hiện chỉ có `/admin/reports` |
| 18 | C4 | Trang Khám phá dùng dữ liệu thật | BE+FE | M | Chưa xem kỹ |
| 19 | F2 | Trang nhúng `/embed/{id}` + mã nhúng trong Chia sẻ | FE | M | |
| 20 | F5 | `sitemap.xml` | FE | S | |
| 21 | F4 | Kiểm tra giao diện điện thoại toàn site | FE | M | |
| 22 | F3 | Giao diện tối (PRD có yêu cầu) | FE | M | Anh để dành |

**Làm lúc nào cũng được:**
- G1: đánh dấu secret trên Vibe Host cho `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`, `LIVEKIT_API_KEY`.
- A8: anh gắn thẻ cho video có sẵn trong Studio (không cần code).
- Bài đăng cộng đồng + Poll đã có cả backend và client, chưa kiểm tra kỹ.

---

## 4. GHI NHỚ RIÊNG (ngoài CLAUDE.md)
- Muốn khỏi phải nói "deploy đi": thêm `mcp__vibehost__redeploy_project` vào `permissions.allow` trong `.claude/settings.local.json`.
- Nên backup MySQL trước mỗi lần đổi schema.
- Client không có framework test: hàm thuần chạy bằng `node file.ts` (Node 24 tự bỏ kiểu). File có import `@/` cần resolve hook đổi `@/x` thành `file:///E:/Wetube/WeTube-client-app-dev/src/x.ts`, nạp bằng `node --import <register.mjs>`.
- Heredoc / `sed` trong Bash dễ làm hỏng `\` và `**`; file nhiều ký tự đặc biệt thì dùng Edit / Write. Nhiều file client dùng CRLF.
- Vibe Host: lỗi deploy thì xem `get_build_logs` so với bản thành công trước; bản thành công có output Docker (`#16 … next build`). Không có lệnh xoá version / image qua MCP.
