# Thiết kế: phát trực tiếp làm lại (L1)

Ngày: 2026-10-27. Trạng thái: **chờ anh duyệt** (anh đã trả lời 4 câu hỏi ngày 27/10, xem mục 5; mục 3.5 mới thêm vì hạn mức gói free của LiveKit).

## 1. Vì sao

PRD luồng 3 và BRD FR-4 yêu cầu:
1. Creator vào **"Go Live"**, nhận Stream Key và cấu hình buổi phát.
2. Phát qua LiveKit (WebRTC), độ trễ dưới 1 giây.
3. Người đăng ký kênh **nhận thông báo** và vào xem.
4. Người xem **chat trực tiếp** và trả lời **khảo sát (Poll)** do người phát tạo.

Lộ trình (`YOUTUBE_PARITY.md` mục L1) thêm: trang "Đang phát trực tiếp", buổi phát gắn với kênh, lưu lịch sử buổi phát, giao diện cùng kiểu site. Chip "Trực tiếp" ở trang chủ (C6) chờ L1.

### Code hiện tại (em đọc và đo 27/10)

**Đang hỏng trên production:**
- `/livestream/host` và `/livestream/watching/{phòng}` trả **500**. Log client: `TypeError: Cannot read properties of undefined (reading 'replace')`. Hai trang đọc `process.env.LIVEKIT_WS_URL`, nhưng client trên Vibe Host chỉ có biến `LIVEKIT_wsUrl`.
- Nút "Join as viewer" sau khi tạo điểm nhận OBS chuyển tới `/watch?at=…&rt=…`, trang không tồn tại (trang xem là `/watch/{videoId}`).

**Lỗ hổng:**
- Client có route Next.js `app/api/streamApi/*` (`create_stream`, `create_ingress`, `join_stream`, `stop_stream`, `invite_to_stage`, `raise_hand`, `remove_from_stage`). Ba route đầu **không kiểm tra đăng nhập** và dùng `LIVEKIT_API_SECRET`, nên ai cũng tạo được phòng và điểm nhận OBS trên tài khoản LiveKit của anh.
- Backend `/api/livekit/*` lấy danh tính từ **nội dung request** (`creatorIdentity`, `identity`), không lấy từ đăng nhập:
  - Ai đăng nhập cũng dừng được buổi phát của người khác, chỉ cần gửi đúng tên người tạo.
  - `join-stream` mở cho cả khách, cấp token chat với bất kỳ tên nào, nên mạo danh được.
- Trang phát nhận token LiveKit của chủ phòng qua URL (`/livestream/host?at=…&rt=…`), nên token lọt vào lịch sử trình duyệt và log.

**Thiếu so với yêu cầu:**
- Buổi phát không gắn với kênh, chỉ là tên tự gõ.
- Người xem phải tự gõ tên phòng. Không có danh sách buổi đang phát.
- Không có thông báo, không có Poll, không lưu lại gì.
- Giao diện Radix Themes riêng, khác hẳn phần còn lại của site.
- Chat qua kênh dữ liệu của LiveKit:
  - Người vào sau không thấy tin cũ.
  - Không giới hạn tần suất, không xoá được tin.
- Code thừa:
  - `lib/controller.ts` (chạy song song với backend).
  - `entity/Room.java` + `RoomRepository` (không ai dùng).
  - "Mời lên sân khấu / giơ tay" gọi route Next.js, xác thực bằng token mà backend Java sinh ra dạng UUID ngẫu nhiên, nên không bao giờ chạy được.

