# Danh sách cần tự test (cần đăng nhập)

Các mục dưới đây em (Claude) không tự thử được vì cần phiên đăng nhập thật. Các phần không cần đăng nhập (API trả 401 khi chưa đăng nhập, trang trả 200, log backend sạch) đã kiểm tra trên server.

- Client: https://wetube.vibe.tinhgon.xyz
- Backend: https://wetube-service.vibe.tinhgon.xyz
- Kênh mẫu của anh: https://wetube.vibe.tinhgon.xyz/channel/cca81de9-7ba7-45dc-9729-08dd4ab498fb

Nên dùng **2 tài khoản** (A và B) cho các mục có người khác tham gia. Hiện DB mới có một tài khoản (`shinizeam@gmail.com`, là admin), nên cần đăng nhập thêm một tài khoản Google nữa.

Ghi kết quả vào cột cuối. Nếu có lỗi, ghi lại màn hình + giờ để tra log (`get_runtime_logs`).

> Giao diện đã Việt hoá toàn bộ (commit sau 30741db). Nếu thấy còn chữ tiếng Anh nào, ghi lại vị trí để em sửa nốt.

## 1. Đăng nhập và quyền

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 1.1 | Đăng nhập bằng tài khoản admin | Vào được trang chủ, không bị đăng xuất bất ngờ sau khi đổi JWT key | |
| 1.2 | Đăng nhập tài khoản thường (B) | Sidebar **không** có mục "Quản lý báo cáo" | |
| 1.3 | Với tài khoản admin | Sidebar có mục "Quản lý báo cáo" | |
| 1.4 | Tài khoản B mở `/admin/reports` | Thấy "Trang này chỉ dành cho quản trị viên" | |

## 2. Trang chủ và video

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 2.1 | Tài khoản mới (B) mở trang chủ | Thấy danh sách video mới nhất, không trống | |
| 2.2 | Upload một video ngắn (dưới 60 giây) | Upload xong, mở được trang xem | |
| 2.3 | Video ngắn vừa upload | Xuất hiện ở trang Shorts | |
| 2.4 | Mở trang Shorts, cuộn xuống | Phát video thật, thích/chia sẻ hoạt động, không còn dữ liệu mẫu | |

## 3. Tương tác ở trang xem (dùng tài khoản B xem video của A)

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 3.1 | Bấm Thích | Số lượt thích tăng, nút đổi màu; video có trong playlist "Đã thích" của B | |
| 3.2 | Bấm Thích lần nữa | Bỏ thích; video biến mất khỏi "Đã thích" | |
| 3.3 | Bấm "Xem sau" | Video có trong trang Xem sau; bấm lại thì gỡ ra | |
| 3.4 | Bấm "Đăng ký" kênh của A | Đổi sang "Đã đăng ký"; kênh A có thêm 1 người đăng ký | |
| 3.5 | Viết bình luận mới | Hiện ngay trong danh sách | |
| 3.6 | Xoá bình luận của chính mình | Bình luận biến mất | |
| 3.7 | B thử xoá bình luận của A | Không có nút "Xoá" | |
| 3.8 | Admin xoá bình luận của B | Có nút "Xoá" và xoá được | |

## 4. Trang kênh

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 4.1 | B mở kênh của A, tab Home và Videos | Thấy đúng tên, ảnh, video của A (không còn "Đức Anh Trader") | |
| 4.2 | A tạo một playlist công khai, B mở tab Playlist của kênh A | Thấy playlist đó | |
| 4.3 | A tạo bài đăng, B mở tab Bài đăng của kênh A | Thấy bài của A; **không** có ô đăng bài; không lẫn bài của B | |
| 4.4 | A mở tab Bài đăng của kênh mình | Có ô đăng bài | |

## 5. Riêng tư playlist

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 5.1 | B mở playlist "Đã thích"/"Lịch sử" của A bằng link trực tiếp | Không xem được (404) | |
| 5.2 | A xem lại các playlist của mình | Vẫn thấy đủ | |

