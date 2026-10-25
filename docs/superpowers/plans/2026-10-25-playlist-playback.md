# Phát theo danh sách phát (B6) + giao diện danh sách phát — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Phát liền một danh sách phát như YouTube (`/watch/{id}?list=`), xoá video khỏi danh sách, thẻ + trang danh sách phát kiểu YouTube, đổi tên / chế độ / xoá danh sách.

**Architecture:** Backend chỉ bổ sung DTO chi tiết / tóm tắt danh sách, thêm `PUT /api/playlists/{id}` và chặn sửa / xoá danh sách hệ thống. Client: các hàm thuần sắp xếp / trộn / tìm video kế tiếp ở `lib/playlistOrder.ts`; một store zustand giữ danh sách đang phát; trang danh sách phát và khung danh sách trên trang xem đọc chung store đó; trình phát hỏi store video kế tiếp khi hết video.

**Tech Stack:** Spring Boot 3 (JUnit 5 + Mockito + AssertJ), Next.js 15 App Router, React 19, Tailwind v4, shadcn (DropdownMenu, Dialog, Select), zustand, lucide-react.

**Spec:** `docs/superpowers/specs/2026-10-25-playlist-playback-design.md`

## Global Constraints
- Không đổi schema DB; "ngày thêm" = cột có sẵn `playlist_videos.created_at` (`PlaylistVideo.createdAt`).
- Chỉ `PlaylistType.USER_PLAYLIST` sửa / xoá được; xoá video khỏi danh sách bị chặn với `USER_UPLOADED` và `HISTORY` (400, `message` tiếng Việt).
- `privacy` khi sửa chỉ nhận `PUBLIC` | `PRIVATE`; tên 1–150 ký tự sau `trim`, không trùng tên danh sách khác của cùng người.
- Link phát: `/watch/{videoId}?list={playlistId}`, trộn thêm `&shuffle=1`; `?t=` vẫn được ưu tiên như hiện nay.
- Trang xem không có `?list=` phải giữ nguyên hành vi (tự phát, màn hình kết thúc, lịch sử, vị trí đang xem).
- Tên hiển thị danh sách hệ thống: `WATCH_LATER` "Xem sau", `LIKED` "Video đã thích", `HISTORY` "Lịch sử xem", `USER_UPLOADED` "Video đã tải lên".
- Chữ giao diện tiếng Việt; toast qua `lib/toastEmitter`; điều hướng bằng `Link` hoặc `useProgressRouter`; Tailwind v4 chỉ đọc `globals.css`; `DialogContent` cần `grid-cols-[minmax(0,1fr)]` khi nội dung có thể tràn.
- Tính năng mới: commit ở máy, **không push / deploy** trước khi anh duyệt.

## Review Focus
- Danh sách rỗng (0 video xem được, vd. toàn video riêng tư của người khác): trang hiện "Đã ẩn N video không xem được", nút Phát tất cả / Trộn bài bị tắt, không lỗi. → test `nextInOrder` với `order = []` (Task 3) + kiểm tra tay ở Task 5.
- Video đang phát không có trong danh sách (đã bị xoá khỏi danh sách, hoặc link `?list=` ghép sai): không tô hàng nào, video kế tiếp = video đầu danh sách. → test `nextInOrder(order, "khong-co", false)` trả `order[0]` (Task 3).
- Danh sách 1 video: không lặp thì hết video hiện màn hình kết thúc thường; bật lặp thì phát lại chính nó qua `?list=`. → test `nextInOrder(["a"], "a", false) === undefined`, `nextInOrder(["a"], "a", true) === "a"` (Task 3).
- Khách / người không phải chủ mở danh sách công khai: không thấy Xoá / Chỉnh sửa / Xoá khỏi; `PUT` / `DELETE` trả 403 (guard `playlistOwner` sẵn có). → kiểm tra bằng curl không đăng nhập sau deploy: `PUT /api/playlists/{id}` → 401 (Task 7).
- Đổi tên trùng tên danh sách khác của chính mình: 400 với `message` "Bạn đã có danh sách phát tên này", hộp thoại giữ nguyên chữ và hiện lỗi. → test `updateRejectsDuplicateTitle` (Task 2).

