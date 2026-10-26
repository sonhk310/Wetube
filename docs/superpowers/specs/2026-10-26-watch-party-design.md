# Thiết kế: phòng xem chung làm lại (W1)

Ngày: 2026-10-26. Trạng thái: **chờ anh duyệt**.

## 1. Vì sao
PRD luồng 2 và mục 4.1.3 yêu cầu:
1. Người dùng mở video, bấm **"Tạo phòng xem chung"**.
2. Hệ thống sinh mã phòng và **link mời**.
3. Bạn bè bấm link là vào phòng ngay.
4. **Bất kỳ ai** bấm Phát / Dừng / Tua thì mọi người đồng bộ ngay qua WebSocket.
5. Phòng có chat nhóm.

**Code hiện tại (em đọc 26/10):**
- Không có link mời: phải mở hộp thoại ở thanh bên rồi gõ mã. Trang `/rooms` không có mã trên URL, nên tải lại trang là mất phòng.
- Chỉ chủ phòng gửi được trạng thái phát, người khác bấm Dừng thì chỉ dừng ở máy mình.
- Tên tự gõ, không gắn tài khoản: hai người gõ cùng tên thì lẫn vào nhau, ai cũng mạo danh được.
- Phòng chỉ nằm trong bộ nhớ backend, không bao giờ bị xoá, mất sạch mỗi lần deploy.
- Dùng thẻ `<video controls>` của trình duyệt, không phải trình phát mới (phụ đề, chương, phím tắt...).
- Lỗi, em đã xác nhận trong code:
  - "Đã tạo phòng" phát lên `/topic/room/create` chung, nên mọi người đang mở hộp thoại Tạo phòng cùng lúc đều nhận phòng của người khác.
  - `GET /api/rooms` trả **mọi phòng kèm mã** cho bất kỳ ai đã đăng nhập, nên ai cũng vào được phòng riêng tư của người khác.
  - `useVideoSync.ts` mở một kết nối thứ hai tới topic không ai gửi (`/topic/rooms.video.*`): code chết.

**Thành công nghĩa là:**
- Từ trang xem, bấm "Tạo phòng xem chung" thì vào `/rooms/{mã}`, video đang xem phát tiếp đúng giây đó, và có hộp thoại chép link mời.
- Người thứ hai mở link (đã đăng nhập) thì vào phòng với tên và ảnh tài khoản, video ở cùng giây với người kia, lệch dưới khoảng 0,5 giây.
- Một trong hai bấm Dừng / Phát / tua / đổi video thì máy kia làm theo trong khoảng một phần mười đến vài phần mười giây.
- Tải lại trang hoặc backend deploy lại thì vẫn ở trong phòng.
- Mọi người rời phòng thì phòng tự xoá.

## 2. Phạm vi

### 2.1 Vào phòng
- **Tạo phòng:**
  - Nút "Xem chung" (biểu tượng người + dấu cộng) cạnh Chia sẻ / Lưu ở trang xem. Bấm thì tạo phòng với video và giây đang xem, rồi chuyển sang `/rooms/{mã}`.
  - Thanh bên giữ mục "Phòng xem chung": hộp thoại có hai lựa chọn, **Tạo phòng trống** và **Nhập mã hoặc dán link** để vào phòng.
- **Link mời:** `https://wetube.vibe.tinhgon.xyz/rooms/{mã}`. Mã giữ kiểu hiện tại `abc-defg-hij` (10 ký tự chữ + số, khoảng 8·10^17 tổ hợp, không đoán được). Vừa tạo phòng xong thì hiện hộp thoại "Mời bạn bè" có nút Chép link. Trong phòng luôn có nút "Mời" để mở lại hộp thoại này.
- **Phải đăng nhập.** Khách mở link thấy trang "Đăng nhập để vào phòng xem chung", đăng nhập xong thì quay lại đúng link.
- Mã sai, hoặc phòng đã bị xoá: trang "Phòng không tồn tại hoặc đã đóng" và nút Về trang chủ.
- Một phòng tối đa **20 người**. Phòng đầy thì báo "Phòng đã đủ 20 người".

