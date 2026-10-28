# Thiết kế: nguồn hình khi phát từ trình duyệt và ảnh thu nhỏ cho buổi phát (L1.1)

Ngày: 2026-10-28. Trạng thái: **chờ anh duyệt**.

## 1. Vì sao

Anh hỏi ngày 28/10:
1. Không muốn hiện facecam thì phát thế nào?
2. Quay màn hình được không, nếu không dùng OBS?
3. Thẻ buổi phát có ảnh thu nhỏ riêng được không, như YouTube?

Anh đã chốt: **chưa làm** lấy khung hình tự động (để sau); buổi phát kết thúc thì thẻ trong lịch sử **giữ** ảnh thu nhỏ.

### Code hiện tại (em đọc 28/10)
- **Phát từ trình duyệt luôn bật camera.** `WebcamBroadcast` vào phòng LiveKit với `audio` và `video` bật sẵn (`live-onair-panel.tsx`). Phần xem trước chỉ chờ track `Camera`, nên chỉ khi có camera mới báo backend "đã lên sóng". Form chuẩn bị cũng mở camera ngay để xem trước.
- Đang phát thì tắt camera được (`live-device-settings.tsx`). Khi đó người xem rơi vào màn hình "Người phát đang gặp sự cố kết nối…", vì trình phát chỉ xét track hình.
- **Backend chỉ coi là "đang phát" khi có track hình** (`LiveKitGatewayImpl.participants`: `publishingVideo` = có track `VIDEO`). Phát chỉ có tiếng thì buổi mãi ở CREATED, sau 1 giờ bị huỷ.
- **Người xem đã đọc được track màn hình:** `live-player.tsx` lấy `Camera` và `ScreenShare` rồi hiện track đầu tiên. Tiếng đi qua `RoomAudioRenderer`, nên tiếng của tab hoặc màn hình cũng phát được, chung nút âm lượng.
- Token phát không giới hạn nguồn (`CanPublish(true)`), nên chia sẻ màn hình không cần đổi token.
- **Thẻ buổi phát dùng banner của kênh** (`live-card.tsx`), không có thì nền tối với avatar ở giữa. Mọi buổi của một kênh có cùng một ảnh. Thẻ chia sẻ link `/live/{id}` (og:image) dùng avatar kênh. Bảng `live_streams` không có cột ảnh.
- Studio đã có sẵn ô chọn ảnh thu nhỏ (`components/ThumbnailSelector.tsx`, PNG / JPEG, khung 16:9). `CloudinaryService.uploadThumbnail` tự cắt 1280×720.

### Thành công nghĩa là
- Ở form chuẩn bị, khi chọn phát từ trình duyệt, chủ kênh chọn một trong bốn kiểu: **Camera**, **Màn hình**, **Màn hình + camera**, **Chỉ âm thanh**. Chọn kiểu không có camera thì **camera không bao giờ được bật**, kể cả để xem trước, và trình duyệt không xin quyền camera.
- Phát màn hình không cần OBS: chọn một tab, một cửa sổ hoặc cả màn hình. Trên Chrome / Edge có thể kèm tiếng của tab hoặc của hệ thống.
- Đang phát vẫn đổi được: bật / tắt camera, bắt đầu / dừng chia sẻ màn hình, bật / tắt micro.
- Người xem luôn thấy thứ có nghĩa: màn hình, camera, cả hai (camera nằm ở góc), hoặc ảnh thu nhỏ kèm dòng "Đang phát âm thanh" khi không có hình. Chỉ hiện "sự cố kết nối" khi người phát thật sự rời phòng.
- Chủ kênh tải ảnh thu nhỏ lúc chuẩn bị hoặc đổi trong lúc đang phát. Thẻ ở `/live`, trang chủ, tab Trực tiếp của kênh và thẻ chia sẻ link đều dùng ảnh đó. Không tải ảnh thì giữ như hiện nay (banner, rồi avatar).

## 2. Phạm vi