### Thành công nghĩa là
- Chủ kênh vào Studio → **Phát trực tiếp**, điền tiêu đề, chọn **Webcam** hoặc **Phần mềm phát (OBS)**, bấm Bắt đầu. Người xem thấy hình trong khoảng 1 giây (webcam).
- Người đăng ký kênh thấy thông báo "… đang phát trực tiếp: {tiêu đề}" ở chuông (chậm nhất khoảng 1 phút, theo nhịp chuông hiện tại). Bấm vào là tới trang xem buổi phát.
- Trang `/live` liệt kê buổi đang phát. Trang chủ có chip **Trực tiếp** khi có ít nhất một buổi đang phát.
- Người xem chat bằng tài khoản thật. Người vào sau thấy các tin gần đây. Chủ kênh xoá được tin.
- Chủ kênh mở Poll 2–4 lựa chọn. Người xem bấm chọn, kết quả cập nhật trực tiếp cho mọi người.
- Kết thúc thì trang xem hiện "Buổi phát trực tiếp đã kết thúc" cùng thời lượng, số người xem cao nhất và kết quả Poll. Tab **Trực tiếp** của kênh liệt kê các buổi đã phát.
- Không còn cách nào tạo phòng, dừng phòng hay chat dưới tên người khác.

## 2. Phạm vi

### 2.1 Phát (chủ kênh) – `/studio/live`
- Vào từ Studio (mục mới **Phát trực tiếp** ở thanh bên Studio) và từ nút "Tạo" trên thanh điều hướng. Mục "Phát trực tiếp" trong nút Tạo đã có sẵn nhưng bấm chỉ đóng menu, nay trỏ tới `/studio/live`.
- **Phải đăng nhập và có kênh.** Kênh bị ẩn (INACTIVE) không phát được (giống luật chặn đăng video ở mục 2.6 của `YOUTUBE_PARITY.md`).
- **Mỗi kênh một buổi chưa kết thúc tại một thời điểm.** Mở `/studio/live` khi đã có buổi chưa kết thúc thì quay lại buổi đó (kể cả sau khi tải lại trang hoặc đổi máy).
- **Bước 1, chuẩn bị:**
  - Tiêu đề (bắt buộc, tối đa 100 ký tự) và mô tả (tuỳ chọn, tối đa 5.000 ký tự). `#thẻ` và `@handle` trong mô tả bấm được như ở trang xem (dùng lại code có sẵn).
  - Chọn nguồn phát:
    - **Webcam:** xem trước camera ở ngay trang, chọn camera / micro (dùng lại `media-device-settings`), bật / tắt micro và camera.
    - **Phần mềm phát (OBS):** chọn giao thức **WHIP** (mặc định, không tốn phút transcode) hoặc **RTMP** (kèm cảnh báo hạn mức, xem mục 3.5). Hiện **URL máy chủ** và **Khoá luồng** / mã (nút Chép, khoá ẩn bằng dấu chấm và có nút hiện). Hướng dẫn ngắn cho OBS. Trạng thái "Đang chờ tín hiệu từ phần mềm phát…" cho tới khi LiveKit nhận được hình.
  - Bật / tắt chat (mặc định bật).
- **Bước 2, đang phát:**
  - Bên trái: hình đang phát (xem trước của chính mình), tiêu đề, số người đang xem, thời gian đã phát, nút **Kết thúc buổi phát** (có hộp thoại xác nhận).
  - Bên phải: khung chat (giống người xem, thêm nút xoá tin) và thẻ **Poll** (tạo / đóng Poll).
  - Nút **Chép link** tới trang xem.
- **Khi nào tính là "đang phát":** khi LiveKit có người phát với luồng hình trong phòng. Lúc đó buổi phát chuyển sang LIVE, ghi giờ bắt đầu và gửi thông báo.

### 2.2 Xem – `/live/{id}`
- Bố cục giống trang xem video:
  - Bên trái: khung hình (tỉ lệ 16:9, nền đen), nhãn đỏ **TRỰC TIẾP**, số người đang xem. Dưới là tiêu đề, hàng kênh (ảnh, tên, số người đăng ký, nút Đăng ký + chuông dùng chung `subscribeControl.tsx`), mô tả thu gọn, nút **Chia sẻ** (dùng `share-dialog.tsx` với link `/live/{id}`, không có "Bắt đầu tại").
  - Bên phải: **Chat trực tiếp**, Poll đang mở ghim ở đầu khung chat.
  - Điện thoại: chat nằm dưới phần thông tin, cao cố định, cuộn trong khung.