### 2.2 Trong phòng `/rooms/{mã}`
- **Bố cục giống trang xem:**
  - Bên trái: trình phát, dưới là tên video, kênh, và hàng "Đang xem: ảnh của từng người" (rê chuột thấy tên; chủ phòng có nhãn "Chủ phòng").
  - Bên phải: khung **Chat** ở trên, **Hàng chờ** ở dưới.
  - Điện thoại: chat và hàng chờ thành hai tab dưới trình phát.
- **Trình phát:**
  - Dùng lại trình phát của trang xem (`modules/watch/components/video/`) ở chế độ phòng: phụ đề, chương, âm lượng, toàn màn hình, PiP, phím tắt vẫn chạy.
  - Tắt ở chế độ phòng: màn hình hết video / tự phát video gợi ý, khung danh sách phát, Shift+N/P và tốc độ phát. Tốc độ khác nhau thì lệch nhau, nên trong phòng luôn là 1x.
  - Video vẫn được tính lượt xem và lịch sử xem như xem bình thường.
- **Ai cũng điều khiển:** Phát, Dừng, tua, bấm chương, bấm mốc thời gian trong chat, đổi video. Mỗi thao tác hiện một dòng nhỏ trong chat, ví dụ "Sơn đã tua tới 3:20", "Lan đã dừng video", để biết ai làm gì.
- **Hàng chờ:**
  - Ô tìm kiếm video (dùng API tìm kiếm có sẵn) hoặc dán link `/watch/...`, bấm để thêm vào cuối hàng chờ.
  - Ai cũng thêm được, cũng xoá được và bấm "Phát ngay" cho một video trong hàng chờ.
  - Hết video thì tự sang video đầu hàng chờ; hàng chờ trống thì dừng ở cuối.
  - Chỉ thêm được video Công khai hoặc Không công khai (giữ luật hiện tại).
- **Chat:**
  - Tin dài tối đa 500 ký tự. Phòng giữ 100 tin gần nhất, người mới vào thấy lại các tin đó.
  - Mốc thời gian như `3:20` trong tin bấm được để tua (dùng lại code link mốc thời gian ở bình luận).
  - Có dòng hệ thống "Lan đã vào phòng", "Lan đã rời phòng".
- **Thả cảm xúc:** hàng nút 👍 ❤️ 😂 😮 😢 🔥 dưới trình phát. Bấm thì biểu tượng bay lên trên trình phát của mọi người (không lưu, tối đa 1 lần / 0,5 giây / người).
- **Chủ phòng:**
  - Là người tạo phòng. Chủ phòng rời đi thì người vào sớm nhất còn lại thành chủ phòng.
  - Chỉ chủ phòng **mời ra** được một người (người đó không vào lại được phòng này) và **đóng phòng** (mọi người thấy "Chủ phòng đã đóng phòng").
- **Rời phòng:** nút "Rời phòng" (về trang xem của video đang phát), hoặc đóng tab.

### 2.3 Không làm trong W1
- Gọi thoại hoặc gọi video trong phòng.
- Phòng lưu lâu dài, đặt lịch xem.
- Xem chung buổi livestream (thuộc L1).
- Khoá "chỉ chủ phòng điều khiển". Có thể thêm sau nếu anh muốn.

## 3. Cách làm

### 3.1 Đồng bộ phát (phần quan trọng nhất)
- **Backend giữ trạng thái gốc:** `{videoId, playing, position (giây), updatedAt (giờ server, ms), version}`.
  - Mỗi thao tác Phát / Dừng / tua / đổi video gửi lên `version` mà máy đó đang thấy.
  - Backend chỉ nhận nếu `version` khớp, rồi tăng `version` và phát trạng thái mới cho cả phòng.
  - Hai người bấm cùng lúc thì người sau bị bỏ và nhận trạng thái mới nhất. Không có cảnh hai máy giật qua lại.
- **Mỗi máy tự tính vị trí đúng:** `position + (giờ hiện tại − updatedAt)` nếu đang phát.
  - Giờ máy và giờ server có thể lệch, nên lúc vào phòng và mỗi 30 giây máy hỏi giờ server qua WebSocket để tính độ lệch, giống NTP đơn giản.
  - Đây là các hàm thuần trong `lib/roomSync.ts`, kiểm bằng `node` như `playlistOrder.ts`.
