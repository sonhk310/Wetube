# Thiết kế: chủ đề video (C8)

Ngày: 2026-10-27. Trạng thái: **nháp, chờ anh duyệt** (trả lời mục 8).

## 1. Vì sao
Chip chủ đề lấy từ hashtag ra tên vụn, không dấu cách, trùng ý ("nhacanime", "animevn", "nhạcphim"), nên đã bỏ ngày 27/10 (commit `2585c15`). YouTube dùng chủ đề có **tên chuẩn** ("Trò chơi", "Trò chơi hành động phiêu lưu", "Bóng đá", "Podcast"). C8 làm lại chip chủ đề theo cách đó:
- Danh sách chủ đề **cố định, 2 tầng** (anh chọn 27/10), mình soạn; không ai tự tạo chủ đề.
- Chủ video chọn chủ đề bằng **dropdown** khi tải lên và khi sửa video (ý anh 27/10).
- Thẻ (`#thẻ`) giữ nguyên vai trò: `/hashtag`, tìm kiếm, gợi ý "Tất cả". Thẻ không làm chip nữa.

Tài liệu tham khảo anh đưa: `youtube-topic-taxonomy.md` (52 nhóm, tới 4 tầng). Em rút gọn thành danh sách nháp ở mục 2, theo đúng hai quy tắc trong tài liệu đó: tách **chủ đề** khỏi **định dạng** (Shorts, livestream, vlog, reaction không phải chủ đề), và dùng **id ổn định** chứ không dùng tên làm khoá.

## 2. Danh sách chủ đề (nháp, anh gạch / thêm / đổi tên)
Id dạng `cha` và `cha.con`, không bao giờ đổi sau khi đã có video dùng (đổi tên hiển thị thì được). Video chọn được cả chủ đề cha (vd. video âm nhạc chung chung) lẫn chủ đề con.

| Chủ đề (id) | Chủ đề con |
|---|---|
| Âm nhạc (`music`) | Nhạc Việt · K-pop · Nhạc Âu Mỹ · Nhạc anime · Nhạc phim · Nhạc game · Rap / Hip-hop · Lo-fi và thư giãn · Nhạc cụ, cover |
| Trò chơi (`gaming`) | Liên Minh Huyền Thoại · Minecraft · Game di động · Game nhập vai · Game hành động phiêu lưu · Game bắn súng · Thể thao điện tử |
| Anime và truyện tranh (`anime`) | Review anime · AMV · Manga, truyện tranh · Cosplay |
| Phim và truyền hình (`film`) | Review phim · Tóm tắt phim · Phim Việt · Phim Hàn · Hoạt hình · Trailer |
| Thể thao (`sports`) | Bóng đá · Bóng rổ · Cầu lông · Võ thuật, MMA · Thể hình, gym · Đua xe, F1 |
| Tin tức (`news`) | Thời sự trong nước · Thời sự thế giới · Kinh tế · Pháp luật |
| Công nghệ (`tech`) | Điện thoại · Máy tính, linh kiện · Lập trình · Trí tuệ nhân tạo · Đánh giá sản phẩm |
| Giáo dục và khoa học (`education`) | Tiếng Anh · Ngoại ngữ khác · Toán, khoa học · Lịch sử · Vũ trụ · Kỹ năng |
| Hài và giải trí (`comedy`) | Hài, tiểu phẩm · Thử thách · Gameshow |
| Podcast (`podcast`) | Trò chuyện · Phỏng vấn · Kể chuyện, true crime |
| Ẩm thực (`food`) | Nấu ăn · Món Việt · Ăn uống, mukbang · Làm bánh · Review quán |
| Du lịch (`travel`) | Du lịch Việt Nam · Du lịch nước ngoài · Phượt, cắm trại |
| Đời sống (`lifestyle`) | Thời trang · Làm đẹp · Sức khỏe · Gia đình, nuôi dạy con · Thú cưng |
| Kinh doanh và tài chính (`finance`) | Đầu tư, chứng khoán · Tiền điện tử · Tài chính cá nhân · Khởi nghiệp · Marketing |
| Xe cộ (`auto`) | Ô tô · Xe máy · Xe điện |
| Thiếu nhi (`kids`) | Hoạt hình thiếu nhi · Bài hát thiếu nhi |

16 chủ đề cha, khoảng 75 chủ đề con. Ít mục thì dropdown gọn, chip không vụn; thiếu thì thêm sau (thêm mục mới không ảnh hưởng video cũ).

Danh sách nằm ở **một file** `WeTube-service-dev/src/main/resources/topics.json` (id, tên, id cha). Backend đọc lúc khởi động; client lấy qua `GET /api/topics` (công khai, cache lâu). Không làm bảng trong DB vì không ai sửa danh sách lúc chạy: đổi danh sách = sửa file + deploy.

