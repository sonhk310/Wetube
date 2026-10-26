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

## 11. Chế độ hiển thị video (A đăng, B kiểm tra)

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 11.1 | A upload video, ô "Chế độ hiển thị" mặc định | Mặc định "Công khai" | |
| 11.2 | A upload video với "Riêng tư" | Upload được. Trong Studio có nhãn "Riêng tư" | |
| 11.3 | B mở link video riêng tư của A | Không xem được (trang báo lỗi / 404) | |
| 11.4 | B tìm tên video riêng tư; xem kênh A, trang chủ | Không thấy video này ở đâu cả | |
| 11.5 | A mở lại video riêng tư của mình | Xem được | |
| 11.6 | A đổi video sang "Không công khai" trong Studio → Lưu | Nhãn "Không công khai" | |
| 11.7 | B mở link video không công khai | Xem được. Nhưng không có trong tìm kiếm, kênh A, gợi ý | |
| 11.8 | A đổi lại "Công khai" | Video xuất hiện lại ở tìm kiếm (có thể chậm vài giây) | |
| 11.9 | Admin ẩn một video (mục 6.7), rồi chờ backend khởi động lại | Video vẫn **không** quay lại tìm kiếm / gợi ý | |
| 11.10 | A mở Studio với video bị admin ẩn | Ô hiển thị khoá, ghi "Bị quản trị viên ẩn" | |

## 12. Xoá video (Studio)

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 12.1 | A bấm "Xoá" ở một video trong Studio | Hiện hộp thoại xác nhận | |
| 12.2 | Xác nhận xoá | Video biến mất khỏi Studio, tìm kiếm, kênh, playlist (kể cả "Xem sau"/"Đã thích") | |
| 12.3 | Mở link video đã xoá | Không xem được (404) | |
| 12.4 | B không phải chủ video thử xoá bằng API | Bị từ chối (403) | |
| 12.5 | Admin xoá video của người khác | Xoá được | |

## 13. Lượt xem

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 13.1 | Mở thẳng link video (không bấm từ trang chủ), phát đủ 5 giây | Tải lại trang: lượt xem tăng đúng 1 | |
| 13.2 | Tải lại và xem tiếp trong 30 phút | Lượt xem **không** tăng thêm | |
| 13.3 | Mở video bằng trình duyệt ẩn danh (chưa đăng nhập), xem 5 giây | Lượt xem tăng 1 | |
| 13.4 | Mở trang Shorts, để một video hiện 3 giây | Lượt xem của short đó tăng 1 | |
| 13.5 | Mở video nhưng tạm dừng trước 5 giây | Không tăng | |

## 14. Tương tác ở trang xem (YouTube parity)

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 14.1 | B bấm "Không thích" | Biểu tượng đậm lên, **không** hiện số lượt không thích | |
| 14.2 | B bấm "Thích" khi đang không thích | Bỏ không thích, chuyển sang thích (và ngược lại) | |
| 14.3 | Tải lại trang | Trạng thái thích / không thích giữ nguyên | |
| 14.4 | Bấm "Trả lời" dưới bình luận của A | Hiện ô nhập; gửi xong phản hồi xuất hiện, số phản hồi tăng | |
| 14.5 | Bấm "Trả lời" dưới một phản hồi | Ô nhập có sẵn "@tên"; phản hồi nằm trong cùng nhóm với bình luận gốc | |
| 14.6 | Bấm "Sửa" ở bình luận / phản hồi của chính mình | Sửa được, nội dung đổi ngay. Bình luận người khác không có nút "Sửa" | |
| 14.7 | Bấm "Chia sẻ" | Hộp thoại có link, "Sao chép", Facebook, X. Tích "Bắt đầu tại m:ss" thì link có `?t=` | |
| 14.8 | Mở link có `?t=30` | Video nhảy tới giây 30 | |
| 14.9 | Trên điện thoại bấm "Chia sẻ" | Hiện bảng chia sẻ của hệ điều hành | |
| 14.10 | Bấm "Lưu" | Hộp thoại liệt kê playlist của B kèm dấu tích (playlist đã chứa video có tích sẵn) | |
| 14.11 | Tích / bỏ tích một playlist; tạo playlist mới ngay trong hộp thoại | Video vào / ra playlist; playlist mới có video | |
| 14.12 | Không thấy nút "Cảm ơn" và chuông thông báo | Đã ẩn | |

