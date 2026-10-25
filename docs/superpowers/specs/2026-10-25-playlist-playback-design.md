# Thiết kế: phát theo danh sách phát (B6) + giao diện danh sách phát kiểu YouTube

Ngày: 2026-10-25. Trạng thái: **chờ anh duyệt**.

## 1. Vì sao
Nút "Phát tất cả" / "Trộn bài" ở trang danh sách phát hiện không làm gì; bấm vào một video trong danh sách cũng không mở video. Anh muốn xem liền một danh sách như YouTube, bỏ được video khỏi danh sách, và thẻ / trang danh sách phát trông như YouTube (anh gửi 3 ảnh: thẻ danh sách, trang "Xem sau" với menu sắp xếp, menu ⋮ của một hàng).

**Thành công nghĩa là:** từ trang danh sách phát bấm "Phát tất cả" thì video đầu phát, hết thì tự sang video sau theo đúng thứ tự (hoặc thứ tự trộn), cột phải trang xem có khung danh sách với video đang phát được tô sáng; chủ danh sách xoá được video khỏi danh sách, đổi tên / chế độ, xoá danh sách; mở `/watch/{id}` không có `?list=` thì trang xem y như bây giờ.

## 2. Phạm vi
### 2.1 Thẻ danh sách phát (ảnh 1)
Một component dùng chung cho `/playlists/allplaylists` và tab "Danh sách phát" của trang kênh (thay `playlists/ui/list/playlist-card.tsx` và `channel/ui/playlists/playListsCard.tsx`).
- Ảnh bìa = ảnh video đầu tiên (thứ tự mặc định, mục 2.2), không có thì ảnh mặc định; phía trên ảnh có 1–2 vạch mỏng làm hiệu ứng thẻ xếp chồng; góc phải dưới nhãn "≡ N video".
- Bấm ảnh / tên: mở trang danh sách phát. Rê chuột lên ảnh: lớp mờ + chữ "▶ Phát tất cả" (bấm thì phát luôn, như YouTube).
- Dưới ảnh: tên (tối đa 2 dòng); dòng 2 "Riêng tư · Danh sách phát" nếu riêng tư, ngược lại "{tên kênh} · Danh sách phát"; dòng 3 "Xem toàn bộ danh sách".
- Menu ⋮ (chỉ chủ danh sách, không có ở danh sách hệ thống: Xem sau, Đã thích, Lịch sử, Video đã tải lên): **Xoá** (hộp thoại xác nhận) và **Chỉnh sửa** (hộp thoại: tên, chế độ Công khai / Riêng tư).

### 2.2 Trang danh sách phát `/playlists/{id}` (ảnh 2–3)
Dùng cho mọi danh sách, kể cả Đã thích và Xem sau (hai trang này đang dùng chung component).
- **Cột trái** (dính khi cuộn ở màn hình lớn): ảnh bìa; nền là màu chủ đạo của ảnh (đang có `colorthief`) chuyển dần sang tối; tên danh sách; tên chủ sở hữu; "N video · Cập nhật lần cuối vào {ngày}" (+ "Riêng tư" nếu riêng tư); menu ⋮ giống thẻ (chủ danh sách, không phải danh sách hệ thống); nút trắng **Phát tất cả**, nút xám **Trộn bài**. Bỏ 2 nút "Tải xuống" / "⋮" chết đang có nếu không có tác dụng.
- **Dòng báo** "Đã ẩn N video không xem được" (nút ✕ để tắt) khi danh sách có video người xem không được thấy (riêng tư của người khác, bị ẩn, đã xoá).
- **Sắp xếp** (menu thả xuống): Ngày thêm (mới nhất) — mặc định, Ngày thêm (cũ nhất), Phổ biến nhất (lượt xem), Ngày xuất bản (mới nhất), Ngày xuất bản (cũ nhất). **Chip** Tất cả / Video / Shorts. Cả hai làm ở client trên dữ liệu đã tải, không lưu.
- **Mỗi hàng:** số thứ tự; ảnh + thời lượng `m:ss` + thanh đỏ đã xem (`useWatchProgress`); tên (2 dòng); "{kênh} · {lượt xem gọn} · {x tháng trước}" (ngày xuất bản). Bấm hàng = phát từ video đó trong danh sách.
- **Menu ⋮ của hàng:** Lưu vào danh sách phát (hộp thoại sẵn có), **Xoá khỏi {tên danh sách}** (chỉ chủ danh sách; xoá ngay trên giao diện, toast "Đã xoá khỏi …"), Chia sẻ (hộp thoại sẵn có).
  - *Em quyết:* "Xoá khỏi" có ở danh sách tự tạo và **Xem sau**; **không** có ở "Video đã thích" (bỏ thích ở trang xem, vì xoá khỏi danh sách mà vẫn còn lượt thích thì lệch số), "Video đã tải lên" (Studio dùng danh sách này để liệt kê video của kênh) và Lịch sử (đã có nút xoá riêng).
- *Em quyết:* tên danh sách hệ thống hiện bằng tiếng Việt: Xem sau, Video đã thích.
- Thứ tự phát = thứ tự đang hiển thị (sau sắp xếp + chip).

