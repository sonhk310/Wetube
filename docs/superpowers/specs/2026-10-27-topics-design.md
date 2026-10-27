# Thiết kế: chủ đề video (C8)

Ngày: 2026-10-27. Trạng thái: **nháp lần 2, chờ anh duyệt** (đã theo câu trả lời của anh ngày 27/10; còn mục 8).

## 1. Vì sao
Chip chủ đề lấy từ hashtag ra tên vụn, không dấu cách, trùng ý ("nhacanime", "animevn", "nhạcphim"), nên đã bỏ ngày 27/10 (commit `2585c15`). YouTube dùng chủ đề có **tên chuẩn** ("Trò chơi", "Bóng đá", "Podcast"). C8 làm lại chip chủ đề theo cách đó.

**Anh đã chốt 27/10:**
- Danh sách chủ đề **cố định, 2 tầng**, lấy từ file anh đưa: `youtube-topic-taxonomy.md`. Không ai tự tạo chủ đề.
- Mỗi video **đúng 1 chủ đề, bắt buộc**.
- Chọn bằng **dropdown** khi tải lên và khi sửa video; có nút **gợi ý bằng AI**.
- Thẻ (`#thẻ`) giữ nguyên vai trò: `/hashtag`, tìm kiếm, gợi ý "Tất cả". Thẻ không làm chip nữa.

## 2. Danh sách chủ đề: từ file của anh sang `topics.json`
File có 52 nhóm (tầng 1), mỗi nhóm là các mục cách nhau dấu `;` (tầng 2). Em đo: khoảng **1.570 mục**, trong đó 104 tên lặp ở hai nhóm, 81 mục gộp bằng "/", 18 mục "… tổng hợp". Em chuyển sang `WeTube-service-dev/src/main/resources/topics.json` bằng script + sửa tay, theo quy tắc:
1. **Tầng 1** = 52 nhóm, giữ tên của file ("Âm nhạc", "Gaming và trò chơi điện tử"…).
2. **Bỏ nhóm 50 "Định dạng và hình thức video"**: chính file ghi đây là định dạng, không phải chủ đề (Shorts, vlog, livestream đã có chỗ riêng).
3. **Mục "… tổng hợp"** ("Âm nhạc tổng hợp", "Gaming"…) không thành chủ đề con: chọn **cả nhóm** là chọn nó (mục 3).
4. **Mục gộp "/" tách ra** khi là hai thứ khác nhau ("du lịch Việt Nam/quốc tế" → "Du lịch Việt Nam", "Du lịch quốc tế"); giữ nguyên khi là một thứ ("SSD/HDD", "5G/6G", "Layer 1/Layer 2").
5. **Trùng trong cùng nhóm** (vd. "Android" hai lần ở Công nghệ): giữ một. **Trùng giữa hai nhóm** ("drone" ở Công nghệ và ở Trải nghiệm): giữ cả hai, vì là hai góc nhìn khác nhau; id khác nhau nhờ có id nhóm phía trước.
6. Viết hoa chữ đầu cho dễ đọc ("bóng đá" → "Bóng đá"), giữ nguyên tên riêng / viết tắt ("iPhone", "EDM", "F1").
7. **Id ổn định** dạng `nhóm.mục` không dấu (`sports.bong-da`, `music.lo-fi`), không bao giờ đổi khi đã có video dùng; đổi tên hiển thị thì được.

Em sẽ gửi anh file `topics.json` (hoặc một bảng) để anh xem trước khi code phần còn lại. Backend đọc file lúc khởi động; client lấy qua `GET /api/topics` (công khai, cache lâu). Đổi danh sách = sửa file + deploy.

