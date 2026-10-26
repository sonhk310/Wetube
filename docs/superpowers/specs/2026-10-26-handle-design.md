# Thiết kế: @handle cho kênh (H1)

Ngày: 2026-10-26. Trạng thái: **anh đã duyệt 26/10** (hiện tên kèm @handle xám nhỏ; `/@handle` chuyển hướng; đổi 1 lần / 14 ngày). **Sửa 26/10 sau khi anh thử:** giữ `/@handle` trên thanh địa chỉ (cách 2b: rewrite, không chuyển hướng), `@handle` trong văn bản có màu tím riêng. Đã làm ở máy, chờ anh duyệt push + deploy.

## 1. Vì sao
Mỗi người cần một "@tên" duy nhất, không có dấu cách, để:
- Gọi nhau trong bình luận và phần trả lời.
- Phân biệt người cùng tên trong chat phòng xem chung, và sau này trong chat livestream (L1).
- Có link kênh dễ nhớ: `wetube.vibe.tinhgon.xyz/@huynhkyson`.

**Code hiện tại (em đọc 26/10):**
- `AppUser` và `Channel` là quan hệ một-một. Kênh được tạo tự động lần đầu đăng nhập Google (`UserServiceImpl` gọi `initiateChannel`). Kênh chỉ có `name`, `picture`, tên trùng nhau được.
- Bình luận lấy tên và ảnh từ `AppUser` (`UserDto`: `id`, `name`, `picture`).
- Trả lời một phản hồi thì ô nhập tự điền `@Tên đầy đủ ` (`comment.tsx:264`). Có hai vấn đề:
  - Tên có dấu cách, nên "@Huỳnh Kỳ Sơn" không bấm được và không biết "@" kết thúc ở đâu.
  - Phản hồi luôn gắn vào bình luận gốc, nên thông báo `COMMENT_REPLY` gửi cho **người viết bình luận gốc**, không phải người mình đang trả lời. Người được trả lời không nhận được thông báo nào.
- Chat phòng xem chung hiện `user.getName()` (`WatchRoomService:152`). Hai người cùng tên trông giống hệt nhau.
- Kênh chỉ có URL `/channel/{uuid}`.
- Trang Studio → Tuỳ chỉnh (`app/studio/customization/page.tsx`) mới là khung tĩnh. Backend chưa có API sửa kênh (`updateChannel` đang bị comment).
- `LinkifiedText` (link + mốc thời gian) đang được dùng ở bình luận, chat phòng và mô tả video. Thêm `@handle` vào đó là cả ba chỗ đều có.

**Thành công nghĩa là:**
- Mọi kênh, cả kênh cũ, đều có handle sau lần deploy đầu tiên, ví dụ "Huỳnh Kỳ Sơn" thành `@huynhkyson`.
- Mở `/@huynhkyson` thì ra đúng trang kênh.
- Bình luận và phản hồi hiện `@handle`. Bấm "Phản hồi" một phản hồi thì ô nhập tự điền `@handle `.
- `@handle` trong bình luận, chat phòng và mô tả video bấm được. Người bị nhắc tên trong bình luận nhận thông báo.
- Đổi được handle ở Studio. Handle trùng hoặc sai quy tắc thì báo lỗi ngay khi đang gõ.

## 2. Phạm vi

### 2.1 Quy tắc handle
- Gồm 3–30 ký tự: chữ thường `a-z`, số `0-9`, `_`, `.`, `-`. Không được bắt đầu hoặc kết thúc bằng `.` hoặc `-`.
- Lưu chữ thường. Người dùng gõ hoa thì tự đổi sang thường, nên `@HuynhKySon` và `@huynhkyson` là một.
- Không trùng: có index UNIQUE ở DB. Hai người lấy cùng lúc thì người sau nhận lỗi 409 "Handle đã có người dùng".
- Hiển thị luôn có `@` ở trước. Trong DB không lưu `@`.

