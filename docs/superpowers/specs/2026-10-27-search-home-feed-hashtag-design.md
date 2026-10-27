# Thiết kế: tìm kiếm chính xác hơn, chip và gợi ý trang chủ, #thẻ (S1, C6, C7, T1)

Ngày: 2026-10-27. Trạng thái: **anh đã duyệt 27/10** (trả lời mục 7 bên dưới, chọn cách 2 cho S2).

Bốn việc anh chốt làm trước L1 (YOUTUBE_PARITY.md mục 3, bảng "Làm trước L1") và **S2** (từ khóa tìm kiếm do Gemini gợi ý, anh chọn cách 2 ngày 27/10, mục 1B). Em đề xuất làm và deploy theo **2 đợt**:
- **Đợt 1: T1 + S1 + S2.** Cả ba đều về tìm kiếm / thẻ. Có T1 trước thì anh gắn thẻ bằng cách gõ `#thẻ` vào mô tả, nên S1 / C6 / C7 có dữ liệu để thử.
- **Đợt 2: C7 + C6.** Chip "Tất cả" và "Chưa xem" dùng bộ gợi ý mới của C7.

---

## 1. S1: tìm kiếm chính xác hơn + "Video tương tự"

### 1.1 Hiện trạng (em đo bằng curl trên production 27/10, đọc `service/VideoSearchService.searchByTitleFull`)
| Gõ | Ra | Vì sao (em suy ra từ mapping, chưa chạy `explain` của ES) |
|---|---|---|
| `thien than` | "Ông-Putin-nói-gì…-đàm-phán-…" | `multi_match` có `fuzziness: AUTO` và chỉ cần **1 trong 2 từ** khớp: "than" cách "phan" 1 ký tự |
| `nga my` | "残酷な天使のテーゼ…Zankoku-na-Tenshi…" | "nga" cách "na" 1 ký tự; "my" không cần khớp |
| `putn` | video Putin (đúng) | "putn" cách "putin" 1 ký tự |

Thêm: mỗi kết quả đọc lại video từ MySQL bằng một câu `findById` riêng (N+1); `wildcard` `title: "<cả câu>*"` không bao giờ khớp khi câu có dấu cách (thừa nhưng vô hại).

### 1.2 Thay đổi
**a. Hai tầng kết quả.** Câu truy vấn chia thành:
- **Khớp chặt** (xếp trên): cả cụm khớp tiêu đề; hoặc **mọi từ** khớp ở tiêu đề / tên kênh / thẻ, chỉ cho sai chính tả với từ dài (`fuzziness: AUTO:5,8`: từ dưới 5 chữ phải đúng, 5–7 chữ sai 1, từ 8 chữ trở lên sai 2); hoặc mẩu 3 chữ khớp từ 80% (viết dính).
- **Khớp lỏng** (xếp dưới): như hiện nay (sai 1–2 ký tự với mọi từ, cả mô tả), **nhưng** bắt buộc khớp mọi từ khi câu có 1–2 từ, 75% số từ khi dài hơn (`minimum_should_match: "2<75%"`). Đây là chỗ sửa ca "thien than" / "nga my": từ thứ hai không khớp nên video bị loại.

Cách xếp: tầng chặt cộng một điểm cố định lớn (`constant_score` 1000), nên mọi kết quả chặt luôn đứng trên kết quả lỏng; trong cùng tầng xếp theo độ liên quan như cũ. Với bộ lọc "Ngày tải lên" / "Lượt xem" (C1): xếp theo tầng trước, rồi theo ngày / lượt xem trong từng tầng.

Dự kiến sau sửa: `thien than`, `nga my` → không có kết quả ("Không tìm thấy"); `putn`, `ambesa`, `cruel angle` vẫn ra đúng (tầng lỏng, vì mọi từ đều khớp gần đúng).

**b. "Video tương tự".** Chỉ ở trang 1 của tab "Tất cả" / "Video", dưới kết quả chính, tiêu đề "Video tương tự":
- API mới `GET /api/videos/search/similar?title=…` (công khai như tìm kiếm): lấy 3 kết quả đầu của chính câu tìm đó, chạy `more_like_this` trên `title`, `tags.text`, `description`, `channelName` (đặt `min_doc_freq: 1`, `min_term_freq: 1` vì kho video còn nhỏ), bỏ các video đã có trong trang kết quả, tối đa 8 video.
- Không có kết quả chính thì không có mục này (không có gì để "tương tự").
- Client: thêm một hàng lưới dưới kết quả ở `modules/home/ui/components/home-navbar/search-result.tsx`; rỗng thì ẩn.

