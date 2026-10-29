# M1: Hội viên kênh (thiết kế; anh chốt các câu hỏi 29/10)

**Anh chốt 29/10:** (1) đổi cấp: cấp mới bắt đầu ngay, ngày còn lại quy đổi theo tỉ lệ giá; (2) video hội viên: chủ kênh chọn cấp tối thiểu; (3) phí WeTube 10%; (4) **video chỉ hội viên hiện ở mọi nơi** (trang chủ, tìm kiếm, gợi ý, trang kênh) kèm ổ khoá, người chưa đủ cấp bấm vào thì thấy màn hình khoá.

PRD luồng 4: khán giả bấm **"Tham gia Hội viên"** → chọn cấp (huy hiệu, nội dung độc quyền) → thanh toán → được xem nội dung đặc quyền và có huy hiệu cạnh tên khi bình luận / chat. Đây cũng là giai đoạn C của thiết kế trang kênh (`2026-10-26-channel-ui-design.md`).

## 1. Hiện trạng (em đọc code 29/10)
- Bảng `tiers` (`MembershipTier`: tiêu đề, giá `float`, mô tả, `is_default`). Mỗi kênh có sẵn một cấp mặc định giá 0 đồng, dùng làm "Đăng ký kênh": bảng `subscriptions` có khoá (người dùng, cấp). Tối đa 4 cấp mỗi kênh, kể cả cấp mặc định.
- API `POST/PUT/DELETE /api/tiers` có sẵn (kiểm tra chủ kênh), nhưng **chưa có giao diện**. Trên production mọi kênh chỉ có cấp mặc định.
- **Chưa có bảng nào ghi "ai là hội viên của kênh nào, đến khi nào".** `POST /api/transactions/purchase` đang tắt (503) vì trước đây ai cũng "mua" được miễn phí. Bảng `transactions` có sẵn các cột số tiền, `channel_revenue`, `wetube_fee` (phí 10%).
- Thanh toán: PayPal chạy tốt cho Premium (`PaypalPaymentService`, đơn lưu trong `payment_momo`). MoMo đang ẩn vì MoMo sandbox lỗi (cờ `MOMO_ENABLED`).
- Huy hiệu Premium ở bình luận / chat trực tiếp đã có: cách làm huy hiệu hội viên sẽ giống vậy.
- Video: chế độ hiển thị là cột ENUM `videos_status` (chỉ được nối thêm giá trị vào cuối). `VideoAccess` quyết định ai xem được.

## 2. Đề xuất

### 2.1 Quy tắc
- **Cấp hội viên:** chủ kênh tạo tối đa **3 cấp trả phí**. Mỗi cấp có tên, giá **10.000 – 500.000 ₫ / 30 ngày**, mô tả quyền lợi. Cấp cao hơn = giá cao hơn.
- **Thời hạn:** mỗi lần mua được 30 ngày, **không tự gia hạn**. Mua thêm cùng cấp khi còn hạn thì cộng dồn (như Premium).
- **Đổi cấp khi đang là hội viên** (câu hỏi 1): cấp mới bắt đầu ngay. Số ngày còn lại của cấp cũ được đổi sang theo tỉ lệ giá (vd. còn 10 ngày cấp 20.000 ₫, đổi lên cấp 50.000 ₫ thì được cộng 4 ngày).
- **Không mua được hội viên kênh của chính mình.** Tham gia hội viên thì tự đăng ký kênh luôn (nếu chưa).
- **Phí WeTube 10%** (câu hỏi 3), như code sẵn có: mỗi giao dịch ghi số tiền, phần kênh nhận, phần WeTube. Chưa chi trả tiền thật cho chủ kênh (doanh thu chỉ để xem, Q1 làm trang quản trị doanh thu).
- **Thanh toán:** PayPal (MoMo tự hiện lại khi bật `MOMO_ENABLED`). Giá luôn lấy từ server.

### 2.2 Video chỉ dành cho hội viên
- Thêm lựa chọn **"Chỉ hội viên"** ở bước Chế độ hiển thị (hộp thoại tải lên, Studio), chỉ hiện khi kênh có ít nhất một cấp trả phí. Chủ kênh chọn **cấp tối thiểu** được xem (câu hỏi 2), mặc định cấp rẻ nhất.
- **Ai xem được:** chủ kênh, admin, hội viên còn hạn có cấp từ cấp tối thiểu trở lên. Người khác mở trang xem thì thấy ảnh thu nhỏ mờ, dòng "Video dành cho hội viên cấp X trở lên" và nút **Tham gia**. Tải xuống, phát nền, phòng xem chung, danh sách phát cũng theo quy tắc này.
- **Hiện ở đâu** (anh chốt câu 4): **mọi nơi** video công khai hiện (trang chủ, tìm kiếm, gợi ý, trang kênh: thẻ Video và thẻ mới **Hội viên**), thẻ video có ổ khoá + nhãn "Chỉ hội viên". Vì vậy `MEMBERS_ONLY` được tính là "được liệt kê" (`VideoAccess.LISTED`, chỉ mục tìm kiếm), nhưng **xem** thì vẫn kiểm tra cấp. Thông báo "video mới" cho người đăng ký vẫn gửi (bấm vào thì thấy màn hình khoá nếu chưa đủ cấp).