---

### Task 1: Backend — DTO chi tiết / tóm tắt danh sách

**Files:**
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/dto/response/playlist/PlaylistDetailDto.java`
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/dto/response/playlist/PlaylistVideoDetailDto.java`
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/dto/response/playlist/UserPlaylistDto.java`
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/impl/PlaylistServiceImpl.java` (`getPlaylistDetailedById(UUID, UUID, boolean)`, `toVisibleDtos`)
- Test: `WeTube-service-dev/src/test/java/com/wetube/wetube_service/service/impl/PlaylistDetailTest.java`

**Interfaces:**
- Produces (JSON của `GET /api/playlists/detail/{id}`): `ownerId: UUID`, `ownerName: String` (tên kênh của chủ, null nếu không có kênh), `privacy: String`, `hiddenCount: int`, `lastUpdatedAt: LocalDateTime` (max `addedAt`, danh sách rỗng thì `createdAt` của danh sách); mỗi phần tử `videos[]` thêm `addedAt: LocalDateTime`; `videos` sắp theo `addedAt` giảm dần (null xếp cuối).
- Produces (JSON của các API trả `UserPlaylistDto`): thêm `ownerName: String`; `thumbnailUrl` = ảnh của video xem được có `addedAt` mới nhất.

- [ ] **Step 1: Viết test (thêm vào `PlaylistDetailTest`)**
  - `detailListsNewestAddedFirstWithOwnerAndHiddenCount`: danh sách của owner (kênh "Kỳ Sơn Huỳnh", privacy "PUBLIC") có 3 `PlaylistVideo`: A thêm 2026-10-01, B thêm 2026-10-03 (cả hai ACTIVE), C thêm 2026-10-02 là video PRIVATE của người khác. Gọi với `viewerId = null`. Assert: `videos` = [B, A] (theo `videoId`); `videos.get(0).getAddedAt()` = 2026-10-03; `hiddenCount` = 1; `ownerName` = "Kỳ Sơn Huỳnh"; `ownerId` = owner.getId(); `privacy` = "PUBLIC"; `lastUpdatedAt` = 2026-10-03 (chỉ tính video xem được).
  - `emptyPlaylistLastUpdatedIsCreatedAt`: danh sách không video, `createdAt` = 2026-09-01 → `lastUpdatedAt` = 2026-09-01, `hiddenCount` = 0.
- [ ] **Step 2: Chạy, phải FAIL** — `./gradlew --no-daemon test --tests '*PlaylistDetailTest'` (từ `WeTube-service-dev`): lỗi biên dịch vì chưa có `getAddedAt` / `getHiddenCount`…
- [ ] **Step 3: Cài đặt.** Thêm các trường vào DTO (Lombok `@Data @Builder` sẵn có). Trong `getPlaylistDetailedById(UUID, UUID, boolean)`: `hiddenCount = playlistVideos.size() - visible.size()`; sắp `visible` theo `PlaylistVideo.createdAt` giảm dần trước khi map; set `addedAt`. `ownerName` lấy `playlist.getUser().getChannel().getName()` (null-safe). Trong `toVisibleDtos`: set `ownerName` như trên; `thumbnailUrl` lấy video có `createdAt` lớn nhất trong `visible` thay cho phần tử cuối.
- [ ] **Step 4: Chạy lại, phải PASS** (cả 4 test của lớp).
- [ ] **Step 5: Commit** — `Playlist detail: owner, privacy, hidden count, added date, newest added first`.

### Task 2: Backend — sửa danh sách, chặn danh sách hệ thống

**Files:**
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/dto/request/UpdatePlaylistRequest.java` (`record UpdatePlaylistRequest(String title, String privacy)`)
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/PlaylistService.java`, `.../service/impl/PlaylistServiceImpl.java`, `.../controller/PlaylistController.java`
- Test: `WeTube-service-dev/src/test/java/com/wetube/wetube_service/service/impl/PlaylistEditTest.java`

