# Thiết kế: tìm kiếm "rộng" như YouTube (mở rộng C1)

Ngày: 2026-10-24. Trạng thái: anh đã duyệt toàn bộ (chat 24/10 tối), kể cả chỉ mục kênh trong Elasticsearch.

## 1. Vì sao
Anh muốn tìm kiếm giống YouTube: gõ sai chính tả, thiếu ký tự, viết dính chữ vẫn ra kết quả từ đúng tới gần đúng; gõ tên kênh thì ra cả kênh lẫn video của kênh; tab "Tất cả" có kênh.

Hiện trạng: chỉ mục `videos` được tạo tay trên server (backend `createIndex = false`, không biết cấu hình analyzer), không có tên kênh; kênh tìm bằng MySQL `LIKE` và chỉ hiện ở tab "Kênh".

**Thành công nghĩa là** (kiểm bằng curl trên production): "son tung" / "sontung" / "son tunh" đều ra video "Sơn Tùng…" nếu có; gõ tên kênh ra thẻ kênh ở đầu "Tất cả" và video của kênh; bộ lọc C1 vẫn đúng.

## 2. Phạm vi
- **Chỉ mục video mới `videos_v2`**, backend tự tạo (settings + mappings bên dưới) lúc khởi động nếu chưa có, rồi đồng bộ lại toàn bộ từ MySQL như hiện nay. Chỉ mục cũ `videos` để nguyên.
- **Quay về chỉ mục cũ** nếu không tạo / không ghi được `videos_v2` (vd. API key không có quyền tạo chỉ mục): mọi thao tác dùng `videos`, các trường mới không có thì câu truy vấn vẫn chạy trên `title` / `description` như cũ.
- **Video có thêm** `channelName` và `tags` dạng chữ để tìm.
- **Chỉ mục kênh `channels`**: id, tên (cùng 3 cách phân tích), số người đăng ký (để xếp hạng, có thể cũ tới lần khởi động sau). Chỉ kênh không bị ẩn / xoá. Đồng bộ: toàn bộ lúc khởi động + khi tạo kênh (`initiateChannel`; tên kênh không có chỗ nào sửa sau khi tạo). Không tạo được `channels` thì tìm kênh quay về MySQL như hiện nay. Thẻ kênh vẫn lấy dữ liệu hiển thị từ MySQL (số đăng ký luôn mới).
- **Tab "Tất cả"**: tối đa 2 kênh khớp "chặt" (mọi từ khớp, cho sai 1–2 ký tự; hoặc ≥ 80% mẩu 3 chữ khớp) ở trên, video bên dưới. Tab "Kênh": tối đa 20 kênh khớp "rộng".
- **Gợi ý ô tìm kiếm** dùng chỉ mục mới (tiền tố + không dấu + chịu lỗi).

**Không làm:** sửa chính tả kiểu "Có phải bạn muốn tìm…", tìm theo phụ đề, playlist trong kết quả, cuộn vô hạn (C5).

## 3. Phân tích chữ (cùng cho `videos_v2.title`, `channelName`, `channels.name`)
| Trường con | Analyzer | Ví dụ "Sơn Tùng M-TP" ghi thành | Dùng để |
|---|---|---|---|
| (chính) | `folded`: standard + lowercase + asciifolding | `son`, `tung`, `m`, `tp` | khớp từ, không dấu, `fuzziness: AUTO` |
| `.compact` | `compact`: char_filter xoá mọi ký tự không phải chữ/số → ngram 3 + lowercase + asciifolding | `son`,`ont`,`ntu`,`tun`,`ung`,`ngm`,`gmt`,`mtp` | dính chữ, thiếu ký tự (`minimum_should_match` 60% khi tìm video, 80% cho kênh "chặt") |
| `.prefix` | `autocomplete`: edge_ngram 1–20 + lowercase + asciifolding (search_analyzer `folded`) | `s`,`so`,`son`,`t`,`tu`… | gợi ý khi đang gõ |

`description` và `tags.text` chỉ dùng `folded`. Các trường lọc / sắp xếp giữ nguyên: `isShort`, `duration`, `createdAt`, `totalView`.

## 4. Câu truy vấn video (`/api/videos/search/full`)
`bool`: `should` (ít nhất 1) =
- `match_phrase` `title` (boost 6) — khớp cả cụm xếp đầu;
- `multi_match` best_fields `title^3`, `channelName^2`, `tags.text^2`, `description`, `fuzziness AUTO` (boost 2);
- `match` `title.compact`, `minimum_should_match 60%`;
- `match` `channelName.compact`, `minimum_should_match 60%`;

`filter` = bộ lọc C1 như hiện có. Sắp xếp C1 như hiện có (liên quan = theo điểm).

## 5. Kiểm tra
- Test: settings / mappings JSON hợp lệ và có đủ analyzer; quay về `videos` khi tạo `videos_v2` lỗi; tìm kênh quay về MySQL khi ES lỗi; "chặt" vs "rộng".
- Sau deploy: log tạo chỉ mục + số tài liệu đồng bộ; curl các ca ở mục 1; bộ lọc C1 như lần trước.