## 15. Trang chủ, tìm kiếm, kênh

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 15.1 | Mở trang chủ; sau đó thêm thẻ cho một video công khai (Studio → Thẻ) rồi tải lại | Ban đầu hàng chip chỉ có "Tất cả"; sau khi có thẻ thì chip `#tag` xuất hiện | |
| 15.2 | Bấm chip `#tag` | Chỉ hiện video công khai có thẻ đó, cuộn xuống tải thêm; "Tất cả" quay lại gợi ý | |
| 15.3 | Gõ "vi" vào ô tìm kiếm khi có video tên "view..." | Gợi ý có "view...", vẫn gợi ý được khi gõ sai nhẹ | |
| 15.4 | Mở kênh của A (B đã đăng ký ở 3.4), tải lại trang | Nút vẫn là "Đã đăng ký" | |
| 15.5 | Bấm "Đã đăng ký" | Huỷ đăng ký, nút về "Đăng ký", số người đăng ký giảm | |

## 16. Phòng xem chung (W1)

Cần 2 tài khoản đăng nhập ở 2 cửa sổ (A = chủ phòng, B = khách). Dùng 2 trình duyệt hoặc 1 cửa sổ ẩn danh.

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 16.1 | A mở một video, tua tới khoảng 1:00, bấm nút "Xem chung với bạn bè" (cạnh Chia sẻ) | Chuyển sang `/rooms/abc-defg-hij`, video phát tiếp từ khoảng 1:00, hiện hộp thoại "Mời bạn bè" có link | |
| 16.2 | A bấm "Sao chép", B (đã đăng nhập) mở link | B vào phòng, thấy video ở cùng giây với A (lệch dưới khoảng 0,5 giây), cả hai thấy tên + ảnh nhau trong "Đang xem", A có vương miện | |
| 16.3 | B bấm Dừng | Video của A cũng dừng ngay; chat có dòng "B đã dừng video" | |
| 16.4 | A bấm Phát, rồi tua tới 3:00 | B phát theo, rồi nhảy tới 3:00; chat có dòng "A đã tua tới 3:00" | |
| 16.5 | A và B cùng bấm Dừng / Phát gần như cùng lúc | Cả hai máy kết thúc ở cùng một trạng thái, không bị giật qua lại | |
| 16.6 | Nhắn tin ở hai bên; gõ "2:30" trong tin rồi bấm vào mốc đó | Tin hiện ở cả hai bên; bấm mốc thời gian thì cả phòng tua tới 2:30 | |
| 16.7 | (Đã bỏ: thả cảm xúc, anh quyết 26/10) | | |
| 16.8 | Ở "Hàng chờ", tìm một video khác rồi bấm để thêm; bấm "Phát ngay" ở video đó | Cả hai thấy hàng chờ đổi; "Phát ngay" thì cả phòng chuyển sang video mới từ giây 0 | |
| 16.9 | Thêm một video vào hàng chờ rồi để video hiện tại chạy tới hết | Cả hai tự sang video trong hàng chờ, chỉ nhảy **một** video (không bỏ qua video thứ hai) | |
| 16.10 | B bấm F5 (tải lại trang) | B vào lại phòng trong vài giây, chat không có dòng "B đã rời phòng" rồi "B đã vào phòng" | |
| 16.11 | B mở thêm tab thứ hai cùng phòng rồi đóng tab đó | B vẫn trong phòng, danh sách chỉ có 1 B | |
| 16.12 | B bấm "Rời phòng" | Về trang xem video; A thấy dòng "B đã rời phòng" ngay, danh sách chỉ còn A | |
| 16.13 | B vào lại; A bấm vào ảnh B → "Mời B ra khỏi phòng" | B thấy "Bạn đã bị mời ra khỏi phòng"; B mở lại link thì báo không vào được | |
| 16.14 | Khi B ở trong phòng, A bấm "Rời phòng" | B trở thành chủ phòng (có vương miện và nút "Đóng phòng") | |
| 16.15 | Chủ phòng bấm "Đóng phòng" | Mọi người thấy "Chủ phòng đã đóng phòng"; mở lại link báo phòng không tồn tại | |
| 16.16 | Đăng xuất rồi mở link mời | Hiện "Đăng nhập để vào phòng xem chung"; đăng nhập xong quay lại đúng phòng | |
| 16.17 | Mở `/rooms/xxx-yyyy-zzz` (mã không có) | "Phòng không tồn tại hoặc đã đóng" | |
| 16.18 | Mở `/rooms`: tạo phòng trống; dán link / mã vào "Vào phòng" | Tạo được phòng trống; thêm video đầu tiên thì phát luôn; dán mã sai thì báo lỗi | |
| 16.19 | Trong phòng, mở menu cài đặt của trình phát và bấm chuột phải lên video | Không có "Lặp lại" và "Tốc độ phát"; không có công tắc Tự phát | |
| 16.20 | Backend deploy lại khi cả hai đang trong phòng | Sau vài giây cả hai tự kết nối lại, vẫn đúng video và đúng giây, chat cũ còn | |