## 3. Một video một chủ đề
- Mỗi video có **đúng 1 chủ đề**: một chủ đề con ("Bóng đá") **hoặc** cả nhóm ("Thể thao và thể hình", khi video chung chung hoặc không có mục con hợp).
- **Chủ đề con kéo theo nhóm:** video "Bóng đá" cũng thuộc nhóm "Thể thao và thể hình" khi lọc theo chip nhóm. Chỉ lưu id đã chọn; nhóm được thêm lúc đẩy sang Elasticsearch.
- **Bắt buộc** khi lưu video với chế độ hiển thị khác bản nháp (Xuất bản, và Lưu ở màn sửa video đã đăng). Bước chọn tệp (`POST /api/videos/drafts`, chỉ nhận tệp) và lưu mà vẫn để là bản nháp thì chưa cần, để không chặn việc tải file.
- **Video cũ** (kho có 4 video) chưa có chủ đề: vẫn hiện bình thường, chỉ không vào chip chủ đề. Lần đầu chủ video mở sửa thì phải chọn chủ đề mới lưu được.

## 4. Chọn chủ đề ở Studio
Anh hỏi bản trước có phức tạp quá không: **có**, menu con + ô tích là để chọn nhiều chủ đề. Chỉ chọn 1 thì đơn giản hơn nhiều:

- **Hai dropdown liền nhau, dùng `components/ui/select.tsx` có sẵn** (không thêm component mới):
  1. **Nhóm**: 51 nhóm.
  2. **Chủ đề**: chỉ hiện các mục của nhóm vừa chọn; dòng đầu là "Chung (cả nhóm)". Đổi nhóm thì ô này về "Chung".
- Nhóm dài nhất có khoảng 80 mục ("Nghề nghiệp chuyên biệt…"), cuộn trong dropdown được, chưa cần ô tìm kiếm.
- Nút **"Gợi ý chủ đề"** cạnh hai ô: Gemini đọc tiêu đề, mô tả, thẻ, từ khóa rồi **điền sẵn** cả hai ô; chủ video đổi được rồi lưu cùng biểu mẫu. Không tự gợi ý, không tự lưu (như S2).
- Ba chỗ có ô: tải lên (`upload-wizard.tsx`), sửa video (`form-section.tsx`), hộp thoại nháp (`draft-edit-dialog.tsx`), đặt cạnh ô Thẻ / Từ khóa tìm kiếm. Chưa chọn mà bấm Xuất bản / Lưu thì báo lỗi dưới ô như các ô bắt buộc khác.
- Lưu ý: Radix Select gặp `form.reset()` có thể gọi `onValueChange("")` (bẫy trong CLAUDE.md), nên bỏ qua giá trị rỗng.

## 5. Chip trang chủ và điểm yêu thích chủ đề
- **Điểm:** bảng mới `user_topic` (user_id, topic_id, point), giống `user_tag`. Cộng ở **đúng những chỗ đang cộng điểm thẻ** (`TagAffinityService.addQuietly`: xem được tính +9, thích +5, bình luận gốc đầu tiên +8, Sao chép ở hộp Chia sẻ +12; trần 1000) cho chủ đề của video **và** nhóm của nó. Lỗi chỉ ghi log.
- **Chip:** sau 4 chip cố định (Kênh đăng ký · Mới tải lên · Chưa xem · Đã xem), thêm tối đa 8 chip chủ đề: chủ đề / nhóm người xem có điểm cao trước, rồi những cái có nhiều video công khai nhất (aggregation `terms` trên trường `categories` của ES). Chỉ hiện cái **còn ít nhất 1 video công khai**.
- **Nhãn chip:** tên chủ đề ("Bóng đá", "Lo-fi"). Một số tên nhóm trong file dài ("Pháp y, điều tra và khoa học ứng dụng", "Nghề nghiệp chuyên biệt và các chủ đề bổ sung"), nên `topics.json` có thêm **tên ngắn cho chip** ở những nhóm này (vd. "Pháp y", "Nghề nghiệp"); em đề xuất, anh sửa ở mục 8.
- **Bấm chip:** `GET /api/home/feed?chip=topic:sports.bong-da` (thêm loại chip `topic:` vào `HomeFeedService.feed`): video công khai thuộc chủ đề / nhóm đó, mới nhất trước, tải thêm khi cuộn. Client: nhánh `tag:` còn sót trong `home-view.tsx` đổi thành `topic:`, dùng lại `chipVideoGrid.tsx`.
- Gợi ý "Tất cả" (`FeedRanker`) **không đổi** trong C8: vẫn chấm theo thẻ.