### 2.3 Phát theo danh sách (B6)
- **Link:** `/watch/{videoId}?list={playlistId}`; trộn bài thêm `&shuffle=1`. Có `?t=` thì vẫn ưu tiên `?t=` như hiện nay.
- **Thứ tự:** lấy từ trang danh sách lúc bấm (sắp xếp + chip) và giữ trong bộ nhớ (zustand) theo `playlistId`; mở link `?list=` từ chỗ khác (dán link, tải lại trang) thì dùng thứ tự mặc định "Ngày thêm (mới nhất)". Trộn bài: video được bấm (hoặc video ngẫu nhiên khi bấm "Trộn bài") lên đầu, còn lại xáo ngẫu nhiên một lần, giữ nguyên tới khi tắt trộn hoặc rời danh sách.
- **Khung danh sách** ở đầu cột phải trang xem (trên thẻ / Shorts / video gợi ý; chế độ rạp vẫn ở cột phải dưới trình phát như video gợi ý hiện nay): tên danh sách (link về trang danh sách), "{chủ sở hữu} · {vị trí}/{tổng}", nút **Lặp lại danh sách** và **Trộn bài** (bật/tắt, có màu khi bật), nút thu gọn khung; danh sách cuộn được (cao tối đa ~ 400px), video đang phát tô sáng và tự cuộn vào giữa; mỗi hàng: số thứ tự (hoặc ▶ ở hàng đang phát), ảnh nhỏ + thời lượng, tên, kênh; menu ⋮ của hàng có "Xoá khỏi {tên}" cho chủ danh sách.
- **Hết video:** sang video kế tiếp trong danh sách bằng thẻ đếm ngược "Tiếp theo" sẵn có (Huỷ / Phát ngay), **không phụ thuộc công tắc Tự phát** (như YouTube). Hết danh sách: nếu bật Lặp lại thì về video đầu, không thì hiện màn hình kết thúc bình thường (gợi ý). Đang bật "Lặp lại" của chính video (B7) thì video đó lặp như cũ.
- Video hiện tại không có trong danh sách (vd. đã bị xoá khỏi danh sách) thì khung vẫn hiện, không tô sáng hàng nào, hết video thì phát video đầu danh sách.
- Phím tắt YouTube `Shift+N` (video sau) / `Shift+P` (video trước) khi đang trong danh sách.

### 2.4 Backend
- `GET /api/playlists/detail/{id}` (đã có, `ownershipGuard.playlistReadable`) trả thêm: `ownerId`, `ownerName` (tên kênh của chủ), `privacy`, `hiddenCount` (số video trong danh sách mà người xem không được thấy), `lastUpdatedAt` (lần thêm video gần nhất, không có thì `createdAt` của danh sách); mỗi video thêm `addedAt` (= `PlaylistVideo.createdAt`). Danh sách video trả theo `addedAt` mới nhất trước.
- `PUT /api/playlists/{id}` (mới): body `{ title, privacy }` (`privacy` là `PUBLIC` | `PRIVATE`), chỉ chủ danh sách, không cho danh sách hệ thống (400). Tên rỗng / quá 150 ký tự / trùng tên danh sách khác của cùng người → 400 với `message` tiếng Việt.
- `DELETE /api/playlists/{id}` (đã có): thêm chặn danh sách hệ thống (400); hiện API này cho xoá cả Lịch sử / Đã thích / Xem sau / Video đã tải lên (giao diện chưa gọi tới nên chưa thành lỗi). `DELETE /api/playlists/{id}/{videoId}` (đã có, chỉ chủ): thêm chặn `USER_UPLOADED` và `HISTORY` (400) cho khớp với giao diện.
- Danh sách hệ thống = `HISTORY`, `LIKED`, `WATCH_LATER`, `USER_UPLOADED` (và các loại không do người dùng tạo); chỉ `USER_PLAYLIST` sửa / xoá được.

## 3. Không làm (để lượt sau)
Hàng chờ phát ("Thêm vào danh sách chờ"), Tải xuống, Chuyển lên đầu / xuống dưới cùng, sắp xếp "Thủ công", kéo thả sắp xếp (D2), mô tả danh sách, chọn ảnh bìa, lượt xem của danh sách, lưu danh sách của người khác vào thư viện, "Hoàn tác" sau khi xoá.

## 4. Không được
- Trang xem không có `?list=` thay đổi hành vi (tự phát, màn hình kết thúc, lịch sử, vị trí đang xem giữ nguyên).
- Lộ video riêng tư / bị ẩn của người khác qua danh sách (chỉ hiện số đếm "Đã ẩn N video").
- Người khác (không phải chủ) thấy nút Xoá / Chỉnh sửa / Xoá khỏi danh sách.

## 5. Ràng buộc
- Client: Next.js 15 App Router, Tailwind v4 (chỉ `globals.css`), shadcn (Dropdown, Dialog, Select), toast qua `lib/toastEmitter` / `sonner`, gọi API qua `apiFetch` / `playlistService`. Thanh tiến trình đỏ dùng `useWatchProgress`. Điều hướng dùng `Link` hoặc `useProgressRouter` (để thanh loading chạy).
- Backend: Spring Boot, không đổi schema (chỉ đọc `playlist_videos.created_at` sẵn có). Test backend cho DTO mới, `PUT`, chặn danh sách hệ thống.
- Theo nếp dự án: tính năng mới thì commit ở máy, hỏi anh trước khi push / deploy; deploy backend trước.

## 6. Để người làm tự quyết
Cấu trúc component / file, tên store, cách xáo (Fisher–Yates), bố cục chi tiết trên điện thoại (cột trái thành phần đầu trang), màu nền khi ảnh không lấy được màu (xám như hiện nay), số video hiện trước khi cuộn trong khung danh sách.
