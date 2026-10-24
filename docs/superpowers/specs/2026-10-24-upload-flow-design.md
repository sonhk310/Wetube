# Thiết kế: làm lại flow tải video lên (E3 + 4 bước kiểu YouTube)

Ngày: 2026-10-24. Trạng thái: anh đã duyệt.

## 1. Vì sao làm

Hộp thoại tải lên hiện tại là một trang dài, thanh tiến trình là giả (tự cộng 5% mỗi 0,2 giây tới 90%), không cho thấy lúc "đang xử lý", phân loại Shorts sai, dropdown là `<select>` mặc định của trình duyệt. Anh muốn một flow giống YouTube: chọn tệp là bắt đầu tải lên ngay, trong lúc đó điền 4 bước, tới cuối thì đăng.

**Thành công nghĩa là:** chọn tệp xong thấy tiến trình thật (%) và trạng thái "Đang xử lý"; điền xong 4 bước là đăng được ngay khi tệp đã lên xong; video dọc/vuông ≤ 3 phút tự thành Shorts; video vào đúng danh sách phát đã chọn.

## 2. Phạm vi

### Có làm (anh đã yêu cầu / đã duyệt)
1. **E3, tiến trình thật:** % theo số byte đã gửi; sau 100% hiện "Đang xử lý…" tới khi server trả về.
2. **Tải lên ngay khi chọn tệp** (anh duyệt cách 1): tệp lên thành **bản nháp**, chi tiết lưu sau.
3. **4 bước:** Chi tiết → Các thành phần của video → Kiểm tra → Chế độ hiển thị, có thanh bước như ảnh YouTube anh gửi (chấm tròn nối bằng đường kẻ, bước hiện tại tô màu, bước có lỗi có dấu `!`).
4. **Dropdown kiểu WeTube:** thay mọi `<select>` trong flow bằng `Select` của shadcn (Radix) như trang sửa video trong Studio.
5. **Animation icon:** khi đã có tệp (chọn hoặc thả), icon Lottie upload chạy một lượt rồi mới chuyển sang 4 bước.
6. **Bỏ nút "AI Generate Description"**; **giữ ô thẻ** và thêm nút **"Gợi ý thẻ bằng AI"**.
7. **Phân biệt Shorts:** Shorts = thời lượng ≤ 180 giây **và** tỉ lệ vuông hoặc dọc (rộng ≤ cao). Ngang 16:9 luôn là video thường.
8. **Chọn danh sách phát** khi đăng (chọn nhiều, có thể tạo danh sách mới).

### Em đề xuất thêm, anh đã đồng ý hướng chung
9. Kiểm tra kích thước tệp ở client trước khi tải (tối đa 100MB, giới hạn của backend).
10. Hỏi lại trước khi đóng hộp thoại khi đang tải lên.
11. Nhãn "Video này sẽ là Shorts" kèm lý do, xem trước ở khung dọc nếu là Shorts.
12. Trang Shorts chỉ lấy video có cờ `isShort` (bỏ điều kiện "≤ 60 giây" cũ đang kéo cả video ngang vào).

### Không làm
- Giao diện tối (để F3).
- Dọn bản nháp bỏ dở tự động.
- Màn hình kết thúc, thẻ (cards), bản quyền ở bước "Các thành phần" / "Kiểm tra" như YouTube.
- Tải nhiều tệp một lúc.

### Điều tuyệt đối không được xảy ra
- Bản nháp lộ ra cho người khác (trang chủ, tìm kiếm, kênh, Shorts, thông báo).
- Người đăng ký nhận thông báo "video mới" khi video mới chỉ là bản nháp.
- Bấm "Đăng" khi tệp chưa lên xong mà mất dữ liệu đã điền.

## 3. Hiện trạng code (đã đọc)

- Client: `components/video-upload-modal.tsx` (chọn / thả tệp, Lottie `public/icons/upload.json`) → `components/video-details-modal.tsx` (550 dòng, một trang). Mở từ `modules/studio/ui/components/studio-navbar/index.tsx`.
- Upload: `POST /api/videos/uploadFile` (multipart, một request làm hết: Cloudinary, lưu DB, thẻ, Elasticsearch, thông báo). `VideoServiceImpl.createVideo`.
- `isShort`: client gửi `duration <= 60` nhưng backend bỏ qua; backend tự đặt `duration <= SHORT_MAX_SECONDS (60)`. `VideoRepository` (dòng ~85) lấy Shorts = `isShort OR duration <= maxSeconds`.
- Sửa video: `PUT /api/videos/{id}` (multipart: title, description, status, tags, thumbnailFile). Khi trạng thái chuyển từ không công khai sang công khai thì báo người đăng ký (`wasListed`), và luôn đồng bộ Elasticsearch (`VideoIndexer.index` tự gỡ video không công khai).
- Quyền xem: `VideoAccess.canView` cho chủ video / admin xem mọi trạng thái trừ DELETED; người khác chỉ ACTIVE/PUBLIC/UNLISTED.
- Phụ đề AI cho video có sẵn: `POST /api/videos/{id}/subtitles/ai?targetLang=` (`VideoSubtitleController`).
- AI mô tả + thẻ: `POST /api/ai/description {title}` trả `{description, tags}`.
- Thêm vào danh sách phát: `POST /api/playlists/add {playlistId, videoId}` (chủ playlist; video phải xem được, chủ video xem được bản nháp của mình).
- Trạng thái: enum `ActiveStatus`, cột MySQL `videos_status` là ENUM, `VideoSchemaInitializer.extendStatusEnum` tự nối thêm giá trị còn thiếu vào cuối.
- Giới hạn: `max-file-size: 100MB`, `max-request-size: 120MB`.