**Interfaces:**
- Produces: `PUT /api/playlists/{playlistId}` body `{title, privacy}` → 200 `UserPlaylistDto`; guard `ownershipGuard.playlistOwner(playlistId, jwt)` trước khi gọi service (như `DELETE`).
- Produces: `UserPlaylistDto PlaylistService.updatePlaylist(UUID playlistId, UpdatePlaylistRequest request)`.
- Thay đổi: `removePlaylist(UUID)` và `removeVideoFromPlaylist(UUID, UUID)` ném `IllegalArgumentException` (→ 400 qua `GlobalExceptionHandler`).

- [ ] **Step 1: Viết test `PlaylistEditTest`** (Mockito như `PlaylistDetailTest`; mock `PlaylistRepository`, `PlaylistMapper` nếu service dùng mapper để trả DTO):
  - `updateRenamesAndChangesPrivacy`: USER_PLAYLIST "Cũ" → `updatePlaylist(id, new UpdatePlaylistRequest("  Nhạc chill  ", "PRIVATE"))` lưu playlist có `title` "Nhạc chill", `privacy` "PRIVATE".
  - `updateRejectsSystemPlaylist`: WATCH_LATER → `IllegalArgumentException` message "Không sửa được danh sách phát hệ thống".
  - `updateRejectsBadTitleOrPrivacy`: tên "   " → message "Tên danh sách phát phải có 1–150 ký tự"; tên 151 ký tự → cùng message; privacy "FRIENDS" → message "Chế độ hiển thị không hợp lệ".
  - `updateRejectsDuplicateTitle`: `findByUser_IdAndTitle(userId, "Nhạc")` trả một playlist khác id → message "Bạn đã có danh sách phát tên này"; trả chính nó (đổi privacy giữ tên) → không lỗi.
  - `removePlaylistRejectsSystemPlaylist`: HISTORY → message "Không xoá được danh sách phát hệ thống"; `deleteById` không được gọi.
  - `removeVideoRejectedForUploadsAndHistory`: USER_UPLOADED và HISTORY → message "Không xoá được video khỏi danh sách này"; WATCH_LATER và USER_PLAYLIST vẫn xoá như cũ.
- [ ] **Step 2: Chạy, phải FAIL** — `./gradlew --no-daemon test --tests '*PlaylistEditTest'`.
- [ ] **Step 3: Cài đặt** `updatePlaylist` (tìm playlist hoặc `ResourceNotFoundException`, kiểm loại / tên / privacy / trùng tên theo đúng thứ tự trên, lưu, trả DTO qua `toVisibleDtos(List.of(saved), ownerId, false).get(0)`), chặn trong `removePlaylist` và `removeVideoFromPlaylist` (đọc `playlistType` trước khi xoá), endpoint `@PutMapping("/{playlistId}")`.
- [ ] **Step 4: Chạy cả bộ test backend** — `./gradlew --no-daemon compileJava compileTestJava test -q`; phải xanh.
- [ ] **Step 5: Commit** — `Playlists: rename / privacy endpoint, system playlists cannot be edited or deleted`.

### Task 3: Client — kiểu dữ liệu, API, hàm thuần thứ tự phát

**Files:**
- Create: `WeTube-client-app-dev/src/modules/playlists/types.ts`
- Create: `WeTube-client-app-dev/src/lib/playlistOrder.ts` (không import gì, để `node` chạy được)
- Modify: `WeTube-client-app-dev/src/modules/playlists/ui/list/playlist-API.tsx` (thêm `update`)
- Test (tạm, không commit): `<scratchpad>/playlist-order-check.ts`

**Interfaces:**
- Produces `modules/playlists/types.ts`:
  - `PlaylistVideoItem { videoId; videoTitle; channelName: string | null; channelId: string | null; thumbnailUrl: string | null; duration: number; isShort: boolean | null; totalView: number; createdAt: string; addedAt: string | null }`
  - `PlaylistDetail { playlistId; playlistTitle; playlistType: string; privacy: string; ownerId: string; ownerName: string | null; totalVideos: number; hiddenCount: number; createdAt: string; lastUpdatedAt: string; videos: PlaylistVideoItem[] }`
  - `SYSTEM_PLAYLIST_TITLES: Record<string, string>` (4 giá trị ở Global Constraints), `playlistDisplayTitle(type: string, title: string): string`, `isEditablePlaylist(type: string): boolean` (`=== "USER_PLAYLIST"`), `canRemoveFromPlaylist(type: string): boolean` (`USER_PLAYLIST` hoặc `WATCH_LATER`).