## 6. Báo cáo vi phạm

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 6.1 | B mở video của A, bấm "..." | Có mục "Báo cáo video" | |
| 6.2 | A mở video của chính mình, bấm "..." | Có "Tạo phụ đề AI", **không** có "Báo cáo video" | |
| 6.3 | B gửi báo cáo (chọn lý do) | Báo "Đã gửi báo cáo" | |
| 6.4 | B báo cáo lại cùng video | Báo đã báo cáo, đang chờ xử lý | |
| 6.5 | B mở "Nhật ký báo cáo" (sidebar) | Thấy báo cáo, trạng thái "Đang chờ xử lý" | |
| 6.6 | Admin mở "Quản lý báo cáo" | Thấy báo cáo ở tab "Đang chờ" | |
| 6.7 | Admin bấm "Ẩn video" | Video bị ẩn; báo cáo chuyển "Đã xử lý" | |
| 6.8 | B mở lại link video bị ẩn | Không xem được; video biến mất khỏi tìm kiếm và gợi ý | |
| 6.9 | A (chủ video) mở lại video bị ẩn | Vẫn xem được | |
| 6.10 | Với báo cáo khác, admin bấm "Không vi phạm" | Trạng thái "Không vi phạm", video vẫn hiện | |

## 7. Phụ đề AI (Gemini)

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 7.1 | Upload video **có lời nói rõ**, chọn "Phụ đề AI (dịch sang)" = Tiếng Việt | Upload xong ngay (phụ đề chạy nền) | |
| 7.2 | Đợi khoảng 1 phút, tải lại trang xem | Có phụ đề tiếng Việt, bật/tắt được bằng nút phụ đề | |
| 7.3 | Đối chiếu lời phụ đề với lời nói thật | Đúng nghĩa. **Lưu ý:** Gemini có thể bịa lời khi audio khó nghe hoặc không có lời | |
| 7.4 | Với video đã đăng: "..." → "Tạo phụ đề AI" | Báo đang tạo; sau vài phút có phụ đề | |
| 7.5 | Upload video không có lời nói (nhạc/tiếng ồn) | Không tạo phụ đề, không lỗi | |
| 7.6 | Nút "AI Generate Description" ở form upload | Sinh được mô tả (trước đây hỏng vì model Gemini cũ) | |

## 8. Phòng xem chung và livestream

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 8.1 | Mở `/rooms` | Không sập 500; hiện trạng thái trống nếu chưa có phòng | |
| 8.2 | Tạo phòng, gửi link cho B | B vào được phòng | |
| 8.3 | A bấm play/pause/tua | B thấy đồng bộ | |
| 8.4 | Nhắn tin trong phòng | Cả hai thấy tin nhắn | |
| 8.5 | Livestream: giơ tay / mời lên sân khấu | Chạy (đã sửa đường dẫn `/api/streamApi/*`) | |

## 9. Thanh toán MoMo (sandbox)

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 9.1 | Mở trang Premium | Thấy 4 gói (Basic 99.000đ, Standard, Premium, VIP) | |
| 9.2 | Bấm dùng thử/mua gói Basic, thanh toán bằng thẻ test của MoMo | Sau thanh toán quay về `https://wetube.vibe.tinhgon.xyz/otherservices/payment-result` | |
| 9.3 | Sau khi trả tiền | Tài khoản có trạng thái Premium | |
| 9.4 | Ghi chú | Kiểm tra chữ ký MoMo chưa từng thử với giao dịch sandbox thật; nếu bị từ chối sai, đặt biến `MOMO_VERIFY_SIGNATURE=false` để so sánh | |

## 10. Bảo mật (chỉ cần xem, không phá hoại)

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 10.1 | Đăng xuất rồi thử thích/bình luận/đăng ký | Bị yêu cầu đăng nhập | |
| 10.2 | B xem hồ sơ công khai của A (`/api/users/{id}`) | Không lộ email | |

## Lỗi đã biết, chưa sửa

- Trang xem **chưa có nút "Trả lời"** cho bình luận (chỉ xem được các phản hồi có sẵn, không tạo mới được). Cũng chưa sửa được bình luận.
- Các ô chọn thể loại ở đầu trang chủ (Âm nhạc, Trò chơi...) **chưa lọc gì cả**.
- Nút Chia sẻ, Cảm ơn, Không thích, chuông thông báo **chưa làm gì**.
- Video bị admin ẩn có thể **hiện lại sau khi backend khởi động lại** (đang đồng bộ lại toàn bộ video vào Elasticsearch).
- Mở trang xem của video **không tồn tại** thì backend trả lỗi 500 thay vì 404.
- Nút "Đăng ký" ở trang kênh chỉ nhớ trạng thái trong phiên (tải lại trang thì hiện lại "Đăng ký").
- Creator chưa duyệt/sửa được phụ đề AI trước khi đăng (PRD có yêu cầu).
- Shorts: nút bình luận chỉ mở trang xem, chưa có bình luận ngay trong Shorts.
