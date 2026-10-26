# Thiết kế: giao diện trang kênh giống YouTube (H2)

Ngày: 2026-10-26. Trạng thái: **anh đã duyệt 26/10** (câu 1a: chưa hiện Tham gia; 2a: tên / ảnh kênh thay tên / ảnh Google; 3: Phổ biến theo tổng lượt xem; 4: tắt tạm `purchase`, đã làm). Giai đoạn A đã deploy, anh thử OK (26/10). Giai đoạn B làm xong ở máy, chờ anh duyệt push + deploy; **khác bản thiết kế**: tên / ảnh kênh được ghi sang tài khoản (thay vì đổi từng chỗ đọc), kèm đăng nhập Google không còn ghi đè tên / ảnh của người đã có kênh. C chưa làm.

## 1. Vì sao
Anh gửi ảnh trang kênh YouTube (nền tối) và muốn trang kênh WeTube giống vậy. Trên nền sáng của site (chưa có giao diện tối, việc F3).

**Hiện trạng (em đọc code 26/10):**
- Đầu trang (`channelHeader.tsx`): ảnh bìa cao cố định 160 px, avatar 160 px, tên, một dòng thống kê, mô tả cắt 2 dòng không có "xem thêm". Nút Đăng ký là một nút đen đơn giản, không có chuông.
- **Không có chỗ nào để đặt ảnh bìa, ảnh đại diện, mô tả kênh.** Trang Studio → Tuỳ chỉnh chỉ có khung handle (H1) và vài chữ tĩnh. Nên ảnh bìa của mọi kênh đang trống, và mô tả kênh luôn trống. Backend có sẵn các cột `backgroundImgUrl`, `description`, `picture` nhưng chưa có API sửa.
- Thẻ: Trang chủ, Video, Danh sách phát, Bài đăng. Chưa có thẻ Shorts.
- Trang chủ kênh chỉ là 6 video mới nhất xếp lưới. `VideoCarousel.tsx` và `channelFeatured.tsx` là code chưa ai dùng (khung rỗng).
- Danh sách video của kênh (`GET /api/videos/by-user/{userId}`) trả cả video thường lẫn Shorts, luôn sắp theo mới nhất, không lọc được.
- Backend đã có chế độ thông báo theo từng đăng ký (`ALL` / `PERSONALIZE` / `NONE_NOTIFICATION`, cột `notificationMode`, hàm `updateSubscriptionMode`) nhưng **chưa có API nào gọi hàm đó**, nên chuông không dùng được.
- Mỗi lần đăng nhập Google, `UserServiceImpl` ghi đè `AppUser.name` và `AppUser.picture`. Kênh có tên và ảnh riêng (`Channel.name`, `Channel.picture`), chỉ được đặt một lần lúc tạo kênh. Bình luận, chat phòng và thông báo đang hiện tên và ảnh của **tài khoản**, không phải của kênh.

**Thành công nghĩa là:**
- Trang kênh có bố cục như ảnh: ảnh bìa rộng, avatar lớn, tên, `@handle • N người đăng ký • N video`, mô tả một dòng + "...xem thêm", hàng nút, thẻ gạch chân, trang chủ có các hàng video cuộn ngang.
- Chủ kênh đặt được ảnh bìa, ảnh đại diện, tên, mô tả ở Studio và thấy ngay trên trang kênh.
- Chuông đăng ký chọn được Tất cả / Cá nhân hoá / Không, và chọn "Không" thì thôi nhận thông báo video mới của kênh đó.

## 2. Chia giai đoạn
Mỗi giai đoạn deploy riêng, anh thử xong mới sang giai đoạn sau.

### Giai đoạn A: đầu trang, thẻ, trang chủ (làm trước)
**Đầu trang:**
- Ảnh bìa rộng hết bề ngang nội dung, tỉ lệ khoảng 6:1 trên máy tính (cao tối thiểu 128 px), bo góc. Kênh chưa có ảnh bìa thì hiện nền xám nhạt chuyển sắc, không để trống.
- Avatar 160 px trên máy tính (80 px trên điện thoại). Bên phải avatar:
  - Tên kênh cỡ lớn, đậm.
  - Dòng `@handle • N người đăng ký • N video` (số rút gọn kiểu "6,66 N").
  - Mô tả cắt một dòng, kết thúc bằng "...xem thêm". Bấm thì mở hộp thoại **Giới thiệu**: mô tả đầy đủ (link và `@handle` bấm được), "Tham gia ngày …", quốc gia (đổi từ mã `countryCode`, bỏ qua nếu là `UN`), "N lượt xem".
  - Hàng nút xem ở dưới.
