# WeTube: cách làm việc

Dự án: "YouTube của riêng mình". Đọc `YOUTUBE_PARITY.md` trước khi làm bất cứ gì: đó là nguồn sự thật duy nhất về việc đã làm (gói 1, 2, …), việc dở dang, lộ trình A–G và cách deploy.

## Cấu trúc thư mục
```
E:\Wetube
├── CLAUDE.md, YOUTUBE_PARITY.md   cách làm việc; việc đã làm / dở dang / lộ trình
├── PRD.md, BRD.md                 yêu cầu gốc
├── TEST_CHECKLIST.md              việc anh tự test khi đăng nhập (mục 1–15)
├── WeTube-service-dev/            BACKEND: Spring Boot 3, Java, MySQL, Redis, Elasticsearch
├── WeTube-client-app-dev/         CLIENT: Next.js 15 (App Router), React, Tailwind v4, RTK Query, zustand
└── WeTube-admin-app-dev/          app admin cũ (Vite). KHÔNG còn deploy; quản trị nằm ở client /admin/reports
```

### Backend: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/`
| Thư mục | Chứa gì |
|---|---|
| `controller/` | REST API `/api/...` (VideoController, CommentController, ReportController, SearchHistoryController…) |
| `service/` | Logic: chia theo mảng (`video/`, `interaction/` (bình luận, thích), `channel/` (đăng ký), `notification/`, `report/`, `AI/` (Gemini)…); `impl/` là phần cài đặt. `RedisService`, `CloudinaryService`, `VideoIndexer` (Elasticsearch) nằm ngay ở `service/` |
| `entity/`, `repository/`, `dto/`, `mapper/` (MapStruct) | JPA entity, Spring Data repository, đối tượng trả về, chuyển đổi |
| `security/` | Quyền: `CurrentUser` (lấy user từ JWT), `OwnershipGuard` (chủ sở hữu), `VideoAccess` (ai xem được video nào), `RateLimiter` (429) |
| `job/` | Runner lúc khởi động: tạo bảng / cột (`*Initializer`), seed admin / role, đồng bộ Elasticsearch |
| `configuration/` | `SecurityConfig` (route công khai / cần đăng nhập), `JacksonConfig` (giờ UTC), CORS, Cloudinary, Elasticsearch, LiveKit, MoMo, WebSocket |
| `exception/` | `GlobalExceptionHandler`: map lỗi sang 400 / 404 / 405 / 413 / 500 |
| `src/main/resources/` | `application.yml`, `application-prod.yml` (biến môi trường của Vibe Host ghi đè) |

### Client: `WeTube-client-app-dev/src/`
| Thư mục | Chứa gì |
|---|---|
| `app/` | Route (App Router). Trang chủ, kênh, feed, playlists, search, shorts, admin ở `app/(home)/`; khám phá (âm nhạc, thể thao, tin tức, game) ở `app/(explore)/`; còn có `watch/[videoId]` (server component có `generateMetadata`, giao diện ở `watch-page.tsx`), `studio/`, `rooms/` (phòng xem chung), `livestream/`. `layout.tsx` gốc gắn Providers và 2 bộ toast |
| `app/api/` | RTK Query API (`videoApi`, `commentApi`, `likeApi`, `subscriptionsApi`…), đăng ký trong `store/store.ts` |
| `modules/` | Giao diện theo tính năng. Hay sửa nhất: `watch/components/` (trình phát `video/`, bình luận `comments/`, `video-metadata.tsx`, `share-dialog.tsx`), `home/ui/components/` (navbar, ô tìm kiếm, chuông, sidebar, thẻ video), `studio/`, `channel/`, `reports/` |
| `components/` | Component dùng chung; `components/ui/` là shadcn (Radix) |
| `lib/` | Tiện ích: `apiFetch` (fetch kèm cookie), `apiError`, `toastEmitter`, `utils` (`timeAgo`, `formatCompact`), `watchProgress`, `searchHistory`, `watchSeek` |
| `store/`, `contexts/`, `hooks/`, `types/`, `constants/` | Redux store + zustand (`store/zustand/`), AuthContext (`useAuth`), hook, kiểu TS, `API_PREFIX` |

## Giao tiếp
- Trả lời bằng tiếng Việt, xưng **em**, gọi **anh**.
- Luôn nói rõ cái gì **đã kiểm tra thật** (build, curl, log) và cái gì **chưa kiểm tra**. Session không có trình duyệt, nên giao diện phải nhờ anh thử. Mỗi lần xong thì kèm danh sách ngắn "anh thử giúp em".
- Anh gửi ảnh báo lỗi thì bắt đầu từ ảnh đó: tìm nguyên nhân thật (đọc code, log `get_runtime_logs`), không đoán.
- Gặp lỗi khác trong lúc làm thì báo luôn, kể cả lỗi anh chưa thấy.