### 2.1 Chọn nguồn hình (phát từ trình duyệt)
- Lựa chọn "Webcam" ở form chuẩn bị đổi tên thành **Phát từ trình duyệt**. Bên dưới có bốn kiểu:

  | Kiểu | Hình | Tiếng | Xem trước ở form |
  |---|---|---|---|
  | **Camera** (mặc định, như hiện nay) | camera | micro | camera |
  | **Màn hình** | màn hình | micro, kèm tiếng tab / hệ thống nếu trình duyệt cho | ô "Bạn sẽ chọn màn hình sau khi bấm Bắt đầu" |
  | **Màn hình + camera** | màn hình, camera nhỏ ở góc | như trên | camera |
  | **Chỉ âm thanh** | không | micro | ảnh thu nhỏ (nếu có) hoặc avatar kênh |

- Kiểu **Phần mềm phát (OBS)** giữ nguyên.
- **Điện thoại:** trình duyệt điện thoại gần như không chia sẻ màn hình được. Nếu `navigator.mediaDevices.getDisplayMedia` không có thì ẩn hai kiểu có màn hình.
- Kiểu đã chọn lưu trong `localStorage` theo id buổi phát, để tải lại trang không tự bật camera ngoài ý muốn. Không lưu ở backend vì backend không cần biết.

### 2.2 Đang phát (chủ kênh)
- Thanh thiết bị có ba nút: **Micro**, **Camera** (kèm chọn thiết bị như hiện nay), và nút mới **Chia sẻ màn hình** / **Dừng chia sẻ**.
- **Trình duyệt bắt chia sẻ màn hình phải bắt đầu từ một cú bấm.** Với kiểu có màn hình, sau khi bấm "Bắt đầu" thì khung xem trước hiện nút lớn **Chọn màn hình để chia sẻ**. Bấm vào thì trình duyệt mở hộp chọn tab / cửa sổ / màn hình. Tải lại trang giữa chừng cũng hiện lại nút này.
- Bấm "Dừng chia sẻ" trên thanh của Chrome thì khung xem trước hiện "Đã dừng chia sẻ màn hình" và nút chia sẻ lại. Buổi phát vẫn tiếp tục nếu còn micro hoặc camera.
- Xem trước: camera vẫn lật như gương, màn hình **không lật**. Kiểu "Màn hình + camera" hiện camera nhỏ ở góc dưới bên phải, giống những gì người xem thấy.
- **Chất lượng màn hình:** 1080p, 15 khung hình mỗi giây. Chữ vẫn rõ mà ít tốn băng thông hơn 30 khung. Phát game hay video chuyển động nhanh sẽ hơi giật; muốn mượt thì dùng OBS. *Em chưa đo băng thông thật.*

### 2.3 Người xem
- Có màn hình: màn hình làm hình chính. Có thêm camera thì camera nằm trong khung nhỏ ở góc dưới bên phải (khoảng 1/5 chiều rộng, bo góc). Nút Ảnh trong ảnh (PiP) của trình duyệt chỉ mang theo hình chính.
- Chỉ có camera: như hiện nay.
- **Người phát còn trong phòng nhưng không có hình** (kiểu "Chỉ âm thanh", hoặc tắt camera giữa chừng): hiện ảnh thu nhỏ (không có thì avatar kênh) trên nền tối, kèm dòng "Đang phát âm thanh". Tiếng vẫn phát bình thường.
- Người phát rời phòng: "Người phát đang gặp sự cố kết nối…" như cũ.

### 2.4 Backend coi là "đang phát" khi có bất kỳ track nào
- `publishingVideo` đổi thành `publishing`: người phát có **bất kỳ** track nào (hình hoặc tiếng).
- Ảnh hưởng:
  - Chuyển CREATED → LIVE, gửi thông báo, ghi `last_publisher_at` và tự kết thúc sau 2 phút mất người phát đều tính theo track bất kỳ. Phát chỉ âm thanh vẫn lên sóng và không bị huỷ sau 1 giờ.
  - `POST /api/live/{id}/started` cũng kiểm theo track bất kỳ.
  - OBS không đổi gì, vì OBS luôn gửi cả hình lẫn tiếng.
- Tắt micro trong kiểu "Chỉ âm thanh" là không còn track nào. Quá 2 phút thì buổi tự kết thúc như luật hiện tại. Em giữ luật này; thanh thiết bị nhắc "Buổi phát sẽ tự kết thúc nếu tắt cả hình và tiếng quá 2 phút".