## 6. Thay đổi dữ liệu
- **MySQL:** cột `videos.topic_id` (VARCHAR(100), NULL); bảng `user_topic`. Thêm bằng runner idempotent lúc khởi động (mẫu `CommentSchemaInitializer`, mỗi câu SQL một transaction). Cột để NULL được vì có video cũ và bản nháp; "bắt buộc" kiểm tra ở API lưu / xuất bản.
- **Backend lưu:** `PUT /api/videos/{id}` (nơi lưu mọi chi tiết, kể cả xuất bản qua trường `status`) nhận thêm `topicId`. Id không có trong danh sách thì 400; thiếu `topicId` mà `status` không phải bản nháp thì 400; cả hai kèm thông báo tiếng Việt. `VideoFormDetailDto` trả `topicId`. `PATCH /api/videos/visibility` (đổi hiển thị hàng loạt) đã bỏ qua bản nháp nên không xuất bản được video thiếu chủ đề; video cũ đã đăng mà chưa có chủ đề thì vẫn đổi hiển thị được.
- **Elasticsearch:** trường `categories` (keyword) **đã có** trong `videos_v2-mapping.json` và trên production, đang luôn rỗng. `VideoIndexer.toDocument` ghi vào đó `[chủ đề, nhóm]`. Không cần put mapping; `ElasticBootstrapLoader` đồng bộ lại lúc khởi động.
- **Gemini:** `POST /api/ai/topic` (đăng nhập, cùng hạn mức `RateLimiter.Limit.AI_TEXT`), nhận tiêu đề, mô tả, thẻ, từ khóa. Danh sách quá dài để gửi nguyên một lần cho mỗi lượt gợi ý (khoảng 35 KB), nên làm **hai bước**: (1) gửi 51 tên nhóm, Gemini chọn 1 nhóm; (2) gửi các mục của nhóm đó, Gemini chọn 1 mục hoặc "Chung". Server bỏ id lạ (khi đó trả nhóm với "Chung"). Gemini lỗi / hết lượt: thử lại 1 lần rồi trả 503 "AI đang quá tải" như gợi ý từ khóa. Dùng `GEMINI_MODEL` hiện có.

**Không làm trong C8:** tìm kiếm theo tên chủ đề, trang riêng cho từng chủ đề, chủ đề cho kênh, chip chủ đề cho khách chưa đăng nhập, tầng 3–4 như file gợi ý.

## 7. Kiểm tra
- Backend: test cho đọc `topics.json` (id không trùng, mục nào cũng có nhóm), kiểm tra `topicId` (thiếu, id lạ → 400; nháp không bắt buộc), `categories` có cả nhóm, cộng điểm `user_topic`, thứ tự chip chủ đề (điểm trước, phổ biến sau, tối đa 8, bỏ cái không còn video), feed `topic:`, AI hai bước (id lạ → nhóm + Chung). Cả bộ test phải qua.
- Client: `tsc`, `eslint`, `next build`; Chrome thật (API giả, chặn POST ghi dữ liệu): hai ô chọn (đổi nhóm thì ô dưới về "Chung", thiếu chủ đề thì không lưu được, nút Gợi ý điền đủ hai ô), bề rộng 375px, chip `topic:`.
- Sau deploy: curl `/api/topics` (200), `/api/ai/topic` chưa đăng nhập (401), log sạch. Anh thử: chọn chủ đề cho 4 video, xem / thích vài video, rồi xem hàng chip (TEST_CHECKLIST.md mục mới).
- Cỡ: **M** (khoảng 1 ngày). Thứ tự: `topics.json` cho anh xem trước → backend → client; deploy một đợt, backend trước client sau.

## 8. Cần anh quyết
1. **Quy tắc làm sạch ở mục 2** (bỏ nhóm 50 "Định dạng", mục "tổng hợp" thành "Chung", tách mục "/", giữ trùng giữa hai nhóm): anh đồng ý không?
2. **Tên ngắn cho chip** của các nhóm tên dài: em tự đề xuất trong `topics.json` để anh sửa, được không?