- **Trình phát** (mới, nhỏ gọn, không dùng lại trình phát video vì không có tua / tốc độ / chương / phụ đề):
  - Phát / tạm dừng, âm lượng + tắt tiếng, toàn màn hình, PiP.
  - Trình duyệt chặn tự phát có tiếng: phát không tiếng và hiện nút "Bật tiếng" (giống Shorts và phòng xem chung).
  - Mất tín hiệu từ người phát: hiện "Người phát đang gặp sự cố kết nối…" thay vì màn hình đen.
- **Khách chưa đăng nhập:** xem được và đọc chat. Ô chat thay bằng "Đăng nhập để trò chuyện". Bấm chọn Poll cũng mời đăng nhập.
- **Buổi chưa bắt đầu** (chủ kênh đã tạo nhưng chưa có tín hiệu): "Buổi phát sắp bắt đầu", tự chuyển sang hình khi có tín hiệu.
- **Buổi đã kết thúc:** "Buổi phát trực tiếp đã kết thúc", kèm ngày phát, thời lượng, số người xem cao nhất, kết quả các Poll. Không có video xem lại (xem câu hỏi 1).
- id sai hoặc buổi phát chưa từng lên sóng đã bị huỷ: trang 404 kiểu site.

### 2.3 Chat
- Tin dài tối đa 200 ký tự (YouTube cũng 200). Giữ 100 tin gần nhất cho người vào sau.
- Tin hiện ảnh + tên kênh của người gửi. Tin của chủ kênh có nhãn tên nền màu (như YouTube).
- `@handle` trong tin được tô màu (dùng lại code có sẵn).
- Giới hạn: 5 tin / 10 giây / người (`RateLimiter`). Vượt thì báo riêng người đó bằng tiếng Việt.
- Chủ kênh xoá được bất kỳ tin nào. Tin bị xoá biến mất ở mọi máy.
- Chủ kênh tắt chat thì mọi người thấy "Chat đã bị tắt", tin cũ ẩn đi.
- Chat không lưu lâu dài: buổi phát kết thúc thì chat bị xoá (YouTube giữ chat cho bản xem lại; ta chưa có bản xem lại).

### 2.4 Poll
- Chủ kênh tạo Poll: câu hỏi (tối đa 150 ký tự) và 2–4 lựa chọn (mỗi lựa chọn tối đa 60 ký tự).
- **Mỗi lúc chỉ một Poll đang mở.** Muốn mở Poll mới thì đóng Poll cũ trước.
- Người xem đã đăng nhập chọn **một lần**, không đổi được (giống Poll của bài đăng cộng đồng).
- Kết quả (phần trăm + số phiếu) hiện cho mọi người, kể cả trước khi chọn. Cập nhật trực tiếp, gộp tối đa 1 lần / giây.
- Chủ kênh **Đóng Poll**: Poll khoá lại, kết quả cuối hiện thêm 1 phút trong khung chat rồi ẩn. Buổi phát kết thúc thì Poll đang mở tự đóng.
- Poll lưu vào MySQL theo buổi phát, để trang "đã kết thúc" hiện lại kết quả.

### 2.5 Tìm thấy buổi phát
- **`/live`** (thay `/livestream`): lưới thẻ các buổi đang phát, nhiều người xem trước. Không có ai phát: "Hiện chưa có ai phát trực tiếp".
- Mục **Trực tiếp** ở thanh bên trái trỏ tới `/live`. `/livestream` và các đường dẫn con chuyển hướng về `/live`.
- **Thẻ buổi phát:** ảnh bìa của kênh (banner, không có thì nền tối với ảnh đại diện kênh ở giữa), nhãn đỏ **TRỰC TIẾP**, "N người đang xem", tiêu đề, tên kênh. Không có ảnh chụp từ luồng phát (cần LiveKit Egress, xem mục 2.8).
- **Trang chủ:** chip **Trực tiếp** sau chip "Kênh đăng ký", chỉ hiện khi có buổi đang phát. Bấm chip thì lưới là các buổi đang phát.
- **Trang kênh:**
  - Ảnh đại diện kênh có viền đỏ + nhãn **TRỰC TIẾP** khi kênh đang phát, bấm vào là tới buổi phát.
  - Tab mới **Trực tiếp** (giữa Shorts và Danh sách phát): buổi đang phát (nếu có) ở đầu, sau đó các buổi đã phát, mới nhất trước. Kênh chưa phát lần nào thì không có tab.