## 3. Bài toán "một video nhiều chủ đề"
Đây là chỗ anh nêu. Em đề xuất:

**a. Tối đa 3 chủ đề, ngang hàng, không bắt buộc.** Vd. video "Nhạc phim Your Name" chọn `Nhạc anime` + `Nhạc phim`; video review game LMHT chọn `Liên Minh Huyền Thoại` + `Thể thao điện tử`. Không có "chủ đề chính": chip và điểm yêu thích (mục 5) coi các chủ đề như nhau, nên không cần phân biệt chính / phụ. Video không chọn chủ đề nào vẫn đăng bình thường, chỉ không xuất hiện trong chip chủ đề.

**b. Chủ đề con kéo theo chủ đề cha.** Chọn `Nhạc anime` thì video tự thuộc `Âm nhạc` (chip "Âm nhạc" có video này). Chỉ **lưu** chủ đề chủ video đã chọn; chủ đề cha được thêm lúc đẩy sang Elasticsearch. Hệ quả:
- Chọn cha và con cùng nhánh (`Âm nhạc` + `Nhạc anime`) là thừa: dropdown tự bỏ chủ đề cha khi chọn một chủ đề con của nó, nên 3 chỗ không bị phí.
- Đổi danh sách (chuyển một chủ đề con sang cha khác) thì video cũ tự theo, chỉ cần đồng bộ lại ES.

**c. Cách khác (nếu anh không muốn a):**
- **Một chủ đề duy nhất** (như "Danh mục" của YouTube Studio): dropdown thường, đơn giản nhất, nhưng video "Nhạc phim anime" phải chọn một trong hai.
- **Một chủ đề chính + tối đa 2 phụ:** thêm một bước chọn, chỉ có lợi nếu sau này cần xếp hạng theo chủ đề chính. Em chưa thấy chỗ nào cần.

## 4. Chọn chủ đề ở Studio (tải lên, sửa video, hộp thoại nháp)
- Ô **"Chủ đề"** cạnh ô Thẻ / Từ khóa tìm kiếm (cùng ba chỗ đang có ô Từ khóa: `upload-wizard.tsx`, `form-section.tsx`, `draft-edit-dialog.tsx`).
- Bấm ô thì mở dropdown 2 tầng, dùng `components/ui/dropdown-menu.tsx` có sẵn (Radix): mỗi chủ đề cha là một mục mở **menu con**; trong menu con có dòng "Tất cả *Âm nhạc*" (chọn chủ đề cha) và các chủ đề con, mỗi dòng có **ô tích**. Chọn xong dropdown vẫn mở để tích tiếp. Đủ 3 thì các dòng còn lại mờ đi, kèm dòng "Tối đa 3 chủ đề".
- Chủ đề đã chọn hiện thành chip có nút xoá dưới ô (giống ô Từ khóa), tên đầy đủ có dấu, vd. "Âm nhạc › Nhạc anime".
- Trên điện thoại menu con của Radix vẫn dùng được (bấm để mở), nhưng hẹp; nếu anh thấy khó dùng thì đổi sang danh sách cuộn có tiêu đề nhóm trong cùng một khung. Em sẽ thử bằng Chrome ở bề rộng 375px trước khi báo.
- **(Tuỳ anh, mục 8 câu 3)** Nút **"Gợi ý chủ đề"**: Gemini đọc tiêu đề, mô tả, thẻ, từ khóa rồi **tích sẵn** 1–3 chủ đề trong dropdown; chủ video đổi được rồi lưu cùng biểu mẫu. Không tự gợi ý, không tự lưu (như S2).
- Video cũ (kho có 4 video): chủ video mở sửa rồi chọn, không chạy hàng loạt.

## 5. Chip trang chủ và điểm yêu thích chủ đề
- **Điểm:** bảng mới `user_topic` (user_id, topic_id, point), giống `user_tag`. Cộng điểm ở **đúng những chỗ đang cộng điểm thẻ** (`TagAffinityService.addQuietly`: xem được tính +9, thích +5, bình luận gốc đầu tiên +8, Sao chép ở hộp Chia sẻ +12; trần 1000) cho mọi chủ đề của video, gồm cả chủ đề cha. Lỗi chỉ ghi log như hiện nay.
- **Chip:** sau 4 chip cố định (Kênh đăng ký · Mới tải lên · Chưa xem · Đã xem), thêm tối đa 8 chip chủ đề: chủ đề người xem có điểm cao trước, rồi chủ đề có nhiều video công khai nhất (đếm bằng aggregation `terms` trên trường `categories` của ES). Chỉ hiện chủ đề **còn ít nhất 1 video công khai**. Nhãn là tên chuẩn có dấu ("Nhạc anime", "Bóng đá").
- Chip cha và chip con cùng nhánh có thể cùng hiện (YouTube cũng vậy: "Trò chơi" và "Trò chơi hành động phiêu lưu").
- **Bấm chip:** `GET /api/home/feed?chip=topic:music.anime` (thêm loại chip `topic:` vào `HomeFeedService.feed`): video công khai thuộc chủ đề đó, xếp như "Mới tải lên" (mới nhất trước), tải thêm khi cuộn. Client: nhánh `tag:` còn sót trong `home-view.tsx` đổi thành `topic:`, dùng lại `chipVideoGrid.tsx`.
- Gợi ý "Tất cả" (`FeedRanker`) **không đổi** trong C8: vẫn chấm theo thẻ. Thêm điểm khớp chủ đề vào đó là việc sau, khi kho đủ video có chủ đề để so.

