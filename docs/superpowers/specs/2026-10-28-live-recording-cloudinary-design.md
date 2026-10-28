# Thiết kế: ghi hình trên trình duyệt người phát, lưu ở Cloudinary (L2b)

Ngày: 2026-10-28. Trạng thái: **chờ anh duyệt**. Thay cho cách lưu của L2 (`2026-10-28-live-recording-design.md`).

## 1. Vì sao đổi
- L2 dùng LiveKit ghi hình rồi đẩy vào kho kiểu S3. Mọi kho em tra đều vướng: R2 và Backblaze cần thẻ (R2 không có trần chi tiêu), MinIO đã dừng bản cộng đồng, tự chạy kho trên Vibe Host anh không muốn, Supabase free chặn 50 MB mỗi file và dừng dự án sau 1 tuần không hoạt động.
- LiveKit **không** đẩy được file ghi thẳng lên Cloudinary (chỉ S3 / Google Cloud / Azure / Alibaba).
- Anh chốt 28/10: dự án dùng cho cuộc thi, chỉ thử, nên dùng Cloudinary (đang dùng 5,76 / 25 credit; gói free không cần thẻ). Vì vậy **ghi ngay trên trình duyệt của người phát** rồi tải lên Cloudinary bằng luồng tải video sẵn có.

### Lợi so với L2
- Không cần thẻ, không cần kho mới, không có hoá đơn.
- **Không tốn hạn mức 60 phút mỗi tháng của LiveKit** (LiveKit không ghi).
- Bản ghi là video Cloudinary như mọi video khác: `uploadVideoWithMeta` tự xin Cloudinary chuyển mã sang MP4 H.264 khi tệp không phải MP4 H.264 (tệp quay trên trình duyệt thường là WebM), nên **tua được**; ảnh xem trước khi rê thanh tua và phụ đề AI chạy như video thường.

### Đánh đổi (anh cần biết)
- Bản ghi nằm trong máy người phát cho tới khi tải xong: **đóng tab hoặc mất mạng trước khi tải xong là mất**.
- Ghi trên trình duyệt chỉ ghi được **một nguồn hình cố định** (mục 2.2).
- Dùng chung 25 credit / tháng với toàn bộ WeTube. Trần theo kênh (mục 2.4) giữ cho phần ghi hình chỉ chiếm một phần nhỏ.

## 2. Phạm vi

### 2.1 Form chuẩn bị (giữ giao diện L2)
- Ô "Ghi hình để xem lại" (tắt sẵn) + Chủ đề bắt buộc + "còn X phút", chỉ với **Phát từ trình duyệt**.
- **Ẩn với kiểu Chỉ âm thanh** (bản đầu không ghi buổi chỉ có tiếng).
- OBS: giữ dòng nhắc dùng Record của OBS.

### 2.2 Ghi gì
- Hình: **nguồn chọn lúc bắt đầu**: kiểu Camera ghi camera; kiểu Màn hình và **Màn hình + camera ghi màn hình (không có khung camera nhỏ)**. Ghép camera vào góc phải vẽ lên canvas, mà trình duyệt gần như dừng vẽ khi tab bị ẩn (người phát thường đang xem màn hình được chia sẻ), nên bản đầu không ghép.
- Tiếng: micro, cộng tiếng tab / hệ thống nếu có (trộn bằng Web Audio).
- Bắt đầu ghi khi buổi lên sóng; dừng khi chủ kênh bấm Kết thúc, khi hết phần ghi của kênh, hoặc khi **nguồn hình đã chọn bị tắt** (dừng chia sẻ màn hình / tắt camera): khi đó phần đã ghi vẫn được giữ và tải lên lúc kết thúc, chủ kênh thấy "Ghi hình đã dừng vì nguồn hình đã tắt".
- Định dạng: MP4 H.264 nếu trình duyệt ghi được (Chrome / Edge bản mới), không thì WebM. Khoảng **0,8 Mb/giây hình + 64 kb/giây tiếng ≈ 6,5 MB mỗi phút**.
- **Mỗi bản ghi tối đa khoảng 95 MB** (≈ 14 phút): đường tải video hiện có đi qua backend, giới hạn 100 MB mỗi tệp, và Cloudinary nhận một lần tối đa 100 MB. Chạm mức này thì ngừng ghi như hết phần.

### 2.3 Kết thúc và tải lên
- Bấm **Kết thúc buổi phát**: buổi kết thúc ngay cho người xem như hiện nay, rồi bản ghi được tải lên ở chế độ nền, có thanh tiến độ.
- Thanh tiến độ nằm ở trang Studio (vẫn hiện sau khi buổi đã kết thúc và form chuẩn bị đã quay lại), kèm nhắc "Đừng đóng tab cho tới khi tải xong"; đóng tab thì trình duyệt hỏi lại.
- Tải lỗi: nút **Thử lại**, bản ghi còn trong bộ nhớ của tab.
- Xong: backend tải lên Cloudinary, tạo **video công khai của kênh** (tiêu đề, chủ đề, ảnh thu nhỏ của buổi, mô tả đầu "Đã phát trực tiếp ngày …", không báo người đăng ký lần nữa) như L2. Trang `/live/{id}` có nút **Xem lại**.
- Bản ghi dưới 30 giây: không đăng (như L2).
- Sau **6 giờ** kể từ lúc kết thúc mà không nhận được bản ghi: đánh dấu "Không nhận được bản ghi" (thay cho "chờ LiveKit trả file" của L2).