- Produces `lib/playlistOrder.ts`:
  - `type PlaylistSort = "added-desc" | "added-asc" | "popular" | "published-desc" | "published-asc"`, `PLAYLIST_SORT_LABELS: Record<PlaylistSort, string>` ("Ngày thêm (mới nhất)", "Ngày thêm (cũ nhất)", "Phổ biến nhất", "Ngày xuất bản (mới nhất)", "Ngày xuất bản (cũ nhất)").
  - `type PlaylistChip = "all" | "video" | "short"`.
  - `arrangeVideos<T extends { addedAt: string | null; createdAt: string; totalView: number; isShort: boolean | null }>(videos: T[], sort: PlaylistSort, chip: PlaylistChip): T[]` (không đổi mảng gốc; `isShort` null tính là video).
  - `shuffleOrder(ids: string[], firstId?: string, random?: () => number): string[]` (Fisher–Yates; `firstId` có trong `ids` thì đứng đầu).
  - `nextInOrder(order: string[], currentId: string, loop: boolean): string | undefined` và `prevInOrder(order: string[], currentId: string): string | undefined` (không có `currentId` → `nextInOrder` trả `order[0]`, `prevInOrder` trả `undefined`; đầu danh sách → `prevInOrder` `undefined`).
- Produces `playlistService.update(playlistId: string, body: { title: string; privacy: "PUBLIC" | "PRIVATE" }): Promise<unknown>` — lỗi thì `throw new Error(message của backend)` như các hàm khác trong file.

- [ ] **Step 1: Viết script kiểm tra** `<scratchpad>/playlist-order-check.ts` import `lib/playlistOrder.ts` bằng đường dẫn tương đối, dùng `node:assert`:
  - `arrangeVideos` với 3 video (thêm 1/10, 3/10, 2/10; lượt xem 5, 50, 20; ngày xuất bản 2/9, 1/9, 3/9; video thứ 2 là Shorts): `added-desc` → [3/10, 2/10, 1/10]; `popular` → [50, 20, 5]; `published-asc` → [1/9, 2/9, 3/9]; chip `short` → chỉ video Shorts; chip `video` → 2 video còn lại; mảng gốc không đổi.
  - `shuffleOrder(["a","b","c","d"], "c", seeded)` bắt đầu bằng "c", là hoán vị của đầu vào.
  - `nextInOrder(["a","b"], "a", false) === "b"`; `nextInOrder(["a","b"], "b", false) === undefined`; `nextInOrder(["a","b"], "b", true) === "a"`; `nextInOrder(["a"], "a", true) === "a"`; `nextInOrder(["a","b"], "x", false) === "a"`; `nextInOrder([], "x", true) === undefined`; `prevInOrder(["a","b"], "b") === "a"`; `prevInOrder(["a","b"], "a") === undefined`.
- [ ] **Step 2: Chạy, phải FAIL** — `node <scratchpad>/playlist-order-check.ts` → lỗi không tìm thấy module.
- [ ] **Step 3: Cài đặt** `lib/playlistOrder.ts`, `modules/playlists/types.ts`, `playlistService.update`.
- [ ] **Step 4: Chạy lại, phải in "OK"**; `npx tsc --noEmit` (từ `WeTube-client-app-dev`) chỉ còn 12 lỗi TS7006 cũ.
- [ ] **Step 5: Commit** — `Playlist ordering helpers, playlist types and update API`.

### Task 4: Client — thẻ danh sách phát dùng chung + hộp thoại Sửa / Xoá

**Files:**
- Create: `WeTube-client-app-dev/src/modules/playlists/ui/components/playlist-tile.tsx`
- Create: `WeTube-client-app-dev/src/modules/playlists/ui/components/edit-playlist-dialog.tsx`
- Create: `WeTube-client-app-dev/src/modules/playlists/ui/components/delete-playlist-dialog.tsx`
- Modify: `WeTube-client-app-dev/src/modules/playlists/ui/list/Playlist-list.tsx` (dùng `PlaylistTile`), `WeTube-client-app-dev/src/modules/channel/ui/playlists/channelPlayLists.tsx` (dùng `PlaylistTile`, bỏ map sang `PlaylistItem`)
- Delete: `WeTube-client-app-dev/src/modules/playlists/ui/list/playlist-card.tsx`, `WeTube-client-app-dev/src/modules/channel/ui/playlists/playListsCard.tsx` (khi không còn ai import)