## Nguyên tắc viết code (andrej-karpathy-skills)
Dịch từ [forrestchang/andrej-karpathy-skills](https://github.com/forrestchang/andrej-karpathy-skills): các nguyên tắc giảm lỗi hay gặp khi LLM viết code. Chúng nghiêng về thận trọng hơn là nhanh; việc quá nhỏ thì tự cân nhắc.

### 1. Nghĩ trước khi code
**Không tự giả định. Không giấu chỗ chưa hiểu. Nói rõ các đánh đổi.**
- Nói rõ các giả định trước khi làm. Không chắc thì hỏi.
- Yêu cầu hiểu được theo nhiều cách thì nêu ra các cách, không lặng lẽ chọn một.
- Có cách đơn giản hơn thì nói. Phản biện khi cần.
- Có gì chưa rõ thì dừng lại, gọi tên chỗ chưa rõ, rồi hỏi.

### 2. Đơn giản trước tiên
**Ít code nhất mà vẫn giải quyết được vấn đề. Không làm thứ "phòng khi cần".**
- Không thêm tính năng ngoài yêu cầu.
- Không tạo lớp trừu tượng cho code chỉ dùng một lần.
- Không thêm "linh hoạt" hay "tuỳ chỉnh" không ai yêu cầu.
- Không xử lý lỗi cho tình huống không thể xảy ra.
- Viết 200 dòng mà làm được bằng 50 thì viết lại.

Tự hỏi: "Một kỹ sư lâu năm có thấy chỗ này rườm rà không?" Có thì đơn giản lại.

### 3. Sửa đúng chỗ cần sửa
**Chỉ động vào chỗ bắt buộc. Chỉ dọn phần mình bày ra.**
- Không "cải thiện" code, comment hay định dạng bên cạnh.
- Không refactor thứ đang chạy tốt.
- Theo đúng phong cách code sẵn có, kể cả khi mình sẽ viết khác.
- Thấy code chết không liên quan thì nói, không xoá.
- Xoá import / biến / hàm mà chính thay đổi của mình làm thành thừa; không xoá code chết có sẵn trừ khi được yêu cầu.

Phép thử: mỗi dòng thay đổi phải truy ngược được về yêu cầu của anh.

*Ghi chú cho dự án:* lỗi thật trên tính năng đã deploy mà em phát hiện trong lúc làm thì vẫn theo bảng ở mục "Khi nào được push và deploy" (sửa, tách commit riêng, báo anh). Còn dọn dẹp hay cải thiện không phải lỗi thì chỉ nêu ra, không tự làm.

### 4. Làm theo mục tiêu kiểm chứng được
**Đặt tiêu chí thành công. Lặp cho tới khi kiểm chứng được.**
- "Thêm kiểm tra dữ liệu" → viết test cho dữ liệu sai, rồi làm cho test qua.
- "Sửa lỗi" → viết test tái hiện lỗi, rồi làm cho test qua.
- "Refactor X" → đảm bảo test qua cả trước và sau.

Việc nhiều bước thì nêu kế hoạch ngắn:
```
1. [Bước] → kiểm tra: [cách kiểm tra]
2. [Bước] → kiểm tra: [cách kiểm tra]
```

*Ghi chú cho dự án:* client chưa có test tự động. Tiêu chí kiểm chứng ở đây là các bước trong mục "Kiểm tra trước khi commit", curl API, log sau deploy, và với lỗi giao diện thì tái hiện được trên trình duyệt (anh thử, hoặc Claude in Chrome nếu có).

Các nguyên tắc này đang có tác dụng nếu: diff ít thay đổi thừa hơn, ít phải viết lại vì làm quá phức tạp, và câu hỏi làm rõ đến trước khi code chứ không phải sau khi đã sai.

## Khi nào được push và deploy
| Loại việc | Làm gì |
|---|---|
| Sửa lỗi anh vừa báo trên tính năng đã deploy | Sửa, push, deploy luôn rồi báo |
| Lỗi em tự phát hiện trên tính năng đã deploy | Như trên, nhưng tách commit riêng |
| Tính năng mới (mục A–G) | Commit ở máy, **hỏi anh trước khi push và deploy** |

- Bản sửa lỗi và tính năng mới không đi chung một lần push. Nếu tính năng chưa duyệt đã nằm trên `main` ở máy thì tách nó sang nhánh tạm (`git branch <tên> <commit>` rồi `git reset --keep HEAD~1`), deploy bản sửa, sau đó `cherry-pick` lại khi anh duyệt.
- Deploy **backend trước, client sau**.
- Bộ phân quyền tự động chặn `redeploy_project` cho tới khi anh nói rõ trong chat.
- Commit message tiếng Anh, có dòng `Co-Authored-By`. Push thẳng `main` (Vibe Host theo dõi nhánh này).

## Kiểm tra trước khi commit
- **Backend** (`WeTube-service-dev`): `./gradlew --no-daemon compileJava compileTestJava -q` (Gradle 8.14.3, giống production)
- **Client** (`WeTube-client-app-dev`):
  - `npx tsc --noEmit`: bỏ qua 12 lỗi TS7006 cũ, không được có lỗi mới. Vibe Host build với `ignoreBuildErrors`, nên lỗi kiểu không chặn build: phải tự chạy tsc.
  - `npx eslint <file đã sửa>`
  - `npx next build`
- Khi cần xem HTML thật (vd. thẻ chia sẻ): `npx next start -p 3107` rồi curl, xong thì tắt server.

## Deploy (Vibe Host)
- Backend `cmvkfhypr07ra0i5f5b51wed1`: https://wetube-service.vibe.tinhgon.xyz
- Client `cmvkg1ilo084w0i5fhrmxxz90`: https://wetube.vibe.tinhgon.xyz
- Các bước: `git push`, `redeploy_project`, gọi `wait_for_job` lặp lại cho tới khi xong (mỗi lần chờ tối đa 25 giây, cả lượt mất khoảng 3 phút). Lỗi 520 / ECONNRESET của cổng Vibe Host là lỗi tạm thời, gọi lại là được.
- Sau deploy:
  - Đọc log (`get_runtime_logs`, lọc `Exception`). Các dòng `ERROR … UnauthorizedEntryPoint` là request chưa đăng nhập bị trả 401, không phải lỗi.
  - Curl API mới (khi chưa đăng nhập phải trả 401 nếu API cần đăng nhập) và các trang client (phải trả 200).
- Log runtime chỉ giữ phần đuôi: stack trace dài có thể mất dòng thông báo lỗi.

## Sau mỗi gói / mỗi bản sửa
Cập nhật `YOUTUBE_PARITY.md`:
- Mô tả việc đã làm, commit, giờ deploy.
- Đã kiểm tra gì, chưa kiểm tra gì.
- Đánh ✅ ở bảng lộ trình.

Anh thử OK thì ghi "Anh đã thử trên trình duyệt: OK".

## Bẫy đã gặp
- **DB:** prod thực tế chạy `ddl-auto=update` (biến `SPRING_JPA_HIBERNATE_DDL_AUTO` ghi đè `application-prod.yml`). Hibernate tự thêm bảng / cột nhưng không đổi kiểu cột (ENUM) và không tạo index. Vì vậy mọi thay đổi schema vẫn viết thêm runner idempotent lúc khởi động (mẫu `job/CommentSchemaInitializer.java`: mỗi câu SQL một transaction riêng). Cột `videos_status` là ENUM: chỉ nối thêm giá trị vào cuối.
- **Thời gian:** mọi thời gian trong JSON có dạng UTC `…Z` (`configuration/JacksonConfig.java`); JVM chạy UTC.
- **Tailwind v4** chỉ đọc `globals.css`, không đọc `tailwind.config.ts`.
- **Layout:**
  - Thanh cuộn nằm ở `html`, không phải `body`.
  - Phần tử `absolute` phải nằm trong khung `relative`.
  - `DialogContent` là grid: nội dung cuộn ngang cần `grid-cols-[minmax(0,1fr)]`.
- **Radix Select + `form.reset()`:** có thể gọi `onValueChange("")`, nên bỏ qua giá trị rỗng.
- **RTK Query:**
  - Cache có thể làm dữ liệu cũ (`getVideoDetail` đã đặt `keepUnusedDataFor: 0`).
  - API backend trả chữ thường (không phải JSON) thì phải thêm `responseHandler: "text"`, nếu không `unwrap()` sẽ báo lỗi dù server đã làm xong.
- **Toast:** có cả `react-toastify` (`lib/toastEmitter.ts`) lẫn `react-hot-toast`. Cả hai đều đã được gắn trong `app/layout.tsx`, nhưng code mới nên dùng `toastEmitter`.
- **Rate limit** (`security/RateLimiter.java`): vượt giới hạn thì trả 429 kèm `message` tiếng Việt. Client hiện lỗi bằng `lib/apiError.ts`.
- **Redis** chỉ dùng làm cache. Mọi key ghi qua `RedisService` đều có TTL; Redis lỗi thì code tự chạy tiếp không cần Redis.
