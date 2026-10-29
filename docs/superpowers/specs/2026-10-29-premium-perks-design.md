# Thiết kế: Premium có quyền lợi thật (P1)

Ngày: 2026-10-29. Trạng thái: **anh duyệt 29/10, đã code xong 5 bước (xem YOUTUBE_PARITY.md mục 2.13)**. Anh chốt 28/10: làm cả 3 quyền lợi trang Premium đang hứa (không quảng cáo, tải xuống, phát nền); 29/10: viết theo đề xuất của em, không qua vòng hỏi.

## 1. Vì sao làm
- PRD 4.1.5 có "Gói Premium (tắt quảng cáo)", KPI 5–8% người dùng đăng ký Premium.
- Hiện có trang `/otherservices/premium`, gói `SubPack` (Basic 99.000 ₫ / 30 ngày…), thanh toán MoMo (G2 đã thử phần máy chủ 28/10) và bảng `premium_user`. Nhưng **không chỗ nào đọc trạng thái Premium**: người mua không thấy gì khác. Trang hứa "không quảng cáo, tải xuống, phát nền" mà site không có quảng cáo, không có nút tải, không phát nền.
- Rủi ro cho vòng chấm: giám khảo mua thử rồi không thấy khác biệt.

## 2. Mục tiêu / không làm / tuyệt đối tránh
**Mục tiêu:** người có Premium thấy rõ 3 quyền lợi ngay sau khi thanh toán xong; người chưa có thấy rõ Premium cho mình cái gì.

**Không làm:**
- Mạng quảng cáo bên thứ ba (PRD 4.2 ngoài phạm vi), theo dõi người xem để chọn quảng cáo, đo lượt hiển thị / bấm quảng cáo.
- Chống tải video thật (DRM, link có hạn): PRD 4.2 ngoài phạm vi. Link MP4 của video hiện đã nằm trong trang xem (`VideoDetailDto.videoUrl`), nên ai rành vẫn lấy được; nút tải là **tiện lợi cho Premium**, không phải khoá.
- Xem ngoại tuyến trong app (lưu video trong trình duyệt): "tải xuống" ở đây là tải tệp MP4 về máy.
- Trang quản trị quảng cáo: nội dung quảng cáo nằm cố định trong code.

**Tuyệt đối tránh:**
- Quảng cáo che hoặc chặn việc xem quá vài giây, hiện liên tục, có tiếng, hoặc tự mở trang khác.
- Làm trải nghiệm miễn phí tệ đi ở chỗ khác ngoài quảng cáo (ví dụ chủ động dừng video trên máy tính khi chuyển tab: hiện tại vẫn phát, giữ nguyên).
- Quảng cáo trong phòng xem chung, trang phát trực tiếp, Shorts (làm hỏng luồng chính của PRD).
- Hứa trên trang Premium điều không có thật.

## 3. Trạng thái Premium (nền cho cả 3 quyền lợi)
- **Định nghĩa:** người dùng là Premium khi có dòng `premium_user` `ACTIVE` với `end_date` ≥ hôm nay **theo giờ Việt Nam** (JVM chạy UTC; dùng `Asia/Ho_Chi_Minh` như ngày video xem lại của L2b).
- **Backend:** `PremiumService.premiumUntil(userId)` trả ngày hết hạn xa nhất còn hiệu lực (hoặc rỗng). Một nguồn duy nhất, mọi chỗ khác gọi hàm này.
- **`GET /api/me`** thêm `premiumUntil` (ngày, `null` nếu không có). Client giữ trong `useAuth()`; `isPremium = premiumUntil != null`.
- **Gia hạn cộng dồn (sửa kèm):** hiện mua khi còn hạn thì tạo dòng mới tính từ hôm nay, mất phần ngày còn lại. Đổi thành: ngày bắt đầu của gói mới = ngày sau ngày hết hạn xa nhất còn hiệu lực (không có thì hôm nay). Vẫn mỗi lần mua một dòng.
- **Sau khi thanh toán:** trang `/otherservices/payment-result` thành công thì gọi lại `/api/me` để quyền lợi có ngay, không cần đăng nhập lại.
- Admin không tự có Premium.

## 4. Quyền lợi 1: không quảng cáo
Site chưa có quảng cáo, nên thêm **quảng cáo nội bộ** (WeTube tự quảng bá tính năng của mình) cho người chưa có Premium, kể cả khách chưa đăng nhập. Premium không thấy bất kỳ quảng cáo nào.