**Interfaces:**
- Consumes: `playlistDisplayTitle`, `isEditablePlaylist` (Task 3), `playlistService.update` / `removePlaylist`, `UserPlaylistDto` có `ownerName` (Task 1).
- Produces: `PlaylistTile({ playlist, isOwner, onChanged }: { playlist: { playlistId; playlistTitle; playlistType; privacy; totalVideos; thumbnailUrl: string | null; ownerName: string | null }; isOwner: boolean; onChanged: () => void })` — link trang: `WATCH_LATER` → `/playlists/watchlater`, `LIKED` → `/playlists/liked`, còn lại `/playlists/{id}`; rê chuột lên ảnh hiện "▶ Phát tất cả" dẫn tới `/playlists/{id}?play=1` (trang Task 5 thấy `play=1` thì phát video đầu theo thứ tự mặc định, dùng `router.replace`); danh sách 0 video thì không có lớp "Phát tất cả".
- Produces: `EditPlaylistDialog({ playlist, open, onOpenChange, onSaved })` (ô tên + Select Công khai / Riêng tư, nút Lưu tắt khi tên rỗng; lỗi backend hiện dưới ô tên, không đóng hộp thoại), `DeletePlaylistDialog({ playlist, open, onOpenChange, onDeleted })` ("Xoá {tên}?" + "Danh sách phát sẽ bị xoá vĩnh viễn.").

- [ ] **Step 1: Cài đặt `PlaylistTile`** theo ảnh 1 của spec (mục 2.1): 2 vạch xếp chồng phía trên ảnh (`h-1` và `h-1.5`, bo tròn, nền xám nhạt / đậm, hẹp dần), ảnh `aspect-video rounded-xl`, nhãn `ListVideo` + "N video" góc phải dưới, tên `line-clamp-2`, dòng 2 "Riêng tư · Danh sách phát" khi `privacy` là "private" (không phân biệt hoa thường) hoặc `ownerName · Danh sách phát`, dòng 3 link "Xem toàn bộ danh sách"; ⋮ chỉ khi `isOwner && isEditablePlaylist(playlistType)`.
- [ ] **Step 2: Cài đặt 2 hộp thoại**; sau khi lưu / xoá: toast "Đã lưu thay đổi" / "Đã xoá danh sách phát", gọi `onSaved` / `onDeleted`.
- [ ] **Step 3: Thay vào `Playlist-list.tsx` và `channelPlayLists.tsx`**; `isOwner` = `user?.sub === userId` (trang của mình) / `user?.sub === channel.userId` (trang kênh); `onChanged` tải lại danh sách.
- [ ] **Step 4: Kiểm tra** — `npx tsc --noEmit` (chỉ 12 lỗi TS7006), `npx eslint <các file đã sửa / tạo>` 0 lỗi.
- [ ] **Step 5: Commit** — `YouTube-style playlist tiles with edit / delete for the owner`.

### Task 5: Client — trang danh sách phát

**Files:**
- Modify (viết lại phần hiển thị): `WeTube-client-app-dev/src/modules/playlists/ui/list/playlistdetail-list.tsx` (giữ `PlaylistDetailSkeleton` và màu nền `colorthief`)
- Create: `WeTube-client-app-dev/src/modules/playlists/ui/components/playlist-video-row.tsx`
- Create: `WeTube-client-app-dev/src/store/zustand/usePlaylistPlaybackStore.ts`

