# PRODUCT REQUIREMENTS DOCUMENT (PRD)
# Dự án: WeTube — Nền tảng Chia sẻ Video, Livestream & Phòng Xem Chung tích hợp AI

---

## 1. BÀI TOÁN & BỐI CẢNH (PROBLEM STATEMENT)

### 1.1. Bối cảnh
Trong thời đại bùng nổ của nội dung video số và livestream, nhu cầu tiếp cận kiến thức, giải trí xuyên biên giới và tương tác nhóm trực tuyến ngày càng cao. Tuy nhiên, quy trình sản xuất và thưởng thức video hiện tại vẫn còn nhiều điểm nghẽn lớn.

### 1.2. Bài toán thực tế
1. **Rào cản ngôn ngữ & năng suất Creator:** Nhà sáng tạo nội dung cá nhân và giáo dục phải tự nghe chép, tạo phụ đề và dịch thủ công. Quy trình này lặp lại 3–5 lần/tuần, tiêu tốn 3–5 giờ cho mỗi video dài 10–15 phút.
2. **Trải nghiệm xem chung gián đoạn:** Nhóm bạn bè, đồng nghiệp hoặc cộng đồng học tập muốn xem video cùng nhau (1–2 lần/tuần) hiện phải chia sẻ màn hình qua các app họp trực tuyến (Zoom, Meet, Discord) dẫn đến giật lag, chất lượng hình ảnh/âm thanh giảm và lệch pha khung hình.
3. **Mô hình tương tác & thương mại hóa phân mảnh:** Người sáng tạo khó xây dựng cộng đồng gắn kết và kiếm tiền ổn định nếu chỉ dựa vào lượt xem quảng cáo thụ động mà thiếu các công cụ như Hội viên kênh (Member Tier) hay Livestream tương tác độ trễ thấp.

---

## 2. ĐỐI TƯỢNG NGƯỜI DÙNG (USER PERSONAS)

| Nhóm người dùng | Nhu cầu chính | Nỗi đau hiện tại |
| :--- | :--- | :--- |
| **Nhà sáng tạo nội dung (Creators)** | Xuất bản video nhanh, tiếp cận khán giả đa quốc gia, mở rộng thu nhập. | Tốn nhiều giờ làm phụ đề, khó giữ chân người xem, công cụ livestream phức tạp. |
| **Khán giả / Người học (Viewers)** | Xem video có phụ đề chuẩn xác, xem Shorts tiện lợi, tương tác cộng đồng. | Khó hiểu video tiếng nước ngoài, không có không gian xem đồng bộ cùng bạn bè. |
| **Nhóm bạn bè / Sinh viên (Watch Party)** | Cùng xem một video ca nhạc, bài giảng, phim tài liệu từ xa và bàn luận thời gian thực. | Chia sẻ màn hình bị giật lag, tiếng bị vang, thao tác play/pause không đồng bộ. |
| **Quản trị viên (Admin / Moderator)** | Kiểm duyệt nội dung, bảo vệ bản quyền, phân tích số liệu hệ thống. | Thiếu bảng điều khiển tập trung để xử lý nhanh báo cáo vi phạm. |

---

## 3. MỤC TIÊU SẢN PHẨM & CHỈ SỐ THÀNH CÔNG (GOALS & KPIS)

* **Tự động hóa sản xuất phụ đề:** Giảm thời gian tạo và dịch phụ đề từ 3–5 giờ xuống dưới 1 phút nhờ Google Gemini AI.
* **Tăng mức độ tương tác & giữ chân (Retention):** Nâng thời lượng xem trung bình lên 35% thông qua tính năng Phòng xem chung (Watch Party) và bài đăng cộng đồng (Community Polls).
* **Độ trễ thấp:** Đạt độ trễ Livestream dưới 1 giây với LiveKit WebRTC.
* **Tỷ lệ chuyển đổi hội viên:** Đạt 5%–8% người dùng đăng ký Premium / Hội viên kênh.

---

## 4. PHẠM VI SẢN PHẨM (SCOPE)

### 4.1. Trong phạm vi (In-Scope - MVP & Phase 1)
1. **Hệ thống Video & Shorts:**
   - Đăng tải video dài (VOD) và video ngắn (Shorts) với phân loại danh mục, thẻ, thumbnail.
   - Trình phát video thích ứng, danh sách phát (Xem sau, Lịch sử, Đã thích).
2. **Trợ lý AI Đa ngôn ngữ (Gemini AI):**
   - Tự động nhận diện giọng nói (Speech-to-Text) và trích xuất phụ đề.
   - Tự động dịch phụ đề sang nhiều ngôn ngữ phổ biến.
   - Creator có quyền kiểm tra, chỉnh sửa bản dịch trước khi công bố.
3. **Phòng Xem Chung Đồng Bộ (Real-time Watch Party):**
   - Khởi tạo phòng xem chung riêng tư qua liên kết mời.
   - Đồng bộ thời gian phát (Play/Pause/Seek) giữa các thành viên với độ trễ mili-giây.
   - Khung chat nhóm trực tiếp trong phòng.
4. **Livestreaming độ trễ cực thấp:**
   - Tích hợp LiveKit Server (WebRTC) hỗ trợ phát trực tiếp chất lượng cao kèm Live Chat.
5. **Tương tác & Thương mại hóa:**
   - Đăng ký kênh (Subscribe), bình luận phân cấp (nested comments), thả tim/like.
   - Đăng bài cộng đồng (Community Posts) và Thăm dò ý kiến (Poll Vote).
   - Gói tài khoản Premium (tắt quảng cáo) và Hội viên kênh (Member Tiers).
6. **Cổng Quản trị Admin:**
   - Kiểm duyệt video, tiếp nhận và giải quyết báo cáo vi phạm (Report), quản lý người dùng và doanh thu.