**c. Hết N+1:** đọc các video của trang bằng một câu `findAllById`, xếp lại theo thứ tự của ES, vẫn lọc `VideoAccess.isListed` như cũ.

**Không làm** (anh đã bỏ): "Có phải bạn muốn tìm", tìm theo `#thẻ` trong ô tìm kiếm.

---

## 1B. S2: từ khóa tìm kiếm, Gemini gợi ý, chủ video duyệt

### Vì sao
Video chỉ tìm được qua tiêu đề, mô tả, tên kênh, thẻ. Tiêu đề tiếng Nhật / Anh mà người xem gõ tiếng Việt ("thiên thần" cho "Cruel Angel's Thesis") thì không ra, và tìm kiếm bằng từ khóa không tự dịch nghĩa. Anh chọn **cách 2**: Gemini gợi ý, chủ video xem và sửa trước khi lưu.

### Thay đổi
**Dữ liệu:** cột mới `videos.search_keywords` (TEXT, có thể NULL, các từ phân cách bằng dấu phẩy, tối đa 15 từ, mỗi từ tối đa 40 ký tự). Chỉ dùng để tìm kiếm: **không** thành thẻ, **không** hiện chip / `/hashtag`, **không** cộng điểm gợi ý (đó là lý do không dùng thẻ). Chủ video xem và sửa được, người xem không thấy.
- Runner khởi động `VideoSchemaInitializer` thêm cột (mỗi câu SQL một transaction, idempotent, đúng mẫu `CommentSchemaInitializer`).
- Chỉ mục ES: thêm trường `searchKeywords` (cùng analyzer `folded` + `.compact` như `channelName`) vào `videos_v2-mapping.json`. Chỉ mục `videos_v2` đã tồn tại trên production, nên lúc khởi động backend gọi **put mapping** thêm trường (ES cho phép thêm trường mới vào chỉ mục cũ), rồi `ElasticBootstrapLoader` đồng bộ lại như hiện nay. Không tạo / không thêm được thì tìm kiếm vẫn chạy như cũ, không có trường này.

**Gợi ý bằng Gemini:** `POST /api/ai/keywords` (cần đăng nhập, cùng `RateLimiter.Limit.AI_TEXT` với `/api/ai/description`), nhận `title`, `description`, `tags`; dùng `GeminiClient.generateText` (đã có). Yêu cầu Gemini trả đúng 10–15 từ khóa: tên dịch sang tiếng Việt và tiếng Anh, tên gốc viết latinh (romaji…), bản không dấu, cách gọi khác phổ biến; không bịa tên người / sự kiện không có trong dữ liệu đưa vào. Server cắt về tối đa 15 từ, bỏ trùng với tiêu đề / thẻ. Gemini lỗi hoặc hết lượt thì trả danh sách rỗng, không báo lỗi (chủ video tự nhập được).

**Client (Studio, chỗ sửa video và hộp thoại nháp):**
- Ô mới **"Từ khóa tìm kiếm"** cạnh ô Thẻ, dạng các chip có nút xoá + ô thêm, kèm dòng giải thích "Giúp video được tìm thấy bằng cách gõ khác (chỉ dùng để tìm kiếm, người xem không thấy)".
- Nút **"Gợi ý từ khóa"**: gọi API, **thêm** kết quả vào ô (không ghi đè từ khóa chủ video đã nhập), chủ video xoá cái không muốn rồi lưu cùng biểu mẫu như hiện nay. Không tự gợi ý, không tự lưu.
- Video đã đăng: dùng đúng ô này trong màn sửa video, nên không cần chạy hàng loạt cho video cũ.

**Backend lưu:** `POST /api/videos/drafts` và `PUT /api/videos/{id}` nhận thêm `searchKeywords`, làm sạch (viết thường, bỏ trùng, cắt độ dài) rồi lưu và đẩy sang ES (`VideoIndexer.toDocument`).

**Tìm kiếm:** trường `searchKeywords` nằm trong `multi_match` với trọng số thấp hơn tiêu đề (`searchKeywords^2`) và trong `match` `.compact`; được tính vào **tầng khớp chặt** (mục 1.2a) vì chủ video đã duyệt.

### Rủi ro
- Gemini có thể gợi ý sai (ngày 23/10 từng cho ra phiên âm khác nhau cho cùng một clip), nên không có đường "tự gắn không duyệt".
- Tên mô hình: dùng `GEMINI_MODEL` hiện có, không thêm cấu hình mới.

---

## 2. T1: #thẻ trong tiêu đề và mô tả