### 2.3 Dữ liệu (backend)
- Bảng mới `channel_members` (như `premium_user`): người dùng, kênh, cấp, ngày bắt đầu, ngày kết thúc. Một dòng cho mỗi lần mua; hội viên hiện tại = dòng còn hạn có ngày kết thúc xa nhất.
- `tiers`: thêm cột `archived` (ngừng nhận hội viên mới). Cấp đã có người mua thì không xoá được, chỉ ngừng nhận; hội viên cũ dùng hết hạn.
- `payment_momo`: thêm cột `tier_id` (null với đơn Premium). PayPal trả về thành công thì: đơn có `tier_id` → cấp hội viên + ghi `transactions`; không có → cấp Premium như cũ.
- `videos`: nối giá trị `MEMBERS_ONLY` vào cuối ENUM trạng thái, thêm cột `members_min_tier_id`.
- Runner idempotent cho mọi thay đổi trên (mẫu `CommentSchemaInitializer`), kèm index.

### 2.4 API
| API | Ai | Việc |
|---|---|---|
| `GET /api/channels/{id}/tiers` | mọi người | Các cấp trả phí đang nhận hội viên + cấp hiện tại của người xem và ngày hết hạn |
| `GET/POST/PUT/DELETE /api/tiers/...` | chủ kênh | Sửa API sẵn có: kiểm tra giá, tối đa 3 cấp, cấp có người mua thì ngừng nhận thay vì xoá |
| `POST /api/payment/paypal/membership` `{tierId}` | đăng nhập | Tạo đơn PayPal cho cấp, trả link chấp thuận; về `/return` chung với Premium |
| `GET /api/channels/me/members` | chủ kênh | Danh sách hội viên (tên, cấp, ngày hết hạn) + doanh thu (tổng, phí WeTube, phần kênh) |
| `POST /api/transactions/purchase` | | Xoá hẳn (thay bằng luồng PayPal ở trên) |

Bình luận và tin chat trực tiếp có thêm trường `memberTier` (tên cấp của người gửi ở kênh đó, null nếu không phải hội viên), tính một lần cho cả trang bình luận.

### 2.5 Giao diện (client)
- **Studio → mục mới "Hội viên":** tạo / sửa / ngừng nhận cấp (tên, giá, mô tả); danh sách hội viên; thẻ doanh thu (tổng, phí WeTube 10%, kênh nhận).
- **Trang kênh và trang xem:** nút **Tham gia** cạnh nút Đăng ký (chỉ khi kênh có cấp trả phí và người xem không phải chủ kênh). Bấm thì mở hộp thoại các cấp (tên, giá / 30 ngày, quyền lợi), chọn cấp rồi tới hộp thoại thanh toán như trang Premium. Đã là hội viên thì nút thành **"Hội viên · đến dd/mm"**, bấm vào để gia hạn hoặc đổi cấp.
- **Trang kết quả thanh toán:** "Bạn đã là hội viên cấp X của kênh Y đến dd/mm" + link về kênh.
- **Huy hiệu:** cạnh tên ở bình luận và chat trực tiếp, rê chuột hiện "Hội viên cấp X".
- **Video chỉ hội viên:** lựa chọn ở Chế độ hiển thị, nhãn "Chỉ hội viên" trên thẻ video ở trang kênh, thẻ **Hội viên** ở trang kênh, màn hình khoá ở trang xem.

## 3. Chia lần deploy
- **M1a (cỡ M–L):** cấp hội viên ở Studio, nút Tham gia + thanh toán PayPal, bảng hội viên, huy hiệu, danh sách hội viên + doanh thu ở Studio.
- **M1b (cỡ M–L):** video chỉ dành cho hội viên. Chỗ dễ sai nhất: mọi API trả danh sách video (trang chủ, tìm kiếm, gợi ý, kênh, danh sách phát, lịch sử) đều đang trả `videoUrl`; với video hội viên phải bỏ link phát khi người xem chưa đủ cấp, nếu không ai cũng lấy link xem được.
Mỗi phần commit ở máy, anh duyệt rồi mới push + deploy (backend trước, client sau).

## 4. Kiểm tra
- Backend: test cho cộng dồn / đổi cấp quy đổi ngày, không mua kênh mình, giá ngoài khoảng bị từ chối, cấp có người mua không xoá được, đơn PayPal cấp hội viên chỉ khi PayPal báo COMPLETED đúng số tiền, `VideoAccess` với video chỉ hội viên (khách, hội viên cấp thấp, cấp đủ, hết hạn, chủ kênh, admin), video hội viên có mặt ở trang chủ / tìm kiếm / gợi ý kèm cờ khoá nhưng link phát (`videoUrl`, `audioUrl`, tải xuống) không lộ cho người chưa đủ cấp.
- Client: `tsc`, `eslint`, `next build`; Chrome thật với API giả lập: Studio tạo cấp, nút Tham gia, hộp thoại chọn cấp, huy hiệu, màn hình khoá.
- Sau deploy: log không Exception; API mới khi chưa đăng nhập trả 401; anh mua thật bằng PayPal sandbox với 2 tài khoản (một chủ kênh, một người mua).

## 5. Ngoài phạm vi
Tự động gia hạn; chi trả tiền thật cho chủ kênh; bài đăng cộng đồng / emoji / livestream chỉ cho hội viên; thông báo khi có hội viên mới; hàng "Hội viên của chúng tôi" trên trang kênh; tặng hội viên.