### 4.1 Nội dung
- Một danh sách cố định trong client, khoảng 4–6 mẫu, mỗi mẫu: tiêu đề, một dòng mô tả, nút, link nội bộ, màu nền. Ví dụ: "WeTube Premium: tải video, phát nền, không quảng cáo" → `/otherservices/premium`; "Xem cùng bạn bè" → `/rooms`; "Phát trực tiếp ngay trên trình duyệt" → `/studio/live`; "Phụ đề AI cho video của bạn" → `/studio`.
- Vẽ bằng Tailwind (nền màu, chữ, icon lucide), không cần ảnh. Có nhãn **"Quảng cáo"** nhỏ như YouTube, và dòng "Không muốn thấy quảng cáo? Dùng Premium".
- Chọn mẫu ngẫu nhiên mỗi lần hiện.

### 4.2 Vị trí
1. **Trang xem, cột "Tiếp theo":** một thẻ quảng cáo ở đầu cột (trên điện thoại: dưới phần thông tin video, trước bình luận).
2. **Trang chủ:** một thẻ quảng cáo cùng cỡ thẻ video, ở vị trí thứ 4 của lưới (chỉ một thẻ, không lặp mỗi hàng). Không chèn khi đang lọc theo chip khác "Tất cả".
3. **Quảng cáo trước video (pre-roll), đề xuất của em:** trên trang xem, trước khi video bắt đầu phát, phủ một thẻ quảng cáo lên khung phát, nút **"Bỏ qua"** hiện sau 5 giây, tự đóng sau 10 giây. Không có tiếng, video chưa chạy trong lúc đó.
   - Giới hạn: tối đa **một lần mỗi 15 phút** mỗi trình duyệt (`localStorage`; đọc lỗi thì coi như chưa hiện).
   - Không hiện khi: tự phát tiếp video sau (danh sách phát hoặc Tự phát ở màn hình hết video), mở bằng link có `?t=`, phát tiếp từ vị trí đã xem dở.
   - Nếu anh thấy pre-roll phiền thì bỏ mục 3 này, 2 vị trí còn lại vẫn đủ cho "không quảng cáo".

### 4.3 Không có quảng cáo ở
Phòng xem chung, `/live` và trang buổi phát, Shorts, Studio, admin, trang Premium, trang tìm kiếm.

## 5. Quyền lợi 2: tải xuống
- **Nút "Tải xuống"** ở hàng nút dưới video trên trang xem (cạnh Chia sẻ / Lưu), hiện cho mọi người:
  - Premium: tải tệp MP4 ngay.
  - Đã đăng nhập, chưa Premium: hộp thoại "Tải xuống là quyền lợi Premium" + nút tới trang Premium.
  - Khách: mời đăng nhập (như "Lưu vào Xem sau" của khách).
- **Backend:** `GET /api/videos/{id}/download` (cần đăng nhập): kiểm tra Premium (403 kèm `message` tiếng Việt nếu không), kiểm tra người gọi xem được video (`VideoAccess`, như trang xem; video đã xoá 404), giới hạn **30 lần / giờ** mỗi người (`RateLimiter`, 429). Trả **302** tới link Cloudinary của video kèm `fl_attachment:<tên tệp>` (Cloudinary đặt header tải về; tên tệp lấy từ tiêu đề, bỏ dấu và ký tự lạ). Backend không tự chuyển dữ liệu video.
- Video đã được chuyển mã khi tải lên (MP4 H.264), nên tệp tải về mở được trên mọi máy.
- Shorts: không có nút ở trình phát Shorts trong bản này.
- **Chi phí:** mỗi lần tải tốn băng thông Cloudinary như một lần xem trọn video (gói free 25 credit / tháng dùng chung).

## 6. Quyền lợi 3: phát nền
Thực tế trình duyệt:
- **Máy tính:** chuyển tab hoặc thu nhỏ cửa sổ thì video vẫn phát, cho mọi người. Giữ nguyên.
- **Điện thoại:** tắt màn hình hoặc chuyển app thì trình duyệt dừng thẻ `<video>`. Thẻ `<audio>` thì được phát tiếp, và hiện điều khiển trên màn hình khoá nhờ Media Session API.