**Interfaces:**
- Consumes: `PlaylistDetail`, `PlaylistVideoItem`, `playlistDisplayTitle`, `isEditablePlaylist`, `canRemoveFromPlaylist` (Task 3), `arrangeVideos`, `shuffleOrder`, `PLAYLIST_SORT_LABELS` (Task 3), `EditPlaylistDialog`, `DeletePlaylistDialog` (Task 4), `SaveToPlaylistDialog` (`modules/watch/components/save-to-playlist-dialog.tsx`), `ShareDialog` (`modules/watch/components/share-dialog.tsx`, `currentTime={0}`), `useWatchProgress`, `formatDuration`, `formatCompact`, `timeAgo`.
- Produces `usePlaylistPlaybackStore` (zustand):
  - state `{ detail: PlaylistDetail | null; order: string[]; loop: boolean; shuffle: boolean }`
  - `start(detail: PlaylistDetail, order: string[], shuffle: boolean): void` (đặt `loop = false` khi là danh sách khác danh sách đang giữ)
  - `load(detail: PlaylistDetail, shuffle: boolean): void` — chỉ khi chưa giữ đúng danh sách này: `order` = thứ tự backend (đã là "Ngày thêm (mới nhất)"), `shuffle` thì `shuffleOrder(order)`
  - `setLoop(on: boolean)`, `setShuffle(on: boolean, currentId?: string)` (bật: `shuffleOrder(order, currentId)`; tắt: về thứ tự `detail.videos`), `removeVideo(videoId: string)` (bỏ khỏi `detail.videos` và `order`, giảm `totalVideos`)
- Produces `PlaylistVideoRow({ index, video, playlist, href, active?, compact?, onRemove? })` — dùng ở trang này (cỡ lớn) và Task 6 (`compact`: ảnh nhỏ, số thứ tự / ▶ khi `active`); `onRemove` có thì menu ⋮ có "Xoá khỏi {tên}".
- Link phát từ trang: `/watch/{videoId}?list={playlistId}` (+ `&shuffle=1`), gọi `start(...)` với thứ tự đang hiển thị trước khi điều hướng.

- [ ] **Step 1: Store** `usePlaylistPlaybackStore.ts` theo Interfaces.
- [ ] **Step 2: Hàng video** `playlist-video-row.tsx` theo spec mục 2.2 (số thứ tự, ảnh + `formatDuration` + thanh đỏ, tên 2 dòng, "{kênh} · {formatCompact(lượt xem)} lượt xem · {timeAgo(createdAt)}", ⋮: Lưu vào danh sách phát / Xoá khỏi {tên} / Chia sẻ).
- [ ] **Step 3: Trang** `playlistdetail-list.tsx`: cột trái (spec 2.2), dòng "Đã ẩn N video không xem được" (✕ ẩn), Select sắp xếp + 3 chip, danh sách `arrangeVideos(...)`; Phát tất cả / Trộn bài tắt khi danh sách đang hiển thị rỗng; `?play=1` (từ thẻ, Task 4) thì `start(detail, order, false)` rồi `router.replace` tới video đầu; xoá video: gọi `playlistService.removeVideo`, bỏ khỏi state trang + `removeVideo` của store, toast "Đã xoá khỏi {tên}", lỗi thì toast lỗi và giữ hàng. Tên danh sách hiển thị qua `playlistDisplayTitle`.
- [ ] **Step 4: Kiểm tra** — `tsc` (chỉ 12 lỗi TS7006), `eslint` các file đã sửa / tạo 0 lỗi.
- [ ] **Step 5: Commit** — `Playlist page: YouTube layout, sort, chips, play all / shuffle, remove from playlist`.

### Task 6: Client — phát theo danh sách trên trang xem

**Files:**
- Create: `WeTube-client-app-dev/src/modules/watch/components/playlist-panel.tsx`
- Create: `WeTube-client-app-dev/src/modules/watch/hooks/use-playlist-queue.ts`
- Modify: `WeTube-client-app-dev/src/modules/watch/app.tsx` (đặt `<PlaylistPanel />` đầu cột phải, trước `<VideoTags />`)
- Modify: `WeTube-client-app-dev/src/modules/watch/components/video/index.tsx` (`openVideo`, `EndScreen` props, phím `Shift+N` / `Shift+P`)