### 2.5 Ảnh thu nhỏ
- **Không bắt buộc.** Định dạng PNG hoặc JPEG, tối đa **2 MB** (bằng YouTube), tự cắt 16:9 thành 1280×720 bằng `uploadThumbnail` sẵn có.
- **Ở form chuẩn bị:** ô chọn ảnh dùng lại `ThumbnailSelector`, đặt dưới phần mô tả. Bấm Bắt đầu thì tạo buổi phát trước, rồi tải ảnh lên. Tải ảnh lỗi thì buổi phát vẫn chạy, toast báo "Chưa tải được ảnh thu nhỏ, bạn có thể đổi ảnh khi đang phát".
- **Trong lúc chuẩn bị hoặc đang phát:** bảng điều khiển của chủ kênh có ảnh nhỏ và nút **Đổi ảnh thu nhỏ**. Đổi ảnh thì ảnh cũ bị xoá trên Cloudinary.
- **Hiện ở đâu** (thứ tự ưu tiên ảnh thu nhỏ → banner kênh → avatar kênh như hiện nay):
  - thẻ ở `/live`, chip Trực tiếp ở trang chủ, tab Trực tiếp của kênh;
  - thẻ chia sẻ link `/live/{id}` (`og:image`, `twitter:image`);
  - khung của người xem khi không có hình (mục 2.3).
- **Buổi đã kết thúc giữ ảnh** (anh chốt). Buổi bị huỷ vì không lên sóng sau 1 giờ (CANCELED) thì không vào lịch sử, nên xoá ảnh của nó trên Cloudinary lúc huỷ.
- Sau khi kết thúc thì không đổi ảnh được nữa (như mọi chỉnh sửa khác của buổi đã kết thúc).

### 2.6 Không làm trong L1.1
- Lấy khung hình tự động làm ảnh thu nhỏ (anh chốt để sau).
- Ghép nhiều nguồn, lớp phủ, chuyển cảnh như OBS. Người xem tự đặt camera vào góc; không ghép thành một luồng.
- Chọn vị trí hoặc kích thước khung camera nhỏ.
- Chia sẻ màn hình trên điện thoại.
- Cho người xem tắt khung camera nhỏ.
- Nén hay cắt ảnh ở trình duyệt trước khi tải lên (Cloudinary đã cắt 16:9).

## 3. Cách làm

### 3.1 Backend
- **Cột mới** `live_streams.thumbnail_url VARCHAR(500) NULL`:
  - entity `LiveStream.thumbnailUrl`;
  - thêm vào câu `CREATE TABLE IF NOT EXISTS` của `LiveSchemaInitializer` (cho DB mới);
  - thêm bước `ALTER TABLE live_streams ADD COLUMN thumbnail_url VARCHAR(500) NULL`, chỉ chạy khi `information_schema.COLUMNS` chưa có cột, mỗi câu một transaction (mẫu `addColumn` của `CommentSchemaInitializer`). Bảng đã có trên production, nên câu `CREATE TABLE IF NOT EXISTS` không thêm cột cho nó; bước này bắt buộc.
- **API mới:** `POST /api/live/{id}/thumbnail`, multipart, trường `file`.
  - Chỉ chủ kênh của buổi đó, buổi chưa kết thúc. Khác thì 403 / 404 / 409 như các API sửa hiện có.
  - Kiểm `image/png` hoặc `image/jpeg` và ≤ 2 MB, sai thì 400 kèm `message` tiếng Việt.
  - Tải lên bằng `uploadThumbnail(file, false)`, lưu link, xoá ảnh cũ bằng `deleteImage`.
  - Giới hạn tần suất: 20 lần / giờ / người (`RateLimiter`).
  - Trả `LiveView` mới. Không đẩy qua WebSocket: người xem đang ở trang thấy ảnh mới khi tải lại (ảnh chỉ hiện trong trình phát khi không có hình).
- `LiveView` và `LiveCard` thêm `thumbnailUrl`.
- Lúc huỷ buổi (CANCELED) thì xoá ảnh trên Cloudinary. Lỗi xoá chỉ ghi log, không chặn việc huỷ.
- `Participant.publishingVideo` → `publishing` (track bất kỳ), sửa hai chỗ dùng trong `LiveStreamService` (mục 2.4).

### 3.2 Client
- `live-setup-form.tsx`:
  - chọn kiểu (mục 2.1); `CameraPreview` chỉ chạy khi kiểu có camera;
  - `ThumbnailSelector`; sau khi tạo buổi thì gọi API ảnh.