- **Sửa lệch:** mỗi giây so vị trí thật với vị trí đúng.
  - Lệch trên 0,5 giây: tua thẳng tới vị trí đúng.
  - Lệch 0,15–0,5 giây: tăng hoặc giảm tốc độ phát 5% cho đến khi khớp, không giật hình.
- **Không gửi ngược:** khi máy đang áp trạng thái nhận được (tua, phát, dừng do người khác), các sự kiện của thẻ video lúc đó không được gửi lên lại.
- **Đang tải (buffering):** máy đang tải thì không kéo cả phòng dừng theo. Tải xong thì tự tua tới vị trí đúng.
- **Hết video:** mọi máy đều báo "hết video" kèm `version`, nên chỉ lượt báo đầu tiên được nhận, và hàng chờ chỉ nhảy một video.
- **Tự phát bị chặn:** trình duyệt có thể chặn phát có tiếng khi chưa bấm gì trên trang. Khi đó phát không tiếng và hiện nút "Bật tiếng", giống trình phát Shorts.
- "Độ trễ mili-giây" trong PRD: thực tế qua WebSocket là khoảng 50–300 ms, tuỳ mạng. Em sẽ đo và ghi số thật vào `YOUTUBE_PARITY.md` sau khi deploy.

### 3.2 Backend (`WeTube-service-dev`)
- **Danh tính trên WebSocket:**
  - Thêm bộ chặn lúc bắt tay `/ws`: đọc cookie `AT` (cùng cookie với REST), giải JWT bằng `JwtDecoder` sẵn có, gắn người dùng vào phiên WebSocket. Không có hoặc sai thì từ chối kết nối.
  - Client `wetube.vibe.tinhgon.xyz` và backend `wetube-service.vibe.tinhgon.xyz` cùng "site", nên cookie `SameSite=Strict` vẫn được gửi.
  - Mọi tin nhắn lấy người gửi từ phiên đó, **bỏ trường `username` trong nội dung**.
  - Token hết hạn giữa chừng: kết nối đang mở vẫn chạy tới khi đóng. Lúc kết nối lại, client gọi làm mới phiên (`apiFetch` có sẵn) trước.
- **REST:**
  - `POST /api/rooms` `{videoId?, t?}` → `{code}`.
  - `GET /api/rooms/{code}` → ảnh chụp phòng (để trang biết phòng còn hay không trước khi kết nối).
  - Cả hai cần đăng nhập.
  - **Xoá** `GET /api/rooms` (đang lộ mã của mọi phòng).
- **STOMP:** chỉ một topic cho mỗi phòng, `/topic/rooms/{code}`. Tin có `type`: `STATE`, `MEMBERS`, `CHAT`, `SYSTEM`, `REACTION`, `QUEUE`, `CLOSED`, `KICKED`.
  - Client gửi lên `/app/rooms/{code}/join`, `/control`, `/chat`, `/reaction`, `/queue` (thêm / xoá / phát ngay), `/kick`, `/close`, `/time`.
  - Người mới vào nhận ảnh chụp đầy đủ (trạng thái, thành viên, hàng chờ, 100 tin chat) qua `/user/queue/rooms`, riêng cho người đó.
  - Thay toàn bộ các điểm `/app/room/...` cũ.
- **Thành viên theo phiên WebSocket:**
  - Một người mở 2 tab vẫn tính là 1 người. Đóng tab cuối thì người đó rời phòng (dùng `SessionDisconnectEvent`).
  - Mất mạng thì chờ 15 giây mới báo "đã rời phòng", để tải lại trang không làm chat đầy dòng vào / ra.