### 2.2 Tự sinh handle
- Lấy từ tên kênh: bỏ dấu tiếng Việt (`đ` thành `d`), bỏ ký tự không hợp lệ, viết thường, cắt còn 30 ký tự. Ví dụ "Huỳnh Kỳ Sơn" thành `huynhkyson`.
- Trùng thì thêm số: `huynhkyson1`, `huynhkyson2`… Tên rỗng hoặc ngắn dưới 3 ký tự sau khi lọc (ví dụ tên toàn chữ Hán) thì dùng `user` + 6 ký tự ngẫu nhiên.
- Sinh handle ở hai chỗ:
  - `initiateChannel`, khi tạo kênh mới.
  - Runner lúc khởi động (`ChannelHandleInitializer`). Runner thêm cột và index nếu chưa có, rồi sinh handle cho mọi kênh còn `handle IS NULL`. Chạy lại không làm gì thêm. Mỗi câu SQL chạy trong một transaction riêng (theo mẫu `CommentSchemaInitializer`).

### 2.3 Đổi handle (Studio → Tuỳ chỉnh)
- Thêm khung "Thông tin cơ bản" ở trang Tuỳ chỉnh, gồm ô handle có sẵn `@` ở trước và link xem trước `wetube.vibe.tinhgon.xyz/@...`.
- Khi đang gõ, hiện "✓ Dùng được" hoặc lý do lỗi (sai quy tắc / đã có người dùng). Gọi API kiểm tra sau khi ngừng gõ khoảng 400 ms.
- Giới hạn đổi: **1 lần mỗi 14 ngày**. Lần đổi đầu tiên sau khi được tự sinh thì không bị giới hạn. Hết lượt thì báo "Bạn có thể đổi handle lại sau ngày …".
  - Em chọn 1 lần thay vì "2 lần / 14 ngày" như YouTube vì chỉ cần một cột `handle_changed_at`, không phải lưu lịch sử.
- Handle cũ được giải phóng ngay: link `/@cũ` không còn dùng được, và người khác lấy được handle đó.
- Khung này chỉ gồm handle. Phần sửa tên kênh và mô tả không nằm trong gói này.

### 2.4 Hiển thị
| Chỗ | Hiện gì |
|---|---|
| Đầu trang kênh | Dưới tên kênh: `@handle · N người đăng ký · N video` |
| Bình luận / phản hồi | Xem câu hỏi 1 ở mục 4 |
| Ô "Phản hồi" khi trả lời một phản hồi | Tự điền `@handle ` thay cho `@Tên ` |
| Chat phòng xem chung | Rê chuột vào tên / ảnh thấy `@handle`. Trong danh sách người xem: tên kèm `@handle` |
| `LinkifiedText` (bình luận, chat phòng, mô tả video) | `@handle` có màu xanh, bấm mở trang kênh |

### 2.5 Nhắc tên trong bình luận
- Khi tạo bình luận hoặc phản hồi video, backend tìm các `@handle` có trong nội dung (tối đa 10 handle khác nhau mỗi bình luận), tra kênh, rồi gửi thông báo loại mới `COMMENT_MENTION`: "X đã nhắc đến bạn trong một bình luận".
- Không báo cho chính mình. Nếu người được nhắc cũng là người nhận `COMMENT_REPLY` hay `VIDEO_COMMENT` của bình luận đó thì chỉ gửi một thông báo. Handle không tồn tại thì bỏ qua.
- Sửa bình luận thì không gửi lại thông báo.
- Cột `notifications.type` là `VARCHAR(30)`, không phải ENUM, nên thêm giá trị mới không cần sửa schema. Chuông thông báo ở client thêm một `case`.
- Gõ `@` thì hiện gợi ý chọn người: **không làm** trong gói này (xem mục 5).

## 3. Kỹ thuật

### 3.1 Backend
- `Channel`: thêm `handle VARCHAR(30)` (UNIQUE) và `handle_changed_at DATETIME NULL`.
- `ChannelHandles` (lớp tiện ích): `normalize`, `validate`, `generate(name, exists)`, `extractMentions(text)`. Có unit test.
- API:
  | API | Ai gọi | Trả |
  |---|---|---|
  | `GET /api/channels/handle/{handle}` | công khai | `ChannelResponseDto`, hoặc 404 |
  | `GET /api/channels/handle-check?handle=` | cần đăng nhập | `{ available, reason }` |
  | `PUT /api/channels/me/handle` `{ handle }` | cần đăng nhập, chỉ cho kênh của mình | 200 `ChannelResponseDto`; 400 sai quy tắc; 409 trùng; 429 chưa đủ 14 ngày (kèm `message` tiếng Việt) |