- **Hàng nút:**
  - Khách hoặc người xem khác: nút **Đăng ký** (đã đăng ký thì thành **Đã đăng ký ▾** có chuông, bấm mở menu: Tất cả / Cá nhân hoá / Không có thông báo / Huỷ đăng ký).
  - Chủ kênh: **Tuỳ chỉnh kênh** (sang Studio → Tuỳ chỉnh) và **Quản lý video** (sang Studio → Nội dung), thay cho nút Đăng ký, giống YouTube.
  - Không có nút **Tham gia** và **Cộng đồng** ở giai đoạn này (xem câu hỏi 1 và mục 5).

**Thẻ** (gạch chân đậm khi đang chọn như ảnh): Trang chủ, Video, **Shorts (mới)**, Danh sách phát, Bài đăng. Không làm thẻ Podcast (backend không có loại này) và ô tìm kiếm trong kênh (xem mục 5).
- Thẻ Video: hàng chip sắp xếp **Mới nhất / Phổ biến / Cũ nhất**, lưới video thường (không có Shorts). Tải thêm khi cuộn (dùng hook tải thêm có sẵn nếu có, không thì nút "Tải thêm").
- Thẻ Shorts: lưới thẻ dọc 9:16 (dùng lại thẻ Shorts của trang Lịch sử xem), bấm mở `/shorts/{id}`.

**Trang chủ kênh:** các hàng cuộn ngang có nút ‹ ›, mỗi hàng ẩn đi nếu không có nội dung:
1. **Video mới nhất** (video thường, 12 video gần nhất).
2. **Phổ biến** (video thường nhiều lượt xem nhất).
3. **Shorts** (thẻ dọc, kèm biểu tượng Shorts).
- Kênh chưa đăng gì: "Kênh này chưa có video nào." (chủ kênh thấy thêm nút Tải video lên).
- Không có hàng "Dành cho bạn" (YouTube cá nhân hoá theo người xem, WeTube chưa có dữ liệu để làm; "Video mới nhất" thay vị trí đó) và không có hàng "Hội viên của chúng tôi" (thuộc giai đoạn C).
- Xoá `channelFeatured.tsx` (khung rỗng không ai dùng). `VideoCarousel.tsx` được viết lại dùng thẻ video thật thay cho kiểu dữ liệu giả.

**Backend:**
- `GET /api/videos/by-user/{userId}` thêm tham số `type=all|video|short` và `sort=latest|popular|oldest`. Mặc định giữ nguyên hành vi cũ (`all`, `latest`), nên các chỗ đang dùng không đổi.
- `PUT /api/subscriptions/mode` `{ channelId, mode }`: đổi chế độ thông báo của người gọi với kênh đó (dùng hàm có sẵn, danh tính từ JWT). Kênh không đăng ký thì 404.
- `ChannelResponseDto` thêm `notificationMode` (chỉ có khi người xem đã đăng ký).

### Giai đoạn B: sửa kênh ở Studio → Tuỳ chỉnh
- Khung "Thông tin cơ bản" có thêm: **Ảnh bìa** (xem trước, nút Tải lên / Xoá; khuyến nghị 2048×1152 px, tối đa 6 MB), **Ảnh đại diện** (vuông, tối thiểu 98×98 px, tối đa 4 MB), **Tên kênh** (1–50 ký tự), **Mô tả** (tối đa 1000 ký tự). Mỗi khối có nút Lưu riêng, hoặc một nút Lưu chung nếu gọn hơn (em chọn khi làm).
- Ảnh tải lên qua `POST /api/uploads/image` có sẵn (Cloudinary), rồi gửi URL kèm các trường còn lại.
- `PUT /api/channels/me` `{ name?, description?, picture?, backgroundImgUrl? }`: chỉ sửa kênh của người gọi, chỉ ghi các trường có gửi, kiểm tra độ dài, `picture` / `backgroundImgUrl` phải là URL `https` của Cloudinary. Đổi tên thì cập nhật lại chỉ mục tìm kiếm (`ChannelIndexer`).
- **Tên và ảnh của kênh hiện ở mọi nơi** (câu hỏi 2): bình luận, chat phòng, thông báo lấy tên và ảnh từ kênh thay vì từ tài khoản Google. Lý do: Google ghi đè tên / ảnh tài khoản mỗi lần đăng nhập, nên nếu chủ kênh đổi tên ở Studio mà bình luận vẫn hiện tên Google thì rất lạ. Cách làm: `UserMapper.toBasicDto` và `WatchRoomService` ưu tiên `channel.name` / `channel.picture`, không có kênh thì dùng như cũ.