### 2.1 Nhận diện `#thẻ`
- `#` rồi 1–64 chữ / số / `_` (có dấu tiếng Việt), **không** đứng ngay sau chữ / số / `&` / `#` (để "C#", "&#39;" không thành thẻ). Thẻ chỉ có số (`#1`, `#2`) **không** tính, vì hay dùng để đánh số.
- `#` nằm trong link (`https://a.b/#phan-2`) không tính: link đã được nhận diện trước.
- Một hàm nhận diện ở client (`linkified-text.tsx`) và một ở backend, cùng quy tắc, có test cho các ca trên.

### 2.2 Backend: tự gộp vào thẻ của video
- Khi **đăng** và **sửa** video: lấy `#thẻ` trong tiêu đề + mô tả (tối đa 15 thẻ đầu tiên), viết thường, gộp với ô "Thẻ" người dùng nhập, bỏ trùng, rồi lưu như hiện nay (`addTags`), nên Elasticsearch, `/api/tags/popular` và gợi ý đều thấy.
- Tiêu đề và mô tả **giữ nguyên chữ** `#thẻ` (anh chốt).
- Hệ quả cần anh biết: ở Studio, xoá một thẻ khỏi ô "Thẻ" mà mô tả vẫn còn `#thẻ` đó thì lưu xong thẻ sẽ quay lại. Muốn bỏ hẳn thì xoá `#thẻ` trong mô tả.
- Video cũ không tự gộp; gộp khi chủ video bấm lưu lần tới. (Muốn gộp ngay cho video cũ thì thêm runner lúc khởi động; em không làm nếu anh không cần.)

### 2.3 Client
- `linkified-text.tsx`: thêm `#thẻ` → `Link` tới `/hashtag/{thẻ viết thường}`, cùng màu tím và kiểu chữ như `@handle`. Áp dụng ở mọi chỗ đang dùng: mô tả video, bình luận, hộp Giới thiệu kênh, chat phòng xem chung.
- **Tiêu đề trên trang xem** (`video-metadata.tsx`): `#thẻ` bấm được. Tiêu đề trên thẻ video ở trang chủ / tìm kiếm **để nguyên chữ** (cả thẻ đã là một link, không lồng link được; YouTube cũng vậy).
- `video-tags.tsx` (chip thẻ ở trang xem): mỗi chip thành link tới `/hashtag/{thẻ}`, bỏ trạng thái "đang chọn" vô nghĩa hiện nay.
- **Trang mới `/hashtag/[tag]`** trong `app/(home)/`: tiêu đề `#thẻ`, dòng "N video", lưới video từ `GET /api/videos/by-tag` (đã có, sắp theo lượt xem, chỉ video công khai), nút / cuộn "Tải thêm". Không có video thì "Chưa có video nào gắn #thẻ". Có `generateMetadata` (tiêu đề tab `#thẻ - WeTube`).

---

## 3. C7: gợi ý trang chủ "giống YouTube"

### 3.1 Hiện trạng (`service/video/impl/RecServiceImpl`, `interaction/impl/UserInteractionServiceImpl`)
- Một nguồn duy nhất: video có thẻ trùng thẻ người xem thích; điểm = 70% khớp thẻ + 30% độ mới; cố định; cache Redis 5 phút. Người xem không có điểm thẻ nào thì ra video mới nhất.
- Điểm thẻ chỉ cộng khi **bấm thẻ video ở trang chủ / tìm kiếm** (client gửi `POST /api/interactions` loại `VIEW`). Mở link trực tiếp, xem từ trang kênh / danh sách phát / Shorts không cộng.
- `UserInteractionServiceImpl` có điểm LIKE 5 / COMMENT 8 / SHARE 12 nhưng vòng cộng điểm nằm trong `if (type == VIEW)` (do lệch ngoặc), và cũng không nơi nào gửi các loại đó. Nên chưa từng được cộng.
- Không hạ video đã xem, không giới hạn số video mỗi kênh, gợi ý cả video của chính mình.

### 3.2 Thay đổi
**a. Cộng điểm thẻ ở backend, đúng lúc:**
- Xem: khi `POST /api/videos/{id}/view` **được tính** một lượt (đã có chống trùng 30 phút) và người xem đã đăng nhập → +9. Bỏ lời gọi `VIEW` ở client (`videoCard.tsx`, `search-result.tsx`) để không cộng hai lần.
- Thích video: +5 (trong `LikeServiceImpl`, chỗ đã gọi `evictRecommendations`); bỏ thích không trừ.
- Bình luận: +8 (bình luận đầu tiên của người đó trên video đó, để không "cày" điểm).
- Chia sẻ: +12, client gửi `SHARE` khi bấm "Sao chép" trong hộp Chia sẻ (API `POST /api/interactions` giữ nguyên, sửa lệch ngoặc).
- Không làm hỏng thao tác chính: cộng điểm lỗi thì chỉ ghi log.