- Thêm `handle` vào `ChannelResponseDto`, `UserDto` (lấy từ `user.channel.handle`, nên bình luận có luôn), `ChannelSearchDto` và thành viên phòng xem chung.
- `CommentServiceImpl.notifyAboutComment` thêm bước gửi thông báo nhắc tên. Bước này chạy trong `try` riêng như hiện tại, nên lỗi ở đây không làm hỏng việc tạo bình luận.

### 3.2 Client
- Route `/@handle`: thư mục bắt đầu bằng `@` trong App Router là parallel route, nên không tạo được `app/@[handle]` trực tiếp. Có hai cách, xem câu hỏi 2 ở mục 4.
- `LinkifiedText`: thêm token `@handle`. Điều kiện là trước `@` không được là chữ hay số, để `abc@gmail.com` không bị nhận nhầm.
- `comment.tsx`: lấy `handle` từ `apiComment.user.handle`, dùng cho chỗ hiển thị và `replyPrefix`.
- Trang Tuỳ chỉnh: khung handle nói ở 2.3.
- Đầu trang kênh, phòng xem chung, chuông thông báo: như mục 2.4 và 2.5.

### 3.3 Người không có kênh
Tài khoản admin do `AdminSeeder` tạo có thể chưa có kênh, vì kênh chỉ được tạo khi đăng nhập Google. Những tài khoản này không có handle: bình luận hiện tên như cũ, ô phản hồi không tự điền `@`. Em sẽ kiểm tra trên prod xem có tài khoản nào như vậy không.

## 4. Cần anh chọn
1. **Tên người bình luận hiện thế nào?**
   - (a) Như YouTube hiện nay: hiện `@handle` thay cho tên.
   - (b) **Em đề xuất:** hiện tên như cũ, thêm `@handle` màu xám nhỏ bên cạnh. Tên tiếng Việt có dấu dễ đọc hơn handle không dấu, mà vẫn phân biệt được người cùng tên.
2. **Link `/@handle`:**
   - (a) **Em đề xuất:** mở `/@handle` thì chuyển hướng sang `/channel/{uuid}`. Làm bằng `middleware.ts` hoặc một trang server gọi `redirect()`, khoảng 30 dòng. Thanh địa chỉ sẽ hiện link uuid, nhưng link chia sẻ vẫn là `/@handle`.
   - (b) Giữ `/@handle` trên thanh địa chỉ bằng rewrite. Cách này phải sửa khoảng 10 chỗ đang đọc `channelId` từ URL để chấp nhận cả handle. Đổi lại URL đẹp hơn, giống YouTube.
3. **Giới hạn đổi handle:** 1 lần / 14 ngày như ở 2.3, được không anh?

## 5. Không làm trong gói này (ghi lại để làm sau)
- Gợi ý chọn người khi gõ `@`.
- Tìm kênh theo `@handle` ở ô tìm kiếm.
- Giữ handle cũ thêm 14 ngày để link cũ không hỏng.
- Sửa tên và mô tả kênh ở Studio.
- Danh sách handle cấm (`admin`, `wetube`…).

## 6. Kiểm tra
1. Unit test `ChannelHandles`: bỏ dấu (gồm `đ`), cắt 30 ký tự, tên rỗng / toàn ký tự lạ, trùng thì thêm số, quy tắc hợp lệ, tách mention (bỏ qua email, trùng lặp, giới hạn 10) → `./gradlew test --tests *ChannelHandles*`
2. Backend compile → `./gradlew --no-daemon compileJava compileTestJava -q`
3. Client → `npx tsc --noEmit`, `npx eslint` các file sửa, `npx next build`
4. Sau deploy:
   - Log có dòng "Đã sinh handle cho N kênh", không có `Exception`.
   - Curl `GET /api/channels/handle/{một handle thật}` trả 200; `PUT /api/channels/me/handle` khi chưa đăng nhập trả 401; `/@handle` trả 307 hoặc 200 tuỳ cách chọn ở câu hỏi 2.
5. Anh thử trên trình duyệt:
   - Đổi handle (trùng, sai quy tắc, đổi lần hai trong 14 ngày).
   - Bình luận có `@handle` của người khác, người đó nhận thông báo.
   - Trả lời một phản hồi thì ô nhập tự điền đúng `@handle`.
   - Chat phòng hiện handle.
   - Mở `/@handle`.