## 4. Thiết kế

### 4.1 Backend

**Trạng thái mới `DRAFT`** nối vào cuối `ActiveStatus` và vào `REQUIRED_STATUSES` của `VideoSchemaInitializer` (ENUM chỉ nối cuối). `DRAFT` không nằm trong `LISTED`, nên `canView` / `canSeeInList` / `VideoIndexer` tự coi như riêng tư: chỉ chủ video và admin thấy.

**API 1: tạo bản nháp**
`POST /api/videos/drafts` (multipart, cần đăng nhập)
- Tham số: `videoFile` (bắt buộc), `duration`, `width`, `height` (client đọc từ thẻ `<video>`, có thể thiếu).
- Làm: tải lên Cloudinary, tạo `Video` với `videosStatus = DRAFT`, `title` = tên tệp bỏ đuôi (như YouTube), `duration`, `isShort`. Không gắn thẻ, **không** đồng bộ Elasticsearch, **không** báo người đăng ký. Có thêm vào playlist `USER_UPLOADED` như hiện nay (playlist này lọc theo quyền xem nên bản nháp không lộ).
- Trả `VideoDto` (có `id`, `videoUrl`, `duration`, `isShort`).
- `isShort`: ưu tiên kích thước / thời lượng trong kết quả Cloudinary (`width`, `height`, `duration`); thiếu thì dùng giá trị client gửi. Quy tắc: `duration > 0 && duration <= 180 && width > 0 && height > 0 && width <= height`. Để có số liệu Cloudinary, `CloudinaryService.uploadVideo` cần trả cả map kết quả (hoặc thêm một hàm mới trả map); nơi gọi cũ giữ nguyên hành vi.

**API 2: hoàn tất / đăng** dùng lại `PUT /api/videos/{id}`.
- Đã có: title, description, status (PUBLIC/UNLISTED/PRIVATE), tags, thumbnailFile; chuyển DRAFT → công khai thì báo người đăng ký và đưa vào Elasticsearch (cơ chế `wasListed` có sẵn).
- Cần kiểm tra: `parseVisibility` không nhận `DRAFT` từ client (giữ như vậy: không cho quay lại bản nháp).

**Shorts:** `SHORT_MAX_SECONDS` = 180; truy vấn trang Shorts chỉ còn `v.isShort = true`. `POST /uploadFile` cũ vẫn giữ (để không phá gì), nhưng đặt `isShort` theo cùng quy tắc mới nếu có width/height; client mới không gọi nó nữa.

**Rate limit:** dùng lại `RateLimiter` cho `POST /drafts` như upload hiện tại (nếu upload hiện tại chưa có giới hạn thì không thêm).

### 4.2 Client

**Màn chọn tệp** (`video-upload-modal.tsx`): giữ giao diện. Khi có tệp hợp lệ:
1. Kiểm tra `video/*` và ≤ 100MB; sai thì báo bằng `toastEmitter.error` (bỏ `alert`).
2. Lottie chạy một lượt (không lặp) khoảng 1 giây, rồi chuyển sang 4 bước.
3. Đồng thời bắt đầu tải lên `POST /videos/drafts` bằng **axios** với `onUploadProgress` (fetch / RTK Query không báo được tiến độ gửi) và `AbortController`; gửi kèm cookie như `apiFetch`.

**Hộp thoại 4 bước** (thay `video-details-modal.tsx`, tách component theo bước cho dễ đọc):
- **Đầu hộp thoại:** tiêu đề video + thanh bước (4 chấm). Bấm vào chấm để nhảy bước. Bước có lỗi (vd. thiếu tiêu đề) hiện dấu `!` màu đỏ.
- **Cột phải (mọi bước):** xem trước video từ tệp cục bộ (`URL.createObjectURL`), khung dọc nếu là Shorts; tên tệp; nhãn Shorts kèm lý do; trạng thái tải lên.
- **Chân hộp thoại:** trạng thái tải lên bên trái ("Đang tải lên 45%…" → "Đang xử lý…" → "Đã tải lên" / lỗi + "Thử lại"); nút "Quay lại" / "Tiếp" / "Đăng" bên phải.