### Giai đoạn C: nút "Tham gia" và hội viên (M1): để sau, em đề xuất làm riêng
Anh đã chọn làm cả phần này. Em đề xuất tách thành bản thiết kế riêng và làm **sau G2 (thử MoMo sandbox)**, vì:
- Kênh nào hiện cũng chỉ có một cấp mặc định giá 0 đồng. **Không có giao diện nào để chủ kênh tạo cấp hội viên** (chỉ có API `POST /api/tiers/{channelId}`). Nên nút "Tham gia" sẽ không có gì để hiện.
- Thanh toán MoMo mới viết cho gói Premium, **chưa từng thử chữ ký** (G2), chưa nối với cấp hội viên.
- Chưa có bảng ghi "ai là hội viên của kênh nào, đến khi nào", nên không có huy hiệu, video riêng cho hội viên, hay hàng "Hội viên của chúng tôi".
- Ngoài ra em thấy một lỗ hổng (mục 4).

Giai đoạn C cần: Studio tạo / sửa cấp hội viên, bảng hội viên có hạn dùng, thanh toán MoMo cho cấp, nút Tham gia + hộp thoại chọn cấp, huy hiệu ở bình luận / chat, hàng "Hội viên của chúng tôi". Cỡ L, có bản thiết kế riêng cho anh duyệt.

## 3. Cần anh chọn
1. **Nút "Tham gia" / "Cộng đồng" ở giai đoạn A:**
   - (a) **Em đề xuất:** chưa hiện, chờ giai đoạn C. Hiện một nút không làm gì đúng là lỗi anh vừa báo ở trang xem (bấm avatar không đi đâu).
   - (b) Hiện nút "Tham gia" nhưng mờ, ghi "Sắp có".
2. **Tên và ảnh kênh có thay cho tên và ảnh tài khoản ở bình luận / chat / thông báo không** (giai đoạn B, mục trên)?
   - (a) **Em đề xuất:** có.
   - (b) Không: chỉ trang kênh dùng tên / ảnh kênh, bình luận vẫn dùng tên Google.
3. **Hàng "Phổ biến" ở trang chủ:** em xếp theo tổng lượt xem toàn thời gian. Được không, hay anh muốn "phổ biến gần đây"? (Em đề xuất tổng lượt xem cho đơn giản: kênh nhỏ ít dữ liệu.)

## 4. Lỗ hổng em thấy khi đọc code hội viên (đã có trên prod, chưa sửa)
`POST /api/transactions/purchase?membershipId=…` (`TransactionServiceImpl.purchaseMembership`) chỉ cần đăng nhập. Nó **không kiểm tra thanh toán nào**: tạo bản ghi giao dịch và cộng doanh thu (90% giá cấp) cho kênh. Ai gọi thẳng API cũng "mua" được bất kỳ cấp nào, nhiều lần, làm tăng `Channel.revenue` giả, kể cả kênh của chính mình. Client chưa gọi API này nên chưa ảnh hưởng người dùng thường, và doanh thu hiện chưa trả tiền thật cho ai. Em đề xuất: trong giai đoạn C, API này chỉ được gọi từ phía server sau khi MoMo báo thành công. Nếu anh muốn chặn ngay, em tắt endpoint bằng một commit riêng (client không dùng nên không hỏng gì).

## 5. Không làm trong gói này
- Giao diện tối.
- Thẻ Podcast.
- Ô tìm kiếm trong kênh (biểu tượng kính lúp cạnh thẻ).
- Nút "Cộng đồng".
- Hàng "Dành cho bạn" cá nhân hoá.
- Hàng "Kênh nổi bật" / "Kênh khác", video giới thiệu kênh (trailer).
- Cắt ảnh trong trình duyệt khi tải ảnh bìa / avatar (tải ảnh nguyên bản, hiển thị bằng `object-cover`).

## 6. Kiểm tra
- Backend: `compileJava compileTestJava`; test cho lọc / sắp xếp của `by-user` (video thường không lẫn Shorts, thứ tự phổ biến) và cho `PUT /api/channels/me` (độ dài, URL không phải Cloudinary bị từ chối, chỉ sửa kênh của mình).
- Client: `npx tsc --noEmit`, `npx eslint` các file sửa, `npx next build`; `next start` với backend giả để xem `/@handle`, `/@handle/shorts`, `/@handle/videos` trả 200.
- Sau deploy: log không `Exception`; curl `by-user?type=short&sort=popular`; `PUT /api/subscriptions/mode` và `PUT /api/channels/me` khi chưa đăng nhập trả 401.
- Anh thử trên trình duyệt (em chưa xem giao diện được): bố cục đầu trang ở máy tính và điện thoại, hộp thoại Giới thiệu, chuông (chọn "Không có thông báo" rồi đăng video mới bằng tài khoản kia, xem có thông báo không), các thẻ, hàng cuộn ngang, đặt ảnh bìa / avatar / tên / mô tả ở Studio rồi xem trang kênh, bình luận hiện tên kênh mới.