### 2.4 Hạn mức
- Giữ trần theo kênh và trần chung theo tháng của L2, nay để bảo vệ credit Cloudinary: mặc định **15 phút / kênh** và **55 phút toàn WeTube** (≈ 0,4 GB mỗi tháng), đổi bằng biến môi trường như L2.
- Trình duyệt biết số phút còn lại khi bắt đầu và tự dừng ghi khi hết. Backend tính số phút theo thời lượng Cloudinary trả về.

### 2.5 Bật / tắt
- Không cần cấu hình kho nữa. Đề xuất: **bật sẵn** (không cần biến môi trường), tắt được bằng `LIVE_RECORDING_ENABLED=false`.

### 2.6 Không làm
- Khung camera nhỏ trong bản ghi kiểu Màn hình + camera.
- Ghi buổi Chỉ âm thanh, ghi buổi OBS.
- Giữ bản ghi khi tab bị đóng (lưu tạm vào ổ máy rồi tải lại sau).
- Bản ghi dài hơn khoảng 14 phút (cần tải chia phần thẳng lên Cloudinary; làm sau nếu cần).

## 3. Cách làm

### 3.1 Backend
- **Gỡ phần LiveKit ghi hình của L2** (không còn dùng): `LiveKitGateway.startRecording / stopRecording / recording`, bean `EgressServiceClient`, các biến `LIVE_RECORDING_S3_*` / `PUBLIC_URL` / `S3_REGION`, đoạn job hỏi kết quả ghi. Lấy lại được từ git nếu sau này có kho.
- **Giữ** mô hình dữ liệu L2 (các cột `recording_*`, `topic_id`, `replay_video_id`), trần theo kênh / chung, `GET /api/live/recording`, `VideoService.createLiveReplay` (đổi đầu vào sang kết quả tải lên Cloudinary).
- Trạng thái: lên sóng → `RECORDING` (chỉ để hiển thị); kết thúc → `STOPPING` (chờ trình duyệt tải lên); tải xong → `DONE`; lỗi / quá 6 giờ → `FAILED`; dưới 30 giây → `SKIPPED` (xoá tệp trên Cloudinary).
- **API mới** `POST /api/live/{id}/replay` (multipart `file`): chỉ chủ kênh, buổi có bật ghi hình và chưa có bản ghi, tệp video ≤ 100 MB, giới hạn tần suất. Tải lên bằng `uploadVideoWithMeta`, rồi tạo video.
- Test: quyền, trạng thái sai (409), tệp không phải video, dưới 30 giây, cộng phút vào hạn mức, hết 6 giờ thì `FAILED`, không gửi thông báo.

### 3.2 Client
- `modules/live/lib/liveRecorder.ts`: lấy track hình đã chọn + trộn tiếng, `MediaRecorder` (ưu tiên MP4 H.264, không thì WebM), dừng khi hết phút / chạm 95 MB / nguồn hình tắt.
- Nơi giữ bản ghi và tiến độ tải lên dùng chung giữa bảng đang phát và form chuẩn bị (zustand), để thanh tiến độ còn hiện sau khi buổi kết thúc.
- Ẩn ô ghi hình với kiểu Chỉ âm thanh; nhãn "● Đang ghi hình" lấy theo trạng thái thật của bộ ghi trên máy.

### 3.3 Kiểm tra
- Backend: compile + cả bộ test.
- Client: `tsc`, `eslint`, `next build`; **Chrome thật với camera / màn hình giả**: ghi vài giây ra tệp, kiểm tra tệp có thời lượng và kích thước hợp lý, tải lên endpoint giả có tiến độ, nút Thử lại. Phần này em thử được thật (không cần LiveKit).
- **Chưa thử được, cần anh:** buổi phát thật kết thúc rồi tải lên Cloudinary thật, video xem lại phát và tua được, chuyển mã của Cloudinary với tệp WebM, tiêu tốn credit thực tế.

Cỡ ước tính: khoảng 1–1,5 ngày làm.

## 4. Điểm anh cần chốt
1. **Gỡ phần LiveKit ghi hình của L2** (để code gọn, lấy lại từ git được): đồng ý?
2. **Màn hình + camera chỉ ghi màn hình** (không có khung camera nhỏ trong bản ghi): đồng ý?
3. **Đổi nguồn giữa chừng thì ngừng ghi** (giữ phần đã ghi): đồng ý? (Muốn đi theo nguồn mới thì cần cách chỉ chạy trên Chrome / Edge, phức tạp hơn nhiều.)
4. **Bật sẵn** không cần biến môi trường, trần **15 phút / kênh, 55 phút chung** mỗi tháng: giữ hay đổi số?