- `liveApi.ts`: mutation `uploadLiveThumbnail` (FormData), làm mới `getLive` / `getMyLive`; kiểu `thumbnailUrl` trong `types.ts`.
- `live-onair-panel.tsx`:
  - `LiveKitRoom` bật `video` chỉ khi kiểu có camera, `audio` luôn bật;
  - `PublisherStage` báo "đã lên sóng" khi có **bất kỳ** track nào của mình (camera, màn hình hoặc micro), không chỉ camera;
  - xem trước theo mục 2.2; nút "Chọn màn hình để chia sẻ" gọi `localParticipant.setScreenShareEnabled(true, { audio: true, resolution: 1080p 15 khung })`;
  - ảnh thu nhỏ + nút đổi ảnh.
- `live-device-settings.tsx`: thêm nút chia sẻ màn hình, ẩn khi trình duyệt không hỗ trợ.
- `live-player.tsx`: chọn hình chính / khung góc / ảnh thu nhỏ theo mục 2.3. Phân biệt "người phát còn trong phòng nhưng không có hình" với "người phát rời phòng" bằng danh sách người tham gia (identity bắt đầu bằng `u:`).
- `live-card.tsx` và `app/live/[id]/page.tsx` (`generateMetadata`): dùng `thumbnailUrl` trước.

### 3.3 Hạn mức gói free của LiveKit
- **Màn hình + camera** là hai luồng hình, nên mỗi người xem tải về nhiều hơn chỉ camera. LiveKit gửi lớp thấp của camera cho khung nhỏ (simulcast), nên em đoán chỉ tăng thêm khoảng 20–30%, nhưng chưa đo được. Hạn mức 50 GB / tháng sẽ hết nhanh hơn.
- **Chỉ âm thanh** gần như không tốn băng thông, nhưng vẫn tính phút WebRTC như nhau (5.000 phút / tháng tính theo thời gian ở trong phòng, không theo dung lượng).
- Chia sẻ màn hình từ trình duyệt đi qua WebRTC, **không tốn phút transcode** (khác OBS RTMP).

## 4. Chia gói
Mỗi gói một commit, commit ở máy. Em hỏi anh trước khi push và deploy; deploy backend trước, client sau.

1. **Backend.** Cột và runner, API ảnh thu nhỏ, DTO, xoá ảnh khi huỷ, "đang phát" theo track bất kỳ. Test:
   - ảnh: người khác 403, buổi đã kết thúc 409, sai định dạng / quá 2 MB 400, đổi ảnh thì xoá ảnh cũ, huỷ buổi thì xoá ảnh;
   - chỉ có track tiếng thì vẫn chuyển LIVE và không bị tự kết thúc; không còn track nào thì tự kết thúc sau 2 phút;
   - test H2 với `ddl-auto=none` (đã có từ L1) khớp cột mới.
2. **Client: ảnh thu nhỏ.** Form, bảng điều khiển, thẻ, `og:image`.
3. **Client: nguồn hình.** Chọn kiểu, thanh thiết bị, xem trước, trình phát của người xem.
4. **Kiểm tra và ghi chép.**
   - Backend `compileJava compileTestJava` + cả bộ test. Client `tsc`, `eslint`, `next build`.
   - Sau deploy: curl `POST /api/live/{id}/thumbnail` khi chưa đăng nhập trả 401, `GET /api/live` vẫn 200, log không có `Exception`.
   - Chrome thật qua Playwright với camera và màn hình giả (`--use-fake-device-for-media-stream`, `--auto-select-desktop-capture-source`) nếu chạy được, để thử cả bốn kiểu giữa người phát và người xem. Không được thì anh thử theo mục mới trong `TEST_CHECKLIST.md` (28.x).

Gói 2 không phụ thuộc gói 3: nếu anh muốn có ảnh thu nhỏ sớm thì em deploy gói 1 + 2 trước.

Cỡ ước tính: khoảng 1 ngày làm.

## 5. Câu hỏi cho anh
1. **Deploy ảnh thu nhỏ trước** (gói 1 + 2), rồi nguồn hình sau, hay deploy một lần cả L1.1? Em đề xuất một lần, vì gói 1 đã có thay đổi backend cho cả hai.
