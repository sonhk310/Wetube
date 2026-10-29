# Thiết kế: Thanh toán PayPal sandbox (đường thứ hai cạnh MoMo)

Ngày: 2026-10-29. Trạng thái: **chờ anh duyệt**. Anh nói 29/10: MoMo sandbox lỗi (`resultCode 99` cho mọi yêu cầu, `developers.momo.vn` 403, đăng ký không được), muốn thêm PayPal sandbox, anh đã có Client ID và Secret.

## 1. Vì sao làm
- PRD 4.1.5 / BRD FR-7.3 đòi "một cổng thanh toán tích hợp", không chỉ định MoMo. MoMo hỏng thì không ai mua được Premium, quyền lợi P1 không có đường vào.
- PayPal sandbox truy cập được (kiểm tra 29/10), có tài khoản mua giả cho giám khảo.

## 2. Làm / không làm
**Làm:** tạo đơn PayPal, người dùng chấp thuận ở PayPal, backend capture rồi cấp Premium (dùng lại đúng đoạn cấp gói và gia hạn cộng dồn của P1), trang kết quả như MoMo.
**Không làm:** webhook, đăng ký định kỳ, hoàn tiền, các gói ngoài Basic, PayPal thật (live).

## 3. Luồng
1. Client bấm "Thanh toán bằng PayPal" → `POST /api/payment/paypal/create` `{subPackId}` (cần đăng nhập; người mua lấy từ JWT, không từ body).
2. Backend lấy token OAuth (`client_credentials`), gọi `POST {PAYPAL_BASE}/v2/checkout/orders` (`intent=CAPTURE`, 1 `purchase_unit` số tiền USD, `return_url` / `cancel_url` trỏ về backend). Lưu một dòng `payment_momo` (bảng dùng chung, `provider = "PAYPAL"`, `orderId` = id đơn PayPal, `amount` = giá VND, `payload` = JSON số tiền USD) trạng thái `PENDING`. Trả `{approveUrl}`; client chuyển trang sang đó.
3. Người dùng chấp thuận ở PayPal → PayPal chuyển về `GET /api/payment/paypal/return?token=<orderId>`.
4. Backend tìm đơn theo `token` (phải có trong DB, đang `PENDING`), gọi `POST /v2/checkout/orders/{id}/capture`. Chỉ cấp gói khi PayPal trả `status = COMPLETED` **và** số tiền / đơn vị tiền đã capture khớp số tiền đã lưu. Không tin tham số trên URL. Rồi chuyển về `{CLIENT_URL}/otherservices/payment-result?paymentStatus=0|1&orderId=…` (trang hiện có; thành công thì client gọi lại `/api/me`, đã làm ở P1).
5. `GET /api/payment/paypal/cancel` → đơn `FAILED`, chuyển về trang kết quả `paymentStatus=1`.
- Gọi lại `/return` nhiều lần cho cùng đơn chỉ cấp một lần (đã `SUCCESS` thì bỏ qua).
- Cookie đăng nhập (`SameSite=Strict`) không đi kèm lúc PayPal chuyển về, nên `/return` và `/cancel` là công khai (`permitAll`); an toàn vì chỉ capture được đơn đã tồn tại và người mua đã chấp thuận, và gói luôn cấp cho chủ đơn đã lưu.

## 4. Tiền tệ (PayPal không có VND)
- Đổi VND → USD theo **tỉ giá cố định** trong biến `PAYPAL_VND_PER_USD` (mặc định 25000), làm tròn 2 chữ số. Gói Basic 99.000 ₫ → 3,96 USD.
- Nút không ghi số USD (tránh phải thêm API báo giá); số tiền USD hiện ở trang PayPal.

## 5. Bảo mật / vận hành
- Khoá chỉ nằm ở biến môi trường, không ghi vào code, log hay tài liệu.
- Giới hạn gọi `create`: 10 lần / giờ / người (`RateLimiter` mới, 429 kèm câu tiếng Việt).
- **Log lỗi của PayPal** (mã + thông báo, không có khoá) để lần sau không phải đoán như vụ MoMo `99`.
- PayPal báo lỗi lúc tạo đơn thì không lưu đơn `PENDING`, trả lỗi có lý do cho client.
- Tách phần gọi HTTP ra một lớp mỏng (`PaypalClient`) để test không cần mạng.

## 6. Biến môi trường (backend, Vibe Host)
| Biến | Giá trị |
|---|---|
| `PAYPAL_CLIENT_ID` | Client ID sandbox |
| `PAYPAL_SECRET` | Secret sandbox |
| `PAYPAL_BASE` | `https://api-m.sandbox.paypal.com` |
| `PAYPAL_RETURN_BASE` | `https://wetube-service.vibe.tinhgon.xyz/api/payment/paypal` |
| `PAYPAL_VND_PER_USD` | tuỳ chọn, mặc định 25000 |

Dùng lại `CLIENT_URL` đã có. Thiếu biến PayPal thì nút PayPal báo "chưa cấu hình", MoMo không bị ảnh hưởng.

## 7. Client
`PaymentButton` có thêm nút thứ hai "Thanh toán bằng PayPal (tính theo USD)" cùng kiểu nút MoMo, có trạng thái đang xử lý và lỗi rõ ràng (hiện `message` của backend thay vì "Có lỗi" chung chung).

## 8. Kiểm tra
- **Backend (test đơn vị, `PaypalClient` giả):** đổi tiền đúng; tạo đơn đúng nội dung và lưu `PENDING`; capture `COMPLETED` đúng tiền thì cấp gói đúng số ngày và cộng dồn; hai lần `/return` chỉ cấp một lần; `status` khác `COMPLETED` hoặc sai số tiền / đơn vị thì `FAILED` và không cấp; `token` lạ không cấp; `/cancel` → `FAILED`; PayPal lỗi lúc tạo đơn thì không lưu đơn; giới hạn 429.
- **Sandbox thật:** gọi tạo đơn và (nếu anh để em dùng khoá qua biến môi trường của máy anh) capture thử, không dán khoá vào chat.
- **Client:** `tsc`, `eslint`, `next build`; Chrome thật với API giả lập (nút, chuyển trang, trang kết quả).
- **Anh thử:** mua thật bằng tài khoản mua giả PayPal sandbox, thấy Premium có ngay.

## 9. Em tự quyết (anh xem khi đọc)
- Tỉ giá cố định 25.000 ₫ = 1 USD, cấu hình bằng biến.
- Chỉ gói Basic; nhãn nút không ghi số USD.
- Capture khi quay về, không webhook.
- Dùng chung bảng `payment_momo` với `provider = "PAYPAL"` (không đổi cấu trúc DB; tên bảng chưa đổi để khỏi phải migrate).
- Tách đoạn cấp gói trong `PaymentServiceImpl` thành hàm dùng chung (sửa chỗ đang chạy, có test MoMo hiện có bảo vệ).

## 10. Cỡ việc
Cỡ M (khoảng 1 ngày).
