# E2: Duyệt / sửa phụ đề AI trước khi đăng (thiết kế, anh duyệt 29/10)

**Anh chốt 29/10:** (1) bấm Đăng video thì phụ đề đã tạo xong (bản nháp) công khai cùng video, phụ đề còn đang tạo thì ở lại bản nháp để duyệt trong Studio; (2) trình sửa có ở cả hộp thoại tải lên và Studio; (3) sửa được chữ, giờ, xoá / thêm câu, có câu gốc để đối chiếu.

PRD luồng 1 bước 4: "Creator xem trước bản phụ đề, chỉnh sửa câu từ (nếu muốn) và nhấn Xuất bản"; 4.1.2: "Creator có quyền kiểm tra, chỉnh sửa bản dịch trước khi công bố".

## Hiện trạng (em đọc code 29/10)
- AI chỉ được gọi **sau khi** bấm Đăng (`upload-wizard.tsx` `publish`, Studio `form-section.tsx` `save`, menu "..." ở trang xem). `SubtitleAiService` chạy nền, xong thì upload `.vtt` lên Cloudinary và thêm dòng `video_subtitles` (`language`, `subtitle_url`): **người xem thấy ngay**, chủ kênh không xem trước, không sửa được.
- Không có trạng thái: client không biết AI đang chạy, xong hay lỗi (lỗi chỉ nằm trong log).
- Tạo lại cùng ngôn ngữ thì thêm **dòng trùng** (cùng một file Cloudinary).
- Không có API sửa / xoá phụ đề.
- Gemini đã trả cả câu gốc (`original`) lẫn câu dịch (`translated`) nhưng code bỏ câu gốc.

## Đề xuất

### Dữ liệu (backend)
Thêm vào `video_subtitles` (runner idempotent kiểu `CommentSchemaInitializer`):
- `status` VARCHAR: `PROCESSING` → `DRAFT` (AI xong, chưa công khai) → `PUBLISHED`; `FAILED` (kèm `error_message`). Dòng cũ = `PUBLISHED`.
- `cues` MEDIUMTEXT: JSON `[{start, end, text, original}]` (giây, số thực), là bản để sửa.
- `ai_generated` BOOLEAN, `updated_at`.
- Mỗi video mỗi ngôn ngữ một dòng: tạo lại thì ghi đè dòng cũ (hỏi xác nhận ở client nếu dòng đó đang `PUBLISHED`).
- `subtitle_url` chỉ có khi `PUBLISHED`: lúc công khai, server dựng VTT từ `cues` rồi upload Cloudinary (URL có số version nên sửa xong không dính cache).

### API (chỉ chủ video, `OwnershipGuard`)
| API | Việc |
|---|---|
| `POST /api/videos/{id}/subtitles/ai?targetLang=` | Như cũ, nhưng tạo ngay dòng `PROCESSING` và trả về; AI xong thì `DRAFT`, lỗi thì `FAILED` |
| `GET /api/videos/{id}/subtitles/manage` | Mọi phụ đề kèm trạng thái (client hỏi lại mỗi 5 giây khi còn `PROCESSING`) |
| `GET /api/videos/{id}/subtitles/{subtitleId}` | Lấy `cues` để sửa |
| `PUT /api/videos/{id}/subtitles/{subtitleId}` | Lưu `cues` đã sửa (kiểm tra: start < end, không âm, tối đa 3000 câu, 500 ký tự / câu); đang công khai thì upload lại VTT ngay |
| `POST …/{subtitleId}/publish` / `…/unpublish` | Công khai / ẩn khỏi người xem |
| `DELETE …/{subtitleId}` | Xoá |

(Khi code đổi `{lang}` thành `{subtitleId}`: prod có thể đã có dòng trùng ngôn ngữ từ trước. Phụ đề cũ không có `cues`: trình sửa đọc file `.vtt` làm bản để sửa.)

Người xem (`getVideoDetail`) chỉ nhận phụ đề `PUBLISHED`.

### Giao diện (client)
- **Hộp thoại tải lên, bước "Các thành phần":** chọn ngôn ngữ thì gọi AI **ngay** (bản nháp đã có khi tệp tải xong), hiện "Đang tạo phụ đề…" trong lúc anh điền tiếp các bước khác. Xong thì có nút **"Xem và sửa"**. Lỗi thì hiện lý do + "Thử lại".
- **Trình sửa phụ đề** (hộp thoại lớn): trái là video, phải là danh sách câu (giờ bắt đầu, giờ kết thúc, câu dịch có thể sửa, câu gốc in mờ bên dưới để đối chiếu). Bấm vào câu thì video tua tới đó; câu đang phát được tô sáng; phụ đề hiện trên video theo chữ đang sửa. Sửa được chữ và giờ, xoá câu, thêm câu sau câu đang chọn. Nút **Lưu nháp**.
- **Studio → Chi tiết video:** mục "Phụ đề" liệt kê từng ngôn ngữ + trạng thái (Đang tạo / Bản nháp / Đã công khai / Lỗi) với các nút Sửa, Công khai / Ẩn, Xoá, và "Tạo phụ đề AI".
- Menu "..." ở trang xem: vẫn tạo phụ đề AI, nhưng thông báo đổi thành "Phụ đề sẽ ở dạng bản nháp, vào Studio để xem và công khai" kèm link.

### Kiểm tra
- Backend: test cho dựng VTT từ `cues`, kiểm tra `cues` sai, chuyển trạng thái, người không phải chủ video bị 403, người xem không thấy `DRAFT`; `compileJava compileTestJava test`.
- Client: `tsc`, `eslint`, `next build`; Chrome thật (playwright, API giả lập) cho trình sửa và mục Studio.
- Sau deploy: curl các API mới khi chưa đăng nhập (401); anh thử tạo phụ đề thật với Gemini.

### Ngoài phạm vi
Tải lên file `.vtt` / `.srt` có sẵn; một lần tạo nhiều ngôn ngữ; dịch lại từ bản đã sửa sang ngôn ngữ khác; lịch sử chỉnh sửa.