## 6. Thay đổi dữ liệu
- **MySQL:** cột `videos.topic_ids` (VARCHAR(200), NULL, id phân cách dấu phẩy, tối đa 3) theo đúng kiểu `search_keywords`; bảng `user_topic`. Thêm bằng runner idempotent lúc khởi động (mẫu `CommentSchemaInitializer`, mỗi câu SQL một transaction), dù `ddl-auto=update` cũng tự tạo.
- **Backend lưu:** `POST /api/videos/drafts` và `PUT /api/videos/{id}` nhận thêm `topicIds`; bỏ id không có trong danh sách, bỏ trùng, gộp cha khi đã có con (mục 3b), cắt còn 3. `GET` chi tiết video ở Studio trả `topicIds` (`VideoFormDetailDto`).
- **Elasticsearch:** trường `categories` (keyword) **đã có** trong `videos_v2-mapping.json` và trên production, đang luôn rỗng. `VideoIndexer.toDocument` ghi vào đó các chủ đề đã chọn + chủ đề cha. Không cần put mapping; `ElasticBootstrapLoader` đồng bộ lại lúc khởi động như hiện nay.
- **Gemini (nếu anh chọn có):** `POST /api/ai/topics` (đăng nhập, cùng hạn mức `RateLimiter.Limit.AI_TEXT`), nhận tiêu đề, mô tả, thẻ, từ khóa; prompt đưa sẵn danh sách id + tên, yêu cầu trả 1–3 id. Server bỏ id lạ. Gemini lỗi / hết lượt: thử lại 1 lần rồi trả 503 "AI đang quá tải" như gợi ý từ khóa.

**Không làm trong C8:** tìm kiếm theo tên chủ đề (gõ "bóng đá" ra video chủ đề Bóng đá dù tiêu đề không có chữ đó), trang riêng cho từng chủ đề, chủ đề cho kênh, chủ đề cho người xem chưa đăng nhập.

## 7. Kiểm tra
- Backend: test cho việc làm sạch `topicIds` (id lạ, trùng, cha + con, quá 3), chủ đề cha được thêm vào `categories`, cộng điểm `user_topic`, thứ tự chip chủ đề (điểm trước, phổ biến sau, tối đa 8, bỏ chủ đề không còn video), feed `topic:`. Cả bộ test phải qua.
- Client: `tsc`, `eslint`, `next build`; Chrome thật (API giả, chặn POST ghi dữ liệu) cho dropdown (tích, bỏ tích, giới hạn 3, chọn con bỏ cha, bề rộng 375px) và chip `topic:`.
- Sau deploy: curl `/api/topics` (200), `/api/ai/topics` chưa đăng nhập (401), log sạch. Anh thử: gắn chủ đề cho 4 video, xem / thích vài video, rồi xem hàng chip (TEST_CHECKLIST.md mục mới).
- Cỡ: **M** (khoảng 1 ngày), làm một đợt, backend trước client sau.

## 8. Cần anh quyết
1. **Nhiều chủ đề:** (a) tối đa 3, ngang hàng, không bắt buộc *(em đề xuất)*; (b) chỉ 1 chủ đề; (c) 1 chính + tối đa 2 phụ. Có bắt buộc chọn ít nhất 1 chủ đề khi đăng không? *(em đề xuất: không)*
2. **Danh sách mục 2:** anh gạch / thêm / đổi tên. Có cần chủ đề con cụ thể theo game / đội bóng (vd. Valorant, Genshin) không, hay để thẻ lo phần cụ thể đó?
3. **Vai trò của chủ video và AI:** (a) chủ video tự chọn trong dropdown, có nút "Gợi ý chủ đề" tích sẵn *(em đề xuất)*; (b) chỉ dropdown, không có AI (ít việc hơn, bỏ được `/api/ai/topics`); (c) chỉ duyệt gợi ý của AI.
4. **Dropdown:** menu con theo chủ đề cha *(em đề xuất, dùng component có sẵn)*, hay một danh sách cuộn có ô tìm kiếm (phải thêm component Popover/Command mới)?