**b. Nhiều nguồn ứng viên** (mỗi lần tạo danh sách, tổng tối đa ~200 như `pool-size` hiện nay):
| Nguồn | Lấy gì | Tối đa |
|---|---|---|
| Kênh đăng ký | video công khai mới nhất của các kênh đã đăng ký, 60 ngày gần đây | 60 |
| Thẻ yêu thích | như hiện nay (ES theo 20 thẻ điểm cao nhất) | 80 |
| Thịnh hành | nhiều lượt xem nhất trong số video đăng 30 ngày gần đây (ít video thì bỏ điều kiện 30 ngày) | 30 |
| Khám phá | video ngẫu nhiên không thuộc các nguồn trên (`random_score` của ES) | 30 |

**c. Chấm điểm** mỗi video (0–1 mỗi thành phần):
`điểm = 0,40·khớp thẻ + 0,20·kênh đã đăng ký + 0,15·độ mới + 0,10·độ phổ biến (log lượt xem) + 0,15·ngẫu nhiên`
- Phần ngẫu nhiên đổi mỗi lần tải lại trang (mục d), nên thứ tự đổi nhẹ nhưng video hợp vẫn ở trên.
- **Video đã xem hết** (đã xem ≥ 90% độ dài, đọc từ lịch sử xem): nhân 0,2, gần như luôn xuống cuối. Đang xem dở: giữ nguyên (YouTube vẫn gợi ý xem tiếp).
- **Bỏ video của chính mình.**

**d. Xếp lại cho đa dạng:** đi từ trên xuống, một kênh có tối đa **2 video trong mỗi 8 vị trí liên tiếp**; video vượt hạn mức bị lùi xuống, không bị bỏ (kho hiện có ít kênh, bỏ thì trang chủ trống).

**e. Làm mới:** client tạo một mã `seed` mỗi lần mở trang chủ và gửi kèm mọi trang (`?seed=`). Cache Redis theo `rec:{userId}:{seed}`, TTL 10 phút. Cuộn tải thêm thì thứ tự ổn định; tải lại trang thì ra thứ tự mới. Redis lỗi thì vẫn chạy (tính lại mỗi trang, thứ tự có thể xê dịch).

**f. Giữ nguyên:** chưa có lịch sử xem thì trang chủ trống "Hãy thử tìm kiếm để bắt đầu" (anh chọn "C" ngày 27/10).

**g. Lỗi em thấy khi đọc code (xem câu hỏi 5):** `GET /api/recommend/{userId}` công khai và nhận `userId` trên đường dẫn, nên ai biết id của người khác cũng xem được danh sách gợi ý của người đó (lộ sở thích). Đề xuất đổi thành lấy người dùng từ JWT, cần đăng nhập.

---

## 4. C6: chip trang chủ

### 4.1 Danh sách chip (từ trái sang)
| Chip | Hiện video gì | Ẩn khi |
|---|---|---|
| **Tất cả** (mặc định) | gợi ý C7 | không bao giờ |
| **Kênh đăng ký** | video mới của kênh đã đăng ký (`/api/feed/subscriptions` đã có) | chưa đăng ký kênh nào hoặc các kênh chưa có video |
| **Mới tải lên** | video công khai đăng trong 7 ngày gần đây, mới nhất trước, trừ video của mình | 7 ngày qua không có video mới |
| **Chưa xem** | gợi ý C7, bỏ hẳn video đã có trong lịch sử xem | không còn video nào chưa xem |
| **Đã xem** | video trong lịch sử xem, xếp theo điểm C7 (không hạ điểm "đã xem hết"), để xem lại | lịch sử trống |
| **Trực tiếp** | buổi phát đang diễn ra | **làm sau L1**, giờ chưa có |
| **Chip chủ đề** `#thẻ` | video có thẻ đó (`TagVideoGrid` + `/api/videos/by-tag` như hiện nay) | thẻ không còn video công khai |

**Chip chủ đề:** tối đa 8; trước tiên các thẻ người xem có điểm cao nhất (chỉ thẻ còn video công khai), thiếu thì thêm thẻ phổ biến (`/api/tags/popular`), bỏ trùng.