## Lỗi đã biết, chưa sửa

- Chưa có: thông báo (chuông), nút "Cảm ơn", giao diện tối, chất lượng video / tự phát tiếp, trang Đăng ký dạng bảng tin video mới, thống kê trong Studio.
- Video chưa gắn thẻ (#tag) thì chip ở trang chủ chỉ có "Tất cả".
- Số lượt xem hiển thị ở trang xem là số lúc tải trang; lượt xem của chính phiên này chỉ cập nhật khi tải lại.
- Creator chưa duyệt/sửa được phụ đề AI trước khi đăng (PRD có yêu cầu).
- Shorts: nút bình luận chỉ mở trang xem, chưa có bình luận ngay trong Shorts.

## 17. @handle (H1)

Cần 2 tài khoản đăng nhập (A và B). Sau deploy, mọi kênh cũ đã có handle tự sinh từ tên (vd. "Huỳnh Kỳ Sơn" → `@huynhkyson`).

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 17.1 | A mở trang kênh của mình | Dưới tên kênh hiện `@handle • N người đăng ký • N video` | |
| 17.2 | Mở `/@handle` của A (gõ trên thanh địa chỉ); rồi mở `/channel/{id}` của A | Cả hai cho ra trang kênh A và thanh địa chỉ hiện `/@handle`; bấm thẻ Video / Danh sách phát / Bài đăng thì địa chỉ là `/@handle/videos`...; chữ hoa (`/@HuynhKySon`) vẫn ra; handle không có thì báo "Không tìm thấy kênh này" | |
| 17.3 | A vào Studio → Tuỳ chỉnh, gõ handle sai (`ab`, `có dấu`, `-abc`) | Báo lý do ngay dưới ô, nút Lưu bị khoá | |
| 17.4 | A gõ handle của B | Báo "Handle đã có người dùng" | |
| 17.5 | A gõ một handle còn trống rồi Lưu | Báo "Đã đổi handle"; trang kênh hiện handle mới; `/@cũ` không còn dùng được | |
| 17.6 | A đổi handle lần nữa ngay sau đó | Báo "Bạn có thể đổi handle lại sau ngày ..." (14 ngày) | |
| 17.7 | A bình luận video; B xem | Bình luận của A hiện tên kèm `@handle` xám nhỏ | |
| 17.8 | B bấm Phản hồi bình luận của A, gửi; A bấm Phản hồi chính phản hồi của B | Ô nhập tự điền `@handle-của-B `, không phải tên đầy đủ; `@handle` trong bình luận màu tím đậm (khác link xanh), bấm mở kênh | |
| 17.9 | B viết bình luận có `@handle` của A (không phải trả lời A) | A thấy chuông "B đã nhắc đến bạn trong một bình luận"; chỉ một thông báo, không trùng | |
| 17.10 | Viết bình luận có email (`abc@gmail.com`) | Email không bị biến thành link kênh, không ai nhận thông báo | |
| 17.11 | A và B vào cùng phòng xem chung, nhắn tin | Tên trong chat kèm `@handle`; rê chuột vào ảnh ở "Đang xem" thấy `Tên (@handle)` | |
| 17.12 | Trong phòng, B gõ `@` ở cuối ô chat | Hiện danh sách người trong phòng có handle (không có B); gõ thêm chữ thì lọc; ↑↓ chọn, Enter hoặc Tab chèn `@handle `, Esc đóng | |
| 17.13 | B gửi tin có `@handle` của A | Trên máy A, tin đó có nền tím nhạt; trên máy B (người gửi) không tô; `@handle` trong tin màu tím, bấm mở kênh | |

## 18. Giao diện trang kênh (H2, giai đoạn A)

Cần 2 tài khoản: A (chủ kênh, có vài video thường và vài Shorts) và B (người xem). Trang kênh chưa có ảnh bìa thì hiện nền xám chuyển sắc (đặt ảnh bìa là giai đoạn B).

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 18.1 | B mở kênh của A | Ảnh bìa rộng (nền xám nếu chưa có), avatar lớn, tên lớn, dòng `@handle • N người đăng ký • N video`, mô tả một dòng, nút Đăng ký | |
| 18.2 | Bấm "...xem thêm" (hoặc "Tìm hiểu thêm về kênh này" nếu chưa có mô tả) | Hộp thoại Giới thiệu: mô tả đầy đủ, link `/@handle`, "Đã tham gia ngày ...", quốc gia (nếu có), số lượt xem | |
| 18.3 | B bấm Đăng ký | Nút đổi thành "Đã đăng ký ▾" có chuông; mở menu thấy Tất cả / Cá nhân hoá (có dấu ✓) / Không có thông báo / Huỷ đăng ký | |
| 18.4 | B chọn "Không có thông báo", tải lại trang | Biểu tượng chuông gạch chéo, menu vẫn tích "Không có thông báo". A đăng một video thường mới: B **không** nhận thông báo | |
| 18.5 | B chọn "Huỷ đăng ký" | Nút trở lại "Đăng ký", số người đăng ký giảm | |
| 18.6 | Khách (chưa đăng nhập) bấm Đăng ký | Hiện thông báo mời đăng nhập có nút "Đăng nhập" | |
| 18.7 | A mở kênh của mình | Thay nút Đăng ký bằng "Tuỳ chỉnh kênh" (sang Studio → Tuỳ chỉnh) và "Quản lý video" (sang Studio → Nội dung) | |
| 18.8 | Xem thanh thẻ | Trang chủ, Video, Shorts, Danh sách phát, Bài đăng; thẻ đang chọn đậm, gạch chân đen; bấm từng thẻ thì địa chỉ là `/@handle`, `/@handle/videos`, `/@handle/shorts`... | |
| 18.9 | Trang chủ kênh | Hàng "Video mới nhất" (cuộn ngang, nút ‹ › hiện khi còn nội dung), hàng "Shorts" thẻ dọc; hàng "Phổ biến" chỉ có khi kênh có trên 12 video; hàng nào trống thì ẩn | |
| 18.10 | Thẻ Video: bấm chip Phổ biến / Cũ nhất | Thứ tự đổi theo lượt xem / ngày; không có Shorts lẫn trong đó; kênh nhiều video thì có nút "Tải thêm" | |
| 18.11 | Thẻ Shorts | Chỉ có Shorts, thẻ dọc 9:16; bấm một thẻ mở trình phát Shorts | |
| 18.12 | Thu nhỏ cửa sổ cỡ điện thoại | Ảnh bìa thấp hơn, avatar nhỏ, tên và nút căn giữa, thanh thẻ cuộn ngang được, không tràn ngang trang | |
| 18.13 | B chọn "Cá nhân hoá", A đăng một video thường rồi một Short | B nhận thông báo video thường, **không** nhận thông báo Short. Đổi B sang "Tất cả" rồi A đăng thêm một Short: B nhận | |
| 18.14 | B mở một video của kênh A ở trang xem, bấm "Đã đăng ký" | Mở menu chuông giống trang kênh (không còn huỷ đăng ký ngay); chọn chế độ ở đây thì mở lại trang kênh thấy đúng chế độ đó; "Huỷ đăng ký" thì số người đăng ký giảm ngay | |

## 19. Sửa kênh ở Studio (H2, giai đoạn B)

Dùng tài khoản chủ kênh (A), thêm tài khoản B để xem. Ảnh thử: một ảnh ngang lớn (ít nhất 2048 px) cho ảnh bìa, một ảnh chữ nhật cho ảnh đại diện.

| # | Việc cần làm | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| 19.1 | A vào Studio → Tuỳ chỉnh | Khung "Thông tin cơ bản" có: Ảnh bìa, Ảnh đại diện, Tên kênh, Mô tả, Handle | |
| 19.2 | Bấm "Tải ảnh bìa lên", chọn ảnh ngang | Báo "Đã cập nhật ảnh", ảnh hiện trong khung xem trước; mở trang kênh thấy ảnh bìa rộng, không bị méo | |
| 19.3 | Chọn một tệp không phải ảnh (vd. PDF), rồi một ảnh trên 6 MB | Báo lỗi ngay ("Tệp phải là ảnh" / "Ảnh quá lớn"), không tải lên | |
| 19.4 | Bấm "Xoá" ở ảnh bìa | Báo "Đã xoá ảnh bìa", trang kênh trở lại nền xám | |
| 19.5 | Bấm "Đổi ảnh đại diện", chọn ảnh chữ nhật | Ảnh được cắt vuông quanh phần nổi bật; ảnh ở góc phải thanh trên cùng đổi ngay, **không cần tải lại trang** | |
| 19.6 | Đổi tên kênh rồi bấm "Lưu thay đổi" | Tên mới hiện ở trang kênh, thanh trên cùng, bình luận cũ của A, thẻ video của kênh, tên dưới video ở trang xem. Ô tìm kiếm (gõ tên mới) ra đúng kênh / video | |
| 19.7 | Gõ mô tả có một link và một `@handle`, Lưu | Trang kênh hiện mô tả một dòng + "...xem thêm"; hộp thoại Giới thiệu hiện đủ, link và `@handle` bấm được. Xoá hết mô tả rồi Lưu thì dòng đó thành "Tìm hiểu thêm về kênh này" | |
| 19.8 | Xoá trống ô tên; gõ tên dài quá 50 ký tự | Nút Lưu bị khoá khi trống; ô nhập không cho gõ quá 50 ký tự | |
| 19.9 | Đăng xuất rồi đăng nhập lại bằng Google | Tên và ảnh vẫn là bản đã đặt ở Studio, **không** quay về tên / ảnh Google | |
| 19.10 | A vào một phòng xem chung mới (hoặc phòng đang có A thì thoát vào lại), nhắn tin | Danh sách người xem và chat hiện tên và ảnh mới | |