Thiết kế:
- Premium, trên trang xem, video đang phát và trang bị ẩn (`visibilitychange` → `hidden`): chuyển sang một thẻ `<audio>` phát bản MP3 của video tại đúng vị trí đang xem. Mở lại trang thì quay lại video tại vị trí của audio, tiếp tục phát.
  - Link MP3: backend thêm `audioUrl` vào chi tiết video, **chỉ khi người gọi là Premium** (dùng `CloudinaryService.audioUrlFor` sẵn có, Cloudinary tự tạo bản MP3 lần đầu được yêu cầu và giữ lại).
  - Media Session: tiêu đề video, tên kênh, ảnh thu nhỏ; nút phát / dừng, tua 10 giây; video kế tiếp nếu đang phát danh sách phát.
  - Hết audio thì xử lý như hết video (lưu vị trí xem, danh sách phát sang bài kế khi trang hiện lại).
- Lưu vị trí xem và tính thời gian xem vẫn chạy trong lúc phát nền (đọc vị trí từ thẻ audio).
- Chưa Premium: giữ nguyên hành vi hiện tại.
- Thêm mục **"Phát nền: Bật / Tắt"** trong menu cài đặt của trình phát cho Premium (mặc định Bật), để ai không muốn thì tắt.
- **Rủi ro:** iOS Safari và vài trình duyệt Android có thể không cho phát tiếp nếu chưa có thao tác chạm gần đây, hoặc dừng audio khi chuyển thẻ. Phải thử trên điện thoại thật (anh thử); trình duyệt nào không được thì ghi lại, không chặn bản phát hành.

## 7. Huy hiệu và trang Premium
- **Huy hiệu Premium** (icon nhỏ cạnh tên) ở bình luận và chat trực tiếp. Backend thêm cờ `premium` cho tác giả trong dữ liệu bình luận và tin chat (tra một lần cho cả trang bình luận, không tra từng người).
- **Menu tài khoản:** dòng "Premium · đến dd/mm/yyyy" (hoặc "Dùng Premium" cho người chưa có).
- **Trang Premium:**
  - Đang có Premium: hiện "Bạn đang dùng Premium đến …", nút đổi thành "Gia hạn ₫99.000 / 30 ngày".
  - Viết lại chữ cho đúng 3 quyền lợi thật: "Không quảng cáo", "Tải video về máy", "Phát nền trên điện thoại". Bỏ chữ nào hứa thêm ngoài 3 thứ này (ví dụ "ngoại tuyến", "nhạc").

## 8. Thứ tự làm
1. Trạng thái Premium + `/api/me` + gia hạn cộng dồn + làm mới sau thanh toán.
2. Không quảng cáo (2 vị trí thẻ, rồi pre-roll).
3. Tải xuống.
4. Phát nền.
5. Huy hiệu, menu tài khoản, chữ trên trang Premium.

Mỗi bước commit riêng. Deploy cả gói một lần khi xong (hỏi anh trước, theo CLAUDE.md).

## 9. Kiểm tra
- **Backend:** test cho định nghĩa Premium (còn hạn / hết hạn / đúng ngày hết hạn theo giờ VN / `INACTIVE`), gia hạn cộng dồn, `/download` (403 chưa Premium, 404 video xoá, không xem được thì không tải, 429, đúng link `fl_attachment`), `audioUrl` chỉ có khi Premium, cờ `premium` ở bình luận / chat.
- **Client:** `tsc`, `eslint`, `next build`; Chrome thật với API giả lập: thẻ quảng cáo đúng chỗ và không có ở trang bị loại trừ, pre-roll đúng giới hạn 15 phút và các trường hợp không hiện, Premium không thấy quảng cáo, nút tải ba trạng thái, chuyển video ↔ audio khi ẩn / hiện trang.
- **Anh thử (cần tài khoản và điện thoại thật):** mua Premium bằng MoMo sandbox (vòng còn lại của G2) rồi thấy quyền lợi có ngay; tải một video; phát nền trên Android Chrome và iPhone Safari khi tắt màn hình; huy hiệu ở bình luận.

## 10. Em tự quyết (anh xem khi đọc)
- Quảng cáo là quảng cáo nội bộ vẽ bằng Tailwind, không ảnh, không đo lượt.
- Pre-roll: 5 giây mới bỏ qua được, 10 giây tự đóng, 15 phút một lần.
- Tải xuống: 30 lần / giờ, không có ở Shorts.
- Phát nền chỉ khác biệt trên điện thoại; trên máy tính mọi người vẫn phát khi chuyển tab như hiện nay.
- Gia hạn cộng dồn và làm mới quyền lợi sau thanh toán làm chung trong P1.
- Admin không tự có Premium.

## 11. Cỡ việc
Cỡ L (khoảng 2–3 ngày). Phần dễ trượt nhất là phát nền trên điện thoại.