### 4.2 API và client
- API mới `GET /api/home/chips` (cần đăng nhập): trả danh sách chip đang có video, theo thứ tự trên. Mỗi chip kiểm tra bằng một câu "có tồn tại không" nhẹ; cache Redis 5 phút theo người.
- API mới `GET /api/home/feed?chip=SUBSCRIPTIONS|NEW|UNWATCHED|WATCHED&page=&size=&seed=` (cần đăng nhập), trả cùng kiểu `RecommendVideoDto` như gợi ý hiện nay để dùng lại `VideoCard`. "Tất cả" vẫn đi qua API gợi ý; chip chủ đề vẫn đi qua `by-tag`.
- Client `modules/home/ui/views/home-view.tsx`: `FilterCarousel` lấy chip từ `/api/home/chips` thay cho chỉ thẻ phổ biến; chip loại trả về lưới tương ứng. Lưới dùng chung cách tải thêm của `VideoGrid`. Hàng Shorts chỉ hiện ở chip "Tất cả" (như YouTube).
- Chưa có lịch sử xem (trang chủ trống "Hãy thử tìm kiếm") thì **không hiện hàng chip**, giống YouTube.
- Khách chưa đăng nhập: giữ như hiện nay ("Hãy đăng nhập…"), không có chip.

---

## 5. Thay đổi dữ liệu
**Đổi schema chỉ ở S2** (cột `videos.search_keywords` + trường ES `searchKeywords`, mục 1B; nên backup MySQL trước khi deploy đợt 1). S1, T1, C6, C7 không đổi schema; dùng bảng có sẵn: `user_tags` (điểm thẻ), `playlist_videos` của danh sách `HISTORY` (`historyDuration` = vị trí đã xem), `subscriptions`, `tags` / `video_tags`. Chỉ mục ES `videos_v2` giữ nguyên mapping (đã có `tags`, `channelId`, `totalView`, `createdAt`, `duration`).

## 6. Kiểm tra
**Backend (test tự động, chạy cả bộ test như mọi lần):**
- S1: hàm dựng câu truy vấn có đủ hai tầng và `minimum_should_match`; thứ tự `findAllById` giữ đúng thứ tự ES; video không công khai bị lọc.
- S2: làm sạch từ khóa (trùng, quá 15 từ, quá 40 ký tự); Gemini lỗi thì trả rỗng; câu truy vấn có `searchKeywords` ở tầng chặt.
- T1: nhận diện `#thẻ` (có dấu, `C#`, `&#39;`, `#1`, `#` trong link, quá 64 chữ, quá 15 thẻ); gộp với ô Thẻ, bỏ trùng.
- C7: chấm điểm (video đã xem hết xuống cuối, video của mình bị bỏ), xếp lại đa dạng (không kênh nào quá 2 trong 8 vị trí khi còn kênh khác; chỉ có một kênh thì vẫn đủ video), cùng `seed` ra cùng thứ tự; điểm LIKE / COMMENT / SHARE được cộng.
- C6: chip rỗng bị ẩn; "Chưa xem" không chứa video trong lịch sử.

**Sau deploy (curl, log):** các ca tìm kiếm ở mục 1.1 và `putn`, `ambesa`, `cruel angle`; `/search/similar`; `/hashtag/{thẻ}` trả 200; API mới trả 401 khi chưa đăng nhập; log không có `Exception`.

**Chrome thật ở máy (playwright, chặn mọi POST ghi dữ liệu):** `#thẻ` bấm được ở mô tả / tiêu đề / chip, trang `/hashtag`, mục "Video tương tự", hàng chip trang chủ.

**Cần anh thử (sẽ ghi vào TEST_CHECKLIST.md):** gõ `#thẻ` vào mô tả rồi lưu, thẻ xuất hiện ở trang xem và ở chip trang chủ; tải lại trang chủ vài lần thấy thứ tự đổi nhẹ; xem hết một video rồi quay lại trang chủ thấy nó xuống dưới; từng chip.

## 7. Quyết định của anh (27/10)
1. S1: không còn kết quả thì hiện "Không tìm thấy" ("nếu vậy cũng được").
2. C7: mỗi lần tải lại trang chủ **đổi thứ tự**.
3. C6: chip "Mới tải lên" lấy **7 ngày**.
4. T1: `#thẻ` của video cũ **chỉ gộp khi chủ video lưu lại** (không viết runner).
5. Lỗi `/api/recommend/{userId}` công khai: **sửa ngay**, đã deploy (`fa1e6d9`).
6. S2: cách 2 (Gemini gợi ý, chủ video duyệt).