**Interfaces:**
- Consumes: `usePlaylistPlaybackStore` (Task 5), `nextInOrder`, `prevInOrder` (Task 3), `PlaylistVideoRow` (Task 5, `compact`), `playlistService.getDetail`, `playlistService.removeVideo`.
- Produces `usePlaylistQueue(videoId: string): { listId: string | null; detail: PlaylistDetail | null; position: number /* 1-based, 0 nếu không có trong danh sách */; total: number; next: PlaylistVideoItem | undefined; prev: PlaylistVideoItem | undefined; hrefFor(videoId: string): string }` — đọc `list` / `shuffle` từ `useSearchParams`; có `list` mà store chưa giữ danh sách đó thì gọi `playlistService.getDetail(list)` rồi `load(detail, shuffle)`; tải lỗi (404 / mạng) thì `detail = null` và trang xem chạy như không có `?list=`; `hrefFor` = `/watch/{id}?list={listId}` (+ `&shuffle=1` nếu store đang trộn); không có `list` thì mọi giá trị rỗng và `hrefFor(id)` = `/watch/{id}`.

- [ ] **Step 1: Hook** `use-playlist-queue.ts` theo Interfaces (`next` = `nextInOrder(order, videoId, loop)`, `prev` = `prevInOrder(order, videoId)`).
- [ ] **Step 2: Khung** `playlist-panel.tsx` theo spec mục 2.3: không có `detail` thì `return null`; tiêu đề (link về trang danh sách, qua `playlistDisplayTitle`), "{ownerName} · {position}/{total}" (position 0 thì "–/{total}"), nút `Repeat` (Lặp lại danh sách) và `Shuffle` (Trộn bài, gọi `setShuffle(on, videoId)` rồi `router.replace(hrefFor(videoId))`), nút thu gọn (`ChevronUp/Down`), danh sách cuộn `max-h-[400px]` với hàng đang phát tô sáng và `scrollIntoView({ block: "center" })` khi đổi video; hàng `onRemove` khi `user.sub === detail.ownerId && canRemoveFromPlaylist(detail.playlistType)`.
- [ ] **Step 3: Trình phát** `video/index.tsx`: `const queue = usePlaylistQueue(videoId)`; `openVideo(id)` điều hướng `queue.hrefFor(id)` nếu `id` thuộc danh sách, ngược lại `/watch/{id}`; `EndScreen` nhận `videos = queue.next ? [queue.next đổi sang RecommendVideoDto (id, title, thumbnailUrl, duration, totalView, createAt = createdAt, name = channelName ?? "", picture = ""), ...upNext không trùng id] : upNext` và `autoplay = queue.next ? true : autoplayNext`; trong `onKeyDown`: `e.shiftKey && (e.key === "N" || e.key === "P")` và có `queue.listId` → `openVideo(next/prev)` nếu có, `preventDefault`. Không có `?list=` thì mọi nhánh như cũ.
- [ ] **Step 4: Kiểm tra** — `tsc` (chỉ 12 lỗi TS7006), `eslint` các file đã sửa / tạo 0 lỗi, `npx next build` qua.
- [ ] **Step 5: Commit** — `Watch page plays playlists: queue panel, next in list, loop / shuffle, Shift+N / Shift+P`.

### Task 7: Kiểm tra tổng + ghi chép

**Files:**
- Modify: `YOUTUBE_PARITY.md` (gói 16: B6 + danh sách phát; đánh ✅ B6 ở bảng lộ trình), giữ nguyên các mục khác.

- [ ] **Step 1:** Backend `./gradlew --no-daemon compileJava compileTestJava test -q` xanh; client `npx tsc --noEmit` (12 lỗi TS7006), `npx next build` qua.
- [ ] **Step 2:** `npx next start -p 3107` rồi curl `/playlists/{id}` (một id bất kỳ) trả 200, `/watch/be49c4b5-ae4d-4e28-8b62-d29e53958226?list=khong-ton-tai` trả 200 (trang xem vẫn chạy); tắt server.
- [ ] **Step 3:** Ghi `YOUTUBE_PARITY.md`: đã làm, đã kiểm tra / chưa kiểm tra, các quyết định của em trong spec; commit `YOUTUBE_PARITY.md: package 16, playlist playback`.
- [ ] **Step 4:** **Không push.** Báo anh kèm danh sách "anh thử giúp em"; sau khi anh duyệt: push, deploy backend → curl `PUT /api/playlists/{id}` không đăng nhập trả 401, log không Exception → deploy client → các trang 200.