### 2.6 Thông báo
- Loại mới `LIVE_STARTED`: "{kênh} đang phát trực tiếp: {tiêu đề}", bấm vào là tới `/live/{id}`.
- Gửi một lần cho mỗi buổi, lúc buổi chuyển sang LIVE. Người nhận: người đăng ký với chuông **Tất cả** hoặc **Cá nhân hoá** (như video thường).
- Chuông vẫn hỏi server mỗi 60 giây như hiện tại. Không làm thông báo đẩy của trình duyệt (Web Push), xem mục 2.8.

### 2.7 Kết thúc buổi phát
- Chủ kênh bấm **Kết thúc buổi phát**.
- Quản trị viên kết thúc được mọi buổi (để xử lý nội dung xấu), từ nút trên trang xem khi đăng nhập bằng tài khoản admin.
- **Tự kết thúc:**
  - Đang LIVE mà mất người phát quá **2 phút** (OBS tắt, mất mạng, đóng tab).
  - Đã tạo mà không lên sóng sau **1 giờ**: huỷ luôn, không vào lịch sử.
  - Dài quá **12 giờ**.
- Kết thúc thì: xoá phòng LiveKit (mọi người xem bị ngắt và thấy màn hình "đã kết thúc"), xoá điểm nhận OBS, đóng Poll, xoá chat, ghi giờ kết thúc.

### 2.8 Không làm trong L1
- **Bản xem lại (ghi hình)**, xem câu hỏi 1.
- Ảnh chụp từ luồng phát cho thẻ buổi phát (cũng cần Egress).
- Đặt lịch phát trước ("Sắp phát", nhắc tôi).
- Mời người xem lên phát cùng ("sân khấu", "giơ tay"), xem câu hỏi 3.
- Thông báo đẩy của trình duyệt (Web Push).
- Khoá luồng cố định cho mỗi kênh: L1 sinh khoá mới cho mỗi buổi (xem mục 3.2).
- Chặn người xem khỏi chat, chế độ chậm, chỉ người đăng ký được chat.
- Siêu tin nhắn (Super Chat), quà, hội viên trong chat (M1).
- Tìm kiếm buổi phát qua ô tìm kiếm, gợi ý buổi phát trong lưới trang chủ ngoài chip "Trực tiếp".
- Báo cáo buổi phát (người xem báo cáo). Admin vẫn kết thúc được (mục 2.7).
- Xem chung một buổi phát trong phòng xem chung.

## 3. Cách làm

### 3.1 Dữ liệu (MySQL)
- **Bảng `live_streams`:**
  - `id` (UUID), `channel_id`, `owner_id`, `title`, `description`, `source` (`BROWSER` | `ENCODER`), `status` (`CREATED` | `LIVE` | `ENDED` | `CANCELED`, lưu dạng VARCHAR, không dùng ENUM của MySQL, tránh bẫy ENUM ở CLAUDE.md), `chat_enabled`.
  - `livekit_room` (tên phòng = `live-{id}`), `ingress_id` (null với webcam).
  - `created_at`, `started_at`, `ended_at`, `last_publisher_at` (lần cuối thấy người phát), `peak_viewers`.
  - Index `(status)`, `(channel_id, status)`.
- **Bảng `live_polls`:** `id`, `stream_id`, `question`, `options` (JSON: mảng chuỗi), `status` (`OPEN` | `CLOSED`), `created_at`, `closed_at`.
- **Bảng `live_poll_votes`:** khoá chính `(poll_id, user_id)` (chặn bỏ phiếu hai lần ở mức DB), `option_index`, `created_at`. Index `(poll_id, option_index)` để đếm.
- Tạo bằng runner idempotent theo mẫu `CommentSchemaInitializer` (CLAUDE.md: Hibernate `ddl-auto=update` không tạo index).
- **Xoá** `entity/Room.java` và `repository/livekit/RoomRepository.java` (không ai dùng; bảng `rooms` cũ trên MySQL để nguyên, không xoá dữ liệu).