- **Lưu phòng trong Redis** (đề xuất, xem câu hỏi 2):
  - Mỗi phòng là một khoá JSON `room:{code}`, hạn 6 giờ, gia hạn mỗi khi có hoạt động.
  - Backend deploy lại thì client tự kết nối lại (`reconnectDelay` đã có), vào lại phòng và nhận trạng thái cũ.
  - Redis lỗi thì dùng bộ nhớ như bây giờ (cách dự phòng của `RedisService`).
  - PRD có model `WatchRoom` (ID, HostUserId, VideoId, CurrentTimestamp, PlaybackStatus, MembersList). Khoá Redis này chứa đúng các trường đó; không cần bảng MySQL.
- **Dọn phòng:** job chạy mỗi phút, xoá phòng không còn ai quá 5 phút. Phòng có người thì không xoá, nhưng tối đa 24 giờ.
- **Giới hạn tần suất** (`RateLimiter` có sẵn), vượt thì bỏ tin và báo riêng người đó:
  - Điều khiển: 20 lần / 10 giây.
  - Chat: 10 tin / 10 giây.
  - Cảm xúc: 2 lần / giây.
  - Tạo phòng: 10 phòng / giờ / người.
- Xoá code cũ không dùng: `ChatController` (rỗng), `dto/room/RoomMessage`, `UpcommingListUpdate`, `JoinRoomRequest`, `service/impl/RoomService.java` nếu không ai gọi. Em sẽ kiểm trước khi xoá. `entity/Room.java` thuộc livestream thì giữ.

### 3.3 Client (`WeTube-client-app-dev`)
- **Trang mới:** `app/rooms/[code]/page.tsx`.
  - `/rooms` không có mã: hiện hộp thoại Tạo hoặc Vào phòng.
  - `useRoomStore` giữ trạng thái theo mã trên URL, không còn giữ tên tự gõ.
- **Trình phát:** thêm vào `ActiveVideo` một tham số `room`, gồm trạng thái phòng và các hàm `onControl` / `onEnded`.
  - Không có `room` thì trang xem chạy y như cũ.
  - Có `room` thì trình phát gửi thao tác qua hàm đó thay vì tự xử lý, và tắt các phần ở mục 2.2.
- **Xoá code cũ:** `useVideoSync.ts`, `videoControls.tsx`, `videoUrlInput.tsx`, `roomModal.tsx` cũ (viết lại), và `videoPlayer.tsx` của phòng.
- **Dùng lại:** hàng "Đang xem", chat, hàng chờ viết lại theo kiểu trang xem, dùng lại `Avatar`, `formatCompact`, link mốc thời gian, hộp thoại Chia sẻ cho link mời.

### 3.4 Chia gói (mỗi gói commit riêng, deploy chung một lần khi xong)
1. Backend: danh tính WebSocket, REST, trạng thái phòng + `version`, thành viên, Redis, dọn phòng. Kiểm bằng `compileJava` và curl (401 khi chưa đăng nhập, 404 với mã sai).
2. `lib/roomSync.ts` + phép thử `node`: tính vị trí, độ lệch giờ, quyết định tua hay chỉnh tốc độ.
3. Client: trang `/rooms/[code]`, trình phát chế độ phòng, nút trên trang xem, hộp thoại mời.
4. Chat, hàng chờ, cảm xúc, chủ phòng (mời ra / đóng phòng).
5. Kiểm tra: tsc, eslint, `next build`. Sau deploy em tự thử bằng hai cửa sổ nếu có Claude in Chrome; không thì anh thử theo danh sách em ghi vào `TEST_CHECKLIST.md` (mục mới "Phòng xem chung").

Cỡ ước tính: khoảng 2–3 ngày làm.

## 4. Câu hỏi cho anh
1. **Khách chưa đăng nhập** có được vào phòng bằng link không? Em đề xuất **không**: PRD ghi phòng riêng tư, và bắt đăng nhập thì danh tính đáng tin. Nếu anh muốn cho khách vào, em sẽ cho khách xem và chat với tên "Khách 1, 2..." nhưng không điều khiển được.
2. **Lưu phòng ở Redis** (sống qua deploy, đề xuất) hay **chỉ bộ nhớ** (đơn giản hơn, deploy là mất phòng)?
3. **Tối đa 20 người / phòng** có hợp không?
4. Nút trên trang xem: em đặt tên **"Xem chung"**, cạnh Chia sẻ. Anh có muốn tên khác không?
