# Thiết kế: ghi hình buổi phát để xem lại (L2)

Ngày: 2026-10-28. Trạng thái: **chờ anh duyệt**.

## 1. Vì sao

Anh thử L1 ngày 28/10: kết thúc buổi phát thì trong lịch sử không có video của buổi đó. Đúng theo thiết kế L1 (anh chọn phương án (a) ngày 27/10: chỉ lưu số liệu, không ghi hình). Nay anh muốn có video xem lại.

### Anh đã chốt (28/10)
- Lưu bản ghi ở **Cloudflare R2**.
- Bản ghi thành **video công khai của kênh**.
- Buổi phát bằng **OBS không ghi hình trên server**: OBS tự có chức năng Record, creator tải file lên như một video thường (luồng đăng video sẵn có).
- Chỉ ghi buổi phát **từ trình duyệt**, theo ô "Ghi hình để xem lại" creator **tự bật** (tắt sẵn), kèm **trần theo kênh** vì hạn mức LiveKit là của chung.
- Anh duyệt "flow" em đề xuất (Cách 1: LiveKit ghi giúp ở phía máy chủ, không ghi bằng trình duyệt).

### Những gì em tra được (28/10, đọc tài liệu LiveKit, chưa thử thật)
- Nguồn: [quotas and limits](https://docs.livekit.io/deploy/admin/quotas-and-limits/), [egress outputs](https://docs.livekit.io/transport/media/ingress-egress/egress/outputs/).
- **Gói free (Build):** ghi hình tính vào phút transcode, **60 phút mỗi tháng**, hạn mức cứng (hết thì yêu cầu mới thất bại, không tính thêm tiền), tối đa 2 phiên ghi cùng lúc, mỗi file MP4 tối đa 3 giờ. Phút transcode này **dùng chung** với OBS RTMP (L1 mặc định WHIP nên không đụng). Hạn mức là của **tài khoản LiveKit mà WeTube dùng**, mọi kênh dùng chung, không chia theo creator.
- **LiveKit Cloud không giữ file giúp:** phải có kho của mình. Kho tương thích S3 (R2, Backblaze...) khai báo bằng `endpoint` (bắt đầu `https://`) và `force_path_style=true`.
- LiveKit không có API cho biết còn bao nhiêu phút trong hạn mức. Backend chỉ tự đếm được phần WeTube đã ghi.
- SDK Java đã có (`io.livekit:livekit-server:0.10.0` có `EgressServiceClient`: `startRoomCompositeEgress`, `startParticipantEgress`...). Không phải thêm thư viện cho phần gọi LiveKit.

### Code hiện tại (em đọc 28/10)
- `LiveKitGateway` là chỗ duy nhất gọi LiveKit (Ingress, phòng, token); sẽ thêm hàm ghi hình vào đây để test bằng Mockito.
- `LiveStreamService.promote()` chuyển CREATED → LIVE; `finish()` kết thúc / huỷ buổi và xoá phòng; job 15 giây (`refreshAll`) đã duyệt từng buổi chưa kết thúc. Ghi hình bám vào ba chỗ này.
- Trình phát video của client phát `video.videoUrl` bằng thẻ `<video>` thường, ảnh xem trước thanh tua chỉ lấy từ Cloudinary và có đường lùi khi link không phải Cloudinary (`lib/videoMedia.ts`), nên link R2 dùng được.
- Video bắt buộc có **chủ đề** (C8) khi đăng, nên bản ghi cũng cần một chủ đề.
- Tính năng phụ đề AI chỉ làm việc với video trên Cloudinary (`audioUrlFor`), nên bản ghi live **không có phụ đề AI**.

### Thành công nghĩa là
- Ở form chuẩn bị (kiểu phát từ trình duyệt) có ô **Ghi hình để xem lại** (tắt sẵn). Bật thì phải chọn chủ đề, và form cho biết kênh còn bao nhiêu phút ghi trong tháng.
- Buổi đang phát thì bảng điều khiển của chủ kênh có nhãn **● Đang ghi hình**.
- Kết thúc buổi: trong khoảng 1 phút, bản ghi thành một **video công khai** trong kênh (có bình luận, thích, lịch sử xem, tìm kiếm). Trang `/live/{id}` đã kết thúc có nút **Xem lại**.
- Buổi OBS và buổi không bật ghi hình hoạt động y như hiện nay.
- Hết hạn mức (của kênh hoặc chung) hoặc LiveKit lỗi khi ghi: **buổi vẫn phát bình thường**, chủ kênh thấy lý do bằng tiếng Việt, buổi đó không có video.
- Một kênh không ghi quá trần mỗi tháng, không kênh nào dùng hết phần của kênh khác.

## 2. Phạm vi

### 2.1 Form chuẩn bị (`/studio/live`)
- Chỉ hiện khi **Nguồn phát = Phát từ trình duyệt** và máy chủ đã bật ghi hình (`GET /api/live/recording`, xem 3.3). Chưa cấu hình R2 thì ô này không hiện.
- Ô **Ghi hình để xem lại**, tắt sẵn, kèm dòng phụ: "Kênh còn X phút ghi hình trong tháng này. Hết thì buổi vẫn phát nhưng không có video xem lại."
- Bật ô này thì hiện **Chủ đề** (dùng lại `topic-field.tsx` của luồng đăng video), bắt buộc. Không bật thì không hỏi chủ đề.
- Hết phần của kênh hoặc hạn mức chung gần hết (xem 2.3): ô bị khoá, kèm lý do.

### 2.2 Trong lúc phát
- Bảng điều khiển hiện nhãn **● Đang ghi hình** và số phút đã ghi.
- Chủ kênh không dừng / bật lại ghi hình giữa chừng (làm đơn giản: ghi từ lúc lên sóng đến lúc kết thúc).
- Ghi **đúng những gì người xem thấy**: dùng bố cục "speaker" của LiveKit (màn hình chia sẻ là hình chính, camera ở khung nhỏ, một mình camera thì toàn khung), tiếng gồm micro và tiếng tab / hệ thống. *Em chưa kiểm tra bố cục này trên LiveKit thật; lần thử đầu tiên sẽ xác nhận (xem mục 4).*
- Chất lượng: MP4, H.264, **720p 30 khung / giây, khoảng 1,5 Mb/giây hình + 96 kb/giây tiếng**, khoảng 0,7 GB cho một giờ. Hai con số này là hằng số ở một chỗ, đổi được. Với tổng hạn mức 60 phút mỗi tháng thì mỗi tháng dùng tối đa khoảng 0,7 GB trong 10 GB miễn phí của R2.
- Mỗi file tối đa 3 giờ (giới hạn của LiveKit). Quá thì bản ghi dừng ở mốc đó, buổi vẫn phát tiếp, bản ghi 3 giờ vẫn thành video.

### 2.3 Hạn mức
- **Trần theo kênh:** mặc định **15 phút mỗi tháng** cho mỗi kênh (biến `LIVE_RECORDING_CHANNEL_MINUTES`, đổi được không cần sửa code). Tính theo tháng dương lịch UTC (trùng lúc LiveKit đặt lại hạn mức).
- **Trần chung của WeTube:** mặc định **55 phút mỗi tháng** (biến `LIVE_RECORDING_TOTAL_MINUTES`, chừa 5 phút dự phòng cho phần anh tự thử ngoài app). Chạm trần thì mọi kênh không bắt đầu ghi mới được.
- **Cách đếm:** tổng thời lượng các bản ghi đã bắt đầu trong tháng (bản đang ghi tính theo giờ trôi qua). Lưu ở DB.
- **Chạm trần giữa chừng:** job 15 giây thấy buổi đang ghi đã đủ phần của kênh (hoặc phần chung) thì dừng ghi; phần đã ghi vẫn thành video (có thể ngắn hơn buổi phát), buổi tiếp tục phát. Chủ kênh thấy "Đã ghi đủ phần ghi hình của tháng, buổi vẫn đang phát nhưng không ghi tiếp".
- Số phút còn lại là **ước tính** theo số WeTube đã ghi. Nếu LiveKit từ chối vì hết hạn mức thật (anh dùng ngoài app hoặc tính chênh), bản ghi báo lỗi "Hết hạn mức ghi hình của LiveKit", buổi vẫn phát.

### 2.4 Kết thúc buổi và tạo video
- `finish()` (kết thúc tay, tự kết thúc, huỷ): nếu đang ghi thì báo LiveKit **dừng ghi trước khi xoá phòng**.
- Job 15 giây hỏi LiveKit kết quả của phiên ghi đang dừng; **xong thì lấy link file và thời lượng** rồi tạo video:
  - Chủ sở hữu là chủ kênh, **công khai**, tiêu đề = tiêu đề buổi phát, chủ đề đã chọn, thời lượng thật.
  - Mô tả bắt đầu bằng dòng "Đã phát trực tiếp ngày dd/MM/yyyy", sau đó là mô tả buổi phát.
  - Ảnh bìa là ảnh thu nhỏ của buổi phát (không có ảnh thì như video chưa có ảnh bìa).
  - Đưa vào chỉ mục tìm kiếm như video thường. **Không gửi thông báo** cho người đăng ký (họ đã được báo lúc lên sóng).
- Bản ghi ngắn dưới **30 giây** thì bỏ qua, không tạo video.
- Backend khởi động lại giữa chừng: phiên ghi vẫn chạy ở LiveKit và trạng thái nằm trong DB, nên job làm tiếp được.
- **Không có kết quả sau 30 phút** kể từ lúc dừng: đánh dấu lỗi và ghi log, không thử lại mãi.

### 2.5 Trang xem và các thẻ
- `/live/{id}` đã kết thúc: có bản ghi thì nút **Xem lại** tới `/watch/{videoId}`; đang xử lý thì "Đang xử lý bản ghi…" (tự cập nhật); lỗi hoặc không ghi thì không hiện gì (chủ kênh thấy lý do ở Studio).
- Thẻ buổi đã phát ở tab **Trực tiếp** của kênh: giữ như hiện nay, bấm vào `/live/{id}` rồi bấm Xem lại. Video xem lại cũng nằm trong tab **Video** của kênh, trang chủ, tìm kiếm như video thường.

### 2.6 Không làm trong L2
- Ghi hình buổi **OBS** (anh chốt: OBS tự quay).
- **Nhãn "Đã phát trực tiếp" trên thẻ video.** Làm đúng phải thêm cột vào bảng `videos` và sửa mọi DTO / thẻ video; thay bằng dòng đầu mô tả và nút Xem lại. Anh muốn nhãn thì tính thành việc riêng.
- Xoá file R2 khi chủ kênh xoá video (cần thêm thư viện S3 vào backend). Hậu quả: file mồ côi, dọn tay trong bảng điều khiển R2 khi cần; với tối đa 60 phút mỗi tháng thì dung lượng nhỏ.
- Phụ đề AI cho bản ghi (hiện chỉ chạy với Cloudinary).
- Dừng / bật ghi giữa chừng, ghi theo lịch, cắt bản ghi.
- Ghi buổi từ điện thoại ngoài những gì LiveKit đã nhận (không có gì khác).
- Trang thống kê hạn mức ghi hình; chỉ có dòng "còn X phút" ở form.

## 3. Cách làm

### 3.1 Cấu hình (biến môi trường backend, Vibe Host)
| Biến | Ý nghĩa |
|---|---|
| `LIVE_RECORDING_ENABLED` | `true` mới bật tính năng; không đặt thì ô ghi hình không hiện và API từ chối |
| `LIVE_RECORDING_S3_ENDPOINT` | `https://<account-id>.r2.cloudflarestorage.com` |
| `LIVE_RECORDING_S3_BUCKET` | tên bucket |
| `LIVE_RECORDING_S3_ACCESS_KEY` / `LIVE_RECORDING_S3_SECRET` | khoá API của R2 (**đánh dấu secret**, thêm vào việc G1) |
| `LIVE_RECORDING_PUBLIC_URL` | địa chỉ công khai của bucket, ví dụ `https://pub-xxxx.r2.dev` (hoặc tên miền riêng), không có dấu `/` ở cuối |
| `LIVE_RECORDING_CHANNEL_MINUTES` | mặc định 15 |
| `LIVE_RECORDING_TOTAL_MINUTES` | mặc định 55 |

Backend tự đặt `force_path_style=true` khi gọi LiveKit. Vùng ký lấy từ `LIVE_RECORDING_S3_REGION` (tuỳ chọn, mặc định `auto` cho R2; **Backblaze B2 phải đặt đúng vùng của bucket**, ví dụ `us-west-004` nếu địa chỉ là `s3.us-west-004.backblazeb2.com`). Thêm sau khi anh chọn Backblaze (28/10). File lưu ở `live/{streamId}.mp4`.

### 3.2 Dữ liệu
- **Thêm cột vào `live_streams`** (runner `LiveSchemaInitializer`, bước `addColumn` đã có):
  - `recording_enabled` (bit, mặc định 0), `topic_id` (varchar 100, chủ đề chọn lúc tạo).
  - `recording_status` (varchar 20): `NONE` | `RECORDING` | `STOPPING` | `DONE` | `FAILED` | `SKIPPED`.
  - `egress_id` (varchar 100), `recording_started_at`, `recording_stopped_at`, `recording_seconds` (int), `recording_url` (varchar 500), `recording_message` (varchar 200, lý do bằng tiếng Việt), `replay_video_id` (varchar 36).
  - Chỉ mục `(channel_id, recording_started_at)` để cộng dồn theo tháng.
- Không đụng bảng `videos`: bản ghi là một dòng `videos` bình thường (`videoUrl` trỏ vào R2).

### 3.3 Backend
- **`LiveKitGateway`** thêm: `startRecording(room, filepath)` (room composite, bố cục "speaker", MP4, mã hoá ở 2.2, đích S3/R2), `stopRecording(egressId)`, `recordingResult(egressId)` (trạng thái, link, thời lượng). Lỗi hạn mức dùng `LiveKitUnavailableException.isQuota()` có sẵn.
- **`LiveStreamService`:**
  - `create`: nhận `recordingEnabled` + `topicId`. Chỉ nhận khi nguồn là BROWSER, ghi hình đã bật ở máy chủ, chủ đề có trong `TopicCatalog` và còn phần ghi (kênh và chung). Sai thì 400 / 409 kèm `message` tiếng Việt.
  - `promote()` (lên sóng): nếu `recording_enabled` thì bắt đầu ghi, lưu `egress_id`, `RECORDING`. LiveKit lỗi thì `FAILED` + lý do, **buổi vẫn LIVE**.
  - `refresh()` (job 15 giây): dừng ghi khi chạm trần (2.3) hoặc khi phiên ghi tự kết thúc ở LiveKit (3 giờ); với buổi `STOPPING` thì hỏi kết quả và tạo video khi xong.
  - `finish()`: nếu `RECORDING` thì dừng ghi trước khi xoá phòng, `STOPPING`.
  - Tạo video qua một hàm riêng của `VideoService` (không đi qua `createVideo` vì không có file tải lên), rồi `VideoIndexer` đánh chỉ mục.
- **API mới:** `GET /api/live/recording` (đăng nhập) trả `{available, channelMinutesLeft, totalAvailable}` cho form. `LiveView` thêm `recordingEnabled`, `recordingStatus`, `recordingMessage` (chỉ cho chủ kênh), `replayVideoId`.
- **Bảo mật:** khoá R2 chỉ ở backend; `recordingMessage` chỉ trả cho chủ kênh; admin vẫn kết thúc được buổi như cũ.
- **Test (Mockito + H2 `ddl-auto=none` như L1):** chỉ nhận ghi hình cho BROWSER và có chủ đề hợp lệ; trần theo kênh và trần chung (kể cả bản đang ghi, đổi tháng); `promote` lỗi LiveKit thì buổi vẫn LIVE; dừng ghi trước khi xoá phòng; tạo video đúng trường, không gửi thông báo, bỏ qua bản dưới 30 giây; dừng ở mốc chạm trần; quá 30 phút không có kết quả thì `FAILED`; cột mới trên bảng cũ.

### 3.4 Client
- `live-setup-form.tsx`: ô Ghi hình + Chủ đề + dòng "còn X phút" (gọi `GET /api/live/recording`); gửi `recordingEnabled`, `topicId` khi tạo buổi.
- `live-onair-panel.tsx`: nhãn ● Đang ghi hình, hiện `recordingMessage` khi lỗi hoặc chạm trần.
- `live-watch-view.tsx` (màn hình đã kết thúc): nút Xem lại / "Đang xử lý bản ghi…" (làm mới mỗi 10 giây khi `STOPPING`).
- Kiểu `LiveView`, `CreateLiveBody` trong `modules/live/types.ts`.

## 4. Chia gói và cách kiểm tra
Mỗi gói một commit ở máy; em hỏi anh trước khi push và deploy (backend trước, client sau).

1. **Backend:** cột, cấu hình, `LiveKitGateway`, dịch vụ, tạo video, API, test. Kiểm tra: `compileJava compileTestJava` + cả bộ test.
2. **Client:** form, nhãn, nút Xem lại. Kiểm tra: `tsc`, `eslint`, `next build`, Chrome thật với API giả như L1.1.
3. **Cấu hình R2 (việc của anh) rồi thử thật.** Em **không thử được** phần nối LiveKit và R2 thật (không có tài khoản, và hệ thống đã chặn việc chạy LiveKit cục bộ ở lần trước). Vì vậy:
   - Em deploy khi chưa đặt `LIVE_RECORDING_ENABLED`: ô ghi hình **ẩn**, mọi thứ chạy như hiện nay, không rủi ro cho buổi phát.
   - Anh tạo R2 (3.5), đặt biến môi trường, rồi thử **một buổi ngắn 2 phút** (mục kiểm tra trong `TEST_CHECKLIST.md`). Thử để xác nhận ba điều em chưa chắc: bố cục "speaker" có đúng ý (màn hình chính, camera góc), tiếng có đủ, và con số phút bị tính vào hạn mức LiveKit.

Cỡ ước tính: khoảng 1,5–2 ngày làm.

### 3.5 Việc của anh (sau khi thiết kế được duyệt, trước bước 3)
1. Tạo tài khoản Cloudflare → **R2** → **Create bucket** (ví dụ `wetube-live`).
2. Trong bucket: **Settings → Public access**, bật địa chỉ `r2.dev` (hoặc nối tên miền riêng, tốt hơn cho lâu dài; `r2.dev` có giới hạn tốc độ nên chỉ hợp lúc thử và quy mô nhỏ). Chép địa chỉ công khai.
3. **R2 → Manage API tokens → Create API token**, quyền **Object Read & Write**, chỉ cho bucket này. Chép **Access Key ID**, **Secret Access Key** và **Endpoint** (`https://<account-id>.r2.cloudflarestorage.com`).
4. Đặt các biến ở mục 3.1 trực tiếp trên **Vibe Host** (đừng gửi khoá vào chat), đánh dấu secret cho hai khoá.

## 5. Điểm anh cần chốt khi duyệt
1. **Trần theo kênh 15 phút mỗi tháng** và **trần chung 55 phút**: đồng ý, hay đổi số?
2. **Chủ đề bắt buộc khi bật ghi hình** (vì video nào cũng cần một chủ đề): đồng ý, hay anh muốn tự đặt một chủ đề mặc định thuộc nhóm "Chủ đề khác" của danh sách chủ đề?
3. **Bỏ nhãn "Đã phát trực tiếp" trên thẻ video** ở bản đầu (thay bằng dòng đầu mô tả và nút Xem lại): đồng ý, hay anh muốn có nhãn và chấp nhận việc lớn hơn?