### 3.2 LiveKit (backend)
- **Danh tính luôn lấy từ JWT đăng nhập** (`CurrentUser`), không bao giờ từ nội dung request. Identity trong LiveKit:
  - Người đăng nhập: `u:{userId}`, kèm tên và ảnh kênh trong metadata của token. Backend ký nên không giả được.
  - Khách: `g:{chuỗi ngẫu nhiên}`.
- **Token** (do backend ký, hạn 6 giờ):
  - Chủ kênh (webcam): `canPublish` (camera + micro), `canSubscribe`.
  - Người xem và khách: chỉ `canSubscribe`. Không ai có `canPublishData`, vì chat và Poll đi qua backend.
- **Webcam:** trình duyệt chủ kênh vào phòng LiveKit bằng token phát và đăng luồng camera + micro.
- **Phần mềm phát:**
  - Lúc tạo buổi phát, backend tạo một Ingress RTMP gắn với phòng `live-{id}`, identity `u:{ownerId}`, và trả URL + khoá luồng **chỉ cho chủ kênh**.
  - Kết thúc buổi thì xoá Ingress.
  - Ngoài ra, lần tạo buổi phát mới nào cũng xoá Ingress cũ không dùng của chính kênh đó.
  - Mã hoá: giữ preset hiện tại `H264_1080P_30FPS_3_LAYERS` (3 lớp để người xem mạng yếu nhận lớp thấp).
- **Theo dõi trạng thái:** job mỗi **15 giây** hỏi LiveKit (`listParticipants`) cho mỗi buổi CREATED / LIVE. Job này:
  - Chuyển CREATED → LIVE khi thấy người phát có luồng hình, rồi gửi thông báo.
  - Cập nhật số người xem (số người trong phòng trừ người phát) và `peak_viewers`.
  - Tự kết thúc theo mục 2.7.
  - Số người xem đẩy tới người xem qua WebSocket (mục 3.3), không cần hỏi lại.
  - Webcam: client cũng gọi `POST /api/live/{id}/started` ngay khi đăng luồng xong, để LIVE không phải chờ tới 15 giây. Backend vẫn kiểm với LiveKit trước khi nhận.
  - *Em chọn job hỏi định kỳ thay vì webhook của LiveKit:* webhook nhanh hơn nhưng cần anh cấu hình trong trang quản trị LiveKit và thêm một API công khai có kiểm chữ ký. Job đủ dùng với số buổi phát nhỏ.
- **Xoá** `LivekitController`, `LivekitIngressService` và `LivekitRoomService` cũ cùng các DTO `dto/*/livekit/*` không dùng nữa. `LiveKitJob` (dọn Ingress lúc 0h) giữ lại, chuyển vào service mới.

### 3.3 API
REST, tiền tố `/api/live`:

| Phương thức | Đường dẫn | Ai | Trả về |
|---|---|---|---|
| POST | `/api/live` `{title, description, source, chatEnabled}` | chủ kênh | buổi mới (status CREATED) + `ingress {url, streamKey}` nếu ENCODER. 409 nếu kênh đã có buổi chưa kết thúc (kèm id buổi đó). 403 nếu kênh bị ẩn |
| GET | `/api/live/me` | chủ kênh | buổi chưa kết thúc của kênh mình (kèm ingress), hoặc 204 |
| PATCH | `/api/live/{id}` `{title, description, chatEnabled}` | chủ kênh | sửa khi chưa kết thúc |
| POST | `/api/live/{id}/started` | chủ kênh | báo đã đăng luồng webcam (mục 3.2) |
| POST | `/api/live/{id}/end` | chủ kênh, admin | kết thúc |
| GET | `/api/live/{id}` | công khai | thông tin buổi (kênh, trạng thái, số người xem, thời gian, Poll) |
| POST | `/api/live/{id}/token` | công khai | token LiveKit + `wsUrl`. Chủ kênh webcam nhận token phát, còn lại token xem. 410 nếu đã kết thúc |
| GET | `/api/live/{id}/chat` | công khai | 100 tin chat gần nhất (rỗng nếu chat tắt hoặc đã kết thúc) |
| GET | `/api/live` | công khai | các buổi đang LIVE (trang `/live`, chip Trực tiếp) |
| GET | `/api/live/channel/{channelId}` | công khai | buổi đang phát + buổi đã phát của kênh (tab Trực tiếp), phân trang |
| POST | `/api/live/{id}/polls` `{question, options}` | chủ kênh | mở Poll |
| POST | `/api/live/{id}/polls/{pollId}/close` | chủ kênh | đóng Poll |
| POST | `/api/live/{id}/polls/{pollId}/vote` `{option}` | đăng nhập | 409 nếu đã chọn |