Từng bước:
1. **Chi tiết:** Tiêu đề (bắt buộc, điền sẵn tên tệp), Mô tả, Thẻ + nút "Gợi ý thẻ bằng AI" (gọi `/api/ai/description`, chỉ lấy `tags`, gộp với thẻ đã có, cần tiêu đề), Ảnh thu nhỏ (giữ `ThumbnailSelector` + tự chụp khung hình như hiện nay), **Danh sách phát** (dropdown chọn nhiều các playlist của người dùng, có mục "Tạo danh sách phát mới"; dùng lại logic của `save-to-playlist-dialog.tsx` nếu hợp).
2. **Các thành phần của video:** Phụ đề AI (dịch sang) bằng `Select` shadcn; "Không tạo phụ đề" là mặc định.
3. **Kiểm tra:** danh sách tự kiểm: có tiêu đề, tệp đã lên xong, (Shorts hay video thường), ảnh thu nhỏ. Mục nào chưa đạt thì có link về đúng bước.
4. **Chế độ hiển thị:** 3 lựa chọn dạng radio có mô tả (Công khai / Không công khai / Riêng tư). **Mặc định: Công khai** (anh chọn). Nút "Đăng".

**Bấm "Đăng":**
- Nếu tệp chưa lên xong: nút ghi "Đăng khi tải lên xong", bấm thì ghi nhận ý định và tự đăng ngay khi có id bản nháp (không mất dữ liệu đã điền).
- Thứ tự: `PUT /videos/{id}` (chi tiết + chế độ hiển thị + ảnh thu nhỏ) → `POST /playlists/add` cho từng playlist đã chọn → nếu có chọn ngôn ngữ thì `POST /videos/{id}/subtitles/ai`. Lỗi ở bước sau không huỷ bước trước; báo rõ phần nào lỗi.
- Xong: toast "Đã đăng video", đóng hộp thoại, làm mới danh sách video Studio và playlist (invalidate tag như hiện nay).

**Đóng hộp thoại:**
- Đang tải lên: hỏi "Huỷ tải lên?". Đồng ý thì `abort()`; chưa có bản nháp nên không để lại gì.
- Đã có bản nháp mà chưa đăng: đóng luôn, báo "Đã lưu bản nháp trong Studio".

**Studio** (`video-section.tsx`, trang sửa `/studio/videos/[id]`): bản nháp hiện nhãn "Bản nháp"; trang sửa cho chọn chế độ hiển thị để đăng (trạng thái `DRAFT` hiện là "Bản nháp" trong `Select`, không chọn lại được).

## 5. Kiểm chứng

- Backend: `./gradlew compileJava compileTestJava`; sau deploy: `POST /api/videos/drafts` trả 401 khi chưa đăng nhập; log có "Đã mở rộng videos.videos_status" (thêm `DRAFT`) và không có Exception.
- Client: `tsc` (không lỗi mới), eslint các tệp đã sửa, `next build`; trang Studio trả 200.
- Anh thử trên trình duyệt: tệp dọc ngắn → nhãn Shorts, vào trang Shorts; tệp ngang ngắn → video thường; % chạy thật; đóng giữa chừng; bấm Đăng trước khi tải xong; chọn 2 danh sách phát; bản nháp không hiện ở kênh / trang chủ / tìm kiếm khi xem bằng tài khoản khác; người đăng ký nhận thông báo chỉ khi đăng công khai.

## 6. Những chỗ em tự quyết (anh xem lại khi đọc)

- **Bản nháp trong Studio** mở bằng trang sửa video có sẵn (`/studio/videos/[id]`), không mở lại hộp thoại 4 bước (YouTube có mở lại; làm vậy tốn thêm công vì hộp thoại phải chạy được khi không có tệp cục bộ).
- **Huỷ lúc "Đang xử lý"** (đã gửi hết byte, server đang đẩy lên Cloudinary): client huỷ được request nhưng server vẫn làm tiếp, nên bản nháp có thể vẫn xuất hiện trong Studio. Chấp nhận, không xử lý thêm.
- **Không sửa dữ liệu cũ** cho Shorts: video cũ giữ cờ `isShort` đã lưu (production hiện chỉ có 1 video công khai).
- **Thẻ AI gợi ý** được gộp vào thẻ đã có (bỏ trùng), không ghi đè.
- **Danh sách phát** chỉ hiện playlist do người dùng tạo (`USER_PLAYLIST`), không hiện playlist hệ thống (Đã thích, Xem sau, Lịch sử…).
- **Không thêm giới hạn tần suất mới** cho `POST /drafts` nếu upload hiện tại chưa có.

- **Dòng chữ ở màn chọn tệp** "Video của bạn sẽ ở chế độ riêng tư cho đến khi bạn công khai" đổi thành "Video sẽ được lưu dưới dạng bản nháp cho đến khi bạn đăng." cho khớp với mặc định Công khai.

## 7. Câu hỏi đã trả lời

- Chế độ hiển thị mặc định ở bước 4: **Công khai** (anh chọn 2026-10-24).

Trạng thái: anh đã duyệt (2026-10-24).