### 4.2. Ngoài phạm vi (Out of Scope - Giai đoạn kế tiếp)
- Tích hợp mạng lưới quảng cáo tự động bên thứ ba (Programmatic Ads).
- Mã hóa bản quyền nội dung cấp doanh nghiệp (Enterprise Widevine/FairPlay DRM).

---

## 5. CÁC LUỒNG NGƯỜI DÙNG CHÍNH (CORE USER FLOWS)

### Luồng 1: Tải lên video và Tự động sinh phụ đề bằng AI
1. Creator chọn video tải lên từ máy tính -> Điền Tiêu đề, Mô tả, Chọn Danh mục & Thẻ.
2. Bật tùy chọn **"Tự động tạo phụ đề AI & Dịch thuật"**.
3. Hệ thống gửi audio đến dịch vụ AI (Google Gemini) để phân tích giọng nói và tạo phụ đề WebVTT.
4. Creator xem trước bản phụ đề, chỉnh sửa câu từ (nếu muốn) và nhấn **"Xuất bản"**.
5. Video hiển thị với tùy chọn bật/tắt phụ đề đa ngôn ngữ cho toàn bộ người xem.

### Luồng 2: Tạo và Tham gia Phòng Xem Chung (Watch Party)
1. Người dùng mở video yêu thích -> Nhấn nút **"Tạo phòng xem chung"**.
2. Hệ thống sinh mã phòng (Room ID) và đường dẫn chia sẻ (Invite Link).
3. Người tạo phòng gửi liên kết cho bạn bè. Bạn bè nhấp vào link để vào phòng ngay lập tức.
4. Bất kỳ ai trong phòng bấm Play, Pause hoặc tua video (Seek), trạng thái sẽ được đồng bộ ngay tức khắc cho tất cả thành viên qua WebSocket.
5. Các thành viên nhắn tin trò chuyện và thả cảm xúc song song trong khung Live Chat bên cạnh màn hình video.

### Luồng 3: Creator Phát Livestream & Tương tác
1. Creator vào mục **"Go Live"** -> Nhận Stream Key và cấu hình phòng.
2. Bắt đầu phát sóng thông qua LiveKit WebRTC Engine.
3. Người theo dõi nhận thông báo đẩy và tham gia xem với độ trễ dưới 1 giây.
4. Người xem trò chuyện trực tiếp và tham gia các khảo sát (Polls) do Streamer tạo ra.

### Luồng 4: Đăng ký Hội viên Kênh & Quyền lợi Đặc biệt
1. Khán giả bấm nút **"Tham gia Hội viên"** trên kênh của Creator.
2. Chọn cấp độ hội viên (Tier) tương ứng với các quyền lợi (Huy hiệu, nội dung độc quyền).
3. Xác nhận thanh toán qua Cổng thanh toán tích hợp.
4. Hệ thống cấp quyền truy cập nội dung đặc quyền và hiển thị huy hiệu cạnh tên khi bình luận/chat.

---

## 6. MÔ HÌNH DỮ LIỆU & KIẾN TRÚC KỸ THUẬT (DATA & ARCHITECTURE)

### 6.1. Kiến trúc hệ thống
* **Frontend:** Next.js (React), TailwindCSS, TypeScript — Giao diện hiện đại, tối ưu SEO, hỗ trợ Responsive & Dark Mode.
* **Backend:** Spring Boot (Java) — Xử lý nghiệp vụ lõi, bảo mật với Spring Security & JWT Token.
* **Media & Streaming Engine:** LiveKit (WebRTC) phục vụ Livestream; Cloud Storage / CDN phục vụ Video On Demand (HLS/DASH).
* **AI Engine:** Google Gemini AI API — Xử lý Speech-to-Text và Dịch thuật ngữ cảnh.
* **Database & Cache:** MySQL (Lưu trữ quan hệ) + Redis (Bộ nhớ đệm danh mục, xếp hạng và WebSocket Session).

### 6.2. Các thực thể dữ liệu chính (Data Entities)
* `User`: ID, Email, Username, Role (User, Creator, Admin), Avatar, SubscriptionTier.
* `Video`: ID, CreatorId, Title, Description, VideoUrl, ThumbnailUrl, Duration, ViewCount, Status (Public, Unlisted, Private).
* `Subtitle`: ID, VideoId, LanguageCode, Content (VTT format), IsAIGenerated.
* `WatchRoom`: ID, HostUserId, VideoId, CurrentTimestamp, PlaybackStatus, MembersList.
* `Comment`: ID, VideoId, UserId, ParentCommentId, Content, LikeCount, CreatedAt.
* `LiveSession`: ID, CreatorId, LiveKitRoomId, Title, Status, PeakViewers.
* `Transaction`: ID, UserId, Amount, Type (Premium, MemberTier), Status, CreatedAt.

---

## 7. KẾ HOẠCH & TIẾN ĐỘ THỰC HIỆN (ROADMAP)

* **Giai đoạn 1 (Đã hoàn thiện):** Xây dựng Core Framework, Quản lý tài khoản, Đăng tải & Phát video VOD, Mô hình cơ sở dữ liệu.
* **Giai đoạn 2 (Đã hoàn thiện):** Tích hợp Google Gemini AI tạo & dịch phụ đề tự động; Thiết lập máy chủ LiveKit cho Livestreaming.
* **Giai đoạn 3 (Đã hoàn thiện):** Hoàn thiện Watch Party Room đồng bộ thời gian thực; Phát triển tính năng Bài đăng cộng đồng & Polls; Tích hợp gói Hội viên Kênh.
* **Giai đoạn 4 (Hiện tại):** Tối ưu hóa hiệu năng, kiểm thử tải và đóng gói sản phẩm phục vụ vòng chấm thi.