- `SecurityConfig`: thêm GET `/api/live`, `/api/live/**` (gồm `/chat`) và POST `/api/live/*/token` vào nhóm công khai. Còn lại cần đăng nhập. Xoá các dòng `/api/livekit/**`.
- Giới hạn tần suất (`RateLimiter`):
  - Tạo buổi phát: 10 lần / giờ / người.
  - Lấy token: 30 lần / phút / IP.
  - Bỏ phiếu: 10 lần / phút / người.
- **WebSocket** (STOMP sẵn có, `/ws`):
  - Một topic mỗi buổi: `/topic/live/{id}`. Tin có `type`: `CHAT`, `CHAT_DELETED`, `CHAT_STATE` (bật / tắt), `VIEWERS`, `POLL` (mở / cập nhật kết quả / đóng), `STATUS` (LIVE / ENDED).
  - Gửi lên: `/app/live/{id}/chat` `{text}` và `/app/live/{id}/chat/delete` `{messageId}` (chủ kênh).
  - Vào buổi: client đăng ký nghe topic **trước**, rồi gọi REST `GET /api/live/{id}/chat` (công khai) lấy 100 tin gần nhất. Tin trùng (nhận cả hai đường) bỏ theo `id` của tin. Poll đang mở và số người xem nằm sẵn trong `GET /api/live/{id}`. Cùng một đường cho cả khách lẫn người đăng nhập.
  - **Khách đọc chat:** hiện tại `WebSocketStompConfig` từ chối mọi kết nối chưa đăng nhập (luật của phòng xem chung W1). Sửa thành:
    - Cho kết nối không đăng nhập.
    - Khách chỉ được **đăng ký nghe `/topic/live/*`**.
    - Mọi SEND, và mọi đăng ký topic khác (phòng xem chung), vẫn bắt đăng nhập như cũ.
- **Chat lưu ở đâu:** bộ nhớ + Redis giống phòng xem chung (`live:chat:{id}`, 100 tin, TTL 13 giờ). Redis lỗi thì chỉ bộ nhớ (`RedisService` có sẵn).

### 3.4 Client (`WeTube-client-app-dev`)
- **Trang mới:**
  - `app/studio/live/page.tsx`: phát (mục 2.1).
  - `app/(home)/live/page.tsx`: danh sách (mục 2.5).
  - `app/live/[id]/page.tsx`: xem (mục 2.2). Server component có `generateMetadata` (tiêu đề, ảnh kênh) để chia sẻ link có thẻ xem trước, giống `watch/[videoId]`.
- **RTK Query** `app/api/liveApi.ts`, đăng ký trong `store/store.ts`. `getLive` đặt `keepUnusedDataFor: 0` (bẫy cache ở CLAUDE.md).
- **LiveKit phía trình duyệt:** giữ `livekit-client` và `@livekit/components-react` (đã có). Token lấy qua API mỗi lần vào trang, không bao giờ để trên URL.
- **Giao diện:** shadcn + Tailwind như phần còn lại của site, toast qua `lib/toastEmitter.ts`, lỗi API qua `lib/apiError.ts`. Không dùng Radix Themes nữa.
- **Xoá code cũ:**
  - `app/livestream/*` (thay bằng chuyển hướng về `/live`).
  - `app/api/streamApi/*` và `lib/controller.ts`.
  - `modules/stream/*` (trừ `media-device-settings.tsx` nếu dùng lại được).
  - Gói `livekit-server-sdk`, `jsonwebtoken`, `js-confetti`, `@radix-ui/themes` nếu không còn chỗ nào dùng (em sẽ kiểm trước khi gỡ).
- **Biến môi trường client:** sau khi xoá `lib/controller.ts`, client không cần `LIVEKIT_API_SECRET`, `LIVEKIT_API_KEY`, `LIVEKIT_wsUrl` nữa. Em đề nghị anh xoá chúng trên Vibe Host. Địa chỉ LiveKit cho trình duyệt lấy từ API token, không cần biến client.

### 3.5 Hiệu năng và hạn mức gói free của LiveKit
- Hình và tiếng đi thẳng giữa trình duyệt / OBS và máy chủ LiveKit, không qua backend Vibe Host (1 CPU, 1,5 GB). Backend chỉ ký token, giữ chat và hỏi LiveKit mỗi 15 giây.
- **Hạn mức gói Build (free) của LiveKit Cloud**, em đọc ở [docs.livekit.io](https://docs.livekit.io/deploy/admin/quotas-and-limits/) ngày 27/10 (anh nên đối chiếu lại ở trang Settings của dự án LiveKit, vì em chưa đăng nhập được vào đó):

  | Hạn mức | Giá trị | Ý nghĩa với WeTube |
  |---|---|---|
  | Phút người tham gia WebRTC | **5.000 / tháng**, cứng | Mỗi người xem tính theo phút ở trong phòng, người phát cũng tính. Ví dụ: 1 buổi 1 giờ với 20 người xem ≈ 1.260 phút, nên khoảng 4 buổi như vậy là hết tháng |
  | Dữ liệu tải xuống | **50 GB / tháng**, cứng | Một người xem 720p khoảng 1–2 GB mỗi giờ, nên 50 GB đủ cho khoảng 25–50 giờ-người |
  | Người tham gia cùng lúc | **100** (mọi phòng cộng lại) | Trần cứng cho số người xem |
  | Phút transcode (Ingress RTMP) | **60 / tháng** | Phát qua OBS bằng RTMP **chỉ được khoảng 1 giờ mỗi tháng** |
  | Ingress cùng lúc | **2** | Tối đa 2 kênh phát bằng OBS cùng lúc |

  Quá hạn mức thì yêu cầu mới **thất bại** (không tính thêm tiền).
- **Cách thiết kế xử lý:**
  - **Webcam** chỉ tốn phút WebRTC, không tốn phút transcode. Đây là đường phát chính của L1.
  - **OBS: mặc định dùng WHIP** (không transcode, không tốn 60 phút), RTMP chỉ là lựa chọn phụ, kèm cảnh báo "RTMP chỉ còn dùng được khoảng 1 giờ/tháng trên gói miễn phí". Cần OBS bản 30 trở lên cho WHIP. Backend hiện luôn tạo RTMP và bật transcode; em sẽ cho tạo WHIP với `enableTranscoding = false`. *Em chưa kiểm được điều này với server thật; sẽ thử trong gói 1 bằng một buổi phát OBS thật, và nếu LiveKit vẫn tính phút transcode cho WHIP thì báo anh.*
  - **Trần số người xem cho mỗi buổi:** 40 người vào bằng WebRTC (em chọn con số này để hai buổi phát cùng lúc vẫn nằm dưới trần 100). Người thứ 41 trở đi thấy "Buổi phát đang đầy người xem" thay vì bị lỗi khó hiểu. Chat và Poll vẫn dùng được cho người xem ngoài trần vì đi qua backend, nhưng họ không có hình.
  - **Hết hạn mức tháng:** khi cấp token hoặc tạo Ingress thất bại vì quota, backend trả lỗi rõ ràng, chủ kênh thấy "Đã hết hạn mức phát trực tiếp của tháng này, sẽ có lại vào ngày 1", người xem thấy "Buổi phát tạm thời không xem được". Không để lỗi 500 trống.
  - **Tiết kiệm phút xem:** trang xem ngắt kết nối WebRTC khi tab bị ẩn quá 2 phút và nối lại khi quay lại (người đó vẫn thấy chat). Em đoán điều này tiết kiệm đáng kể vì nhiều người để tab chạy nền. *Đây là đề xuất của em.*
  - Số người đang xem của mỗi buổi và phút đã dùng ước tính được ghi vào log backend mỗi lần job 15 giây chạy, để anh nhìn ra mức sử dụng; không làm trang thống kê riêng.
- Nếu sau này cần nhiều hơn, nâng lên gói trả tiền của LiveKit là đủ, không phải đổi code.

## 4. Chia gói
Mỗi gói một commit. Gói 0 deploy riêng ngay nếu anh đồng ý. Các gói 1–5 chỉ deploy khi anh duyệt, backend trước, client sau.

0. **Sửa lỗ hổng ngay** (bản sửa lỗi riêng, tách khỏi L1): xoá `app/api/streamApi/*` và `lib/controller.ts` của client. Sau đó trang `/livestream` cũ vẫn hỏng như hiện tại. Kiểm tra: `tsc`, `next build`, curl `/api/streamApi/create_stream` trả 404 sau deploy.
1. **Backend: buổi phát.** Bảng, `LiveStreamService`, token, Ingress, job 15 giây, thông báo `LIVE_STARTED`, REST, `SecurityConfig`, xoá code LiveKit cũ. Test Mockito:
   - Quyền: người khác không sửa / kết thúc được, admin kết thúc được.
   - Mỗi kênh một buổi, kênh ẩn bị chặn.
   - Chuyển trạng thái và tự kết thúc theo thời gian.
   - Thông báo gửi đúng một lần, đúng chế độ chuông.
   - Token xem không có quyền phát.
2. **Backend: chat + Poll.** STOMP, khách chỉ nghe `/topic/live/*`, Redis, giới hạn tần suất, Poll. Test:
   - Bỏ phiếu hai lần → 409.
   - Chỉ một Poll mở.
   - Khách không SEND được, không nghe được topic phòng xem chung.
   - Tin quá 200 ký tự bị từ chối.
3. **Client: phát + xem.** `/studio/live`, `/live/[id]`, trình phát, `liveApi`, nút "Tạo" → Phát trực tiếp, Studio thanh bên.
4. **Client: chat, Poll, tìm thấy.** Chat và Poll ở cả hai trang, `/live`, chip Trực tiếp, trang kênh (viền đỏ + tab), thông báo `LIVE_STARTED` ở chuông, chuyển hướng `/livestream`, xoá code cũ và gói thừa.
5. **Kiểm tra và ghi chép.**
   - Backend: `compileJava compileTestJava` + cả bộ test.
   - Client: `tsc` (không thêm lỗi mới ngoài 12 lỗi TS7006 cũ), `eslint`, `next build`.
   - Sau deploy: curl API (401 / 404 / 410 đúng chỗ), log không có `Exception`.
   - Thử bằng Chrome với camera giả (`--use-fake-device-for-media-stream`) nếu có Claude in Chrome. Không có thì anh thử theo mục mới "Phát trực tiếp" trong `TEST_CHECKLIST.md`.
   - Đo độ trễ webcam → người xem và ghi số thật vào `YOUTUBE_PARITY.md`.

Cỡ ước tính: khoảng 3–4 ngày làm.

## 5. Anh đã trả lời (27/10)
1. **Lịch sử buổi phát:** chọn **(a)**, danh sách buổi đã phát kèm số liệu và kết quả Poll, không ghi hình. Bản xem lại để sau.
2. **Máy chủ LiveKit:** LiveKit Cloud, **gói free (Build)** theo trí nhớ của anh. Em chưa xác nhận được trong trang quản trị LiveKit; hạn mức ở mục 3.5 lấy từ tài liệu công khai.
3. **Mời lên sân khấu / giơ tay:** bỏ khỏi L1.
4. **Lỗ hổng `/api/streamApi/*`:** sửa ngay (gói 0, tách commit riêng).

Anh hỏi thêm: PRD có nhắc LiveKit nhiều lần (độ trễ dưới 1 giây, "Media & Streaming Engine", luồng 3), nên L1 giữ LiveKit. Nếu hạn mức free quá chật thì phương án thay thế là RTMP + HLS (độ trễ 3–10 giây, bỏ chỉ tiêu dưới 1 giây của PRD) và phải viết lại mục 3.2.
