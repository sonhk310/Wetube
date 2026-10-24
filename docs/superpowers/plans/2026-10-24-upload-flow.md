# Upload flow (drafts, real progress, 4 steps) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Choosing a file starts a real upload to a private draft right away; the creator fills 4 steps (Details, Video elements, Checks, Visibility) and publishes; Shorts are classified by duration ≤ 180 s and width ≤ height.

**Architecture:** Backend gets a `DRAFT` status and `POST /api/videos/drafts` (Cloudinary upload, no indexing, no notifications); publishing reuses `PUT /api/videos/{id}`, which already indexes and notifies on the first transition to public. Client replaces `video-details-modal.tsx` with an upload wizard under `components/upload/`, uploading via the existing axios `apiClient` (`onUploadProgress`, `AbortController`, 401 refresh).

**Tech Stack:** Spring Boot 3 / JPA / MySQL / JUnit 5 + AssertJ; Next.js 15, React, Tailwind v4, shadcn (Radix) `Select`, axios, lottie-react.

**Spec:** `docs/superpowers/specs/2026-10-24-upload-flow-design.md`

## Global Constraints

- Shorts rule: `duration > 0 && duration <= 180 && width > 0 && height > 0 && width <= height`. Same rule on client (badge) and server (stored flag).
- Max upload size 100MB (`max-file-size: 100MB`); client rejects larger files before uploading.
- `DRAFT` is appended at the **end** of `ActiveStatus` and of `REQUIRED_STATUSES` (MySQL ENUM: append only).
- A draft must never be indexed in Elasticsearch, listed, or trigger `notifySubscribers`.
- Default visibility in step 4: **Công khai** (`ACTIVE`).
- Upload screen copy: "Video sẽ được lưu dưới dạng bản nháp cho đến khi bạn đăng."
- AI button label: "Gợi ý thẻ bằng AI"; uses `POST /api/ai/description {title}`, keeps only `tags`, merges with existing tags without duplicates.
- Playlist picker lists only the user's own playlists (`playlistService.getCreated`), multi-select, with "Tạo danh sách phát mới".
- New client code uses `toastEmitter`, not `alert` / `react-hot-toast`.
- Checks before commit (CLAUDE.md): backend `./gradlew --no-daemon compileJava compileTestJava -q`; client `npx tsc --noEmit` (only the 12 old TS7006), `npx eslint <changed files>`, `npx next build`.
- New feature: commit locally, **ask anh before push/deploy**; deploy backend first.

## Review Focus

1. **Publish clicked before the upload finishes** → the click is remembered and publishing runs as soon as the draft id arrives; nothing typed is lost. (Task 5)
2. **Upload fails or the network drops** → footer shows the error and "Thử lại" restarts the upload with the same file; Publish stays disabled. (Task 5)
3. **Dialog closed mid-upload** → confirm "Huỷ tải lên?"; on yes the request is aborted; closing after the draft exists says "Đã lưu bản nháp trong Studio". (Task 5)
4. **Draft visible to others** → `GET /api/videos/by-user/{owner}` as another user does not include it, Shorts feed does not include it. (Task 2 test + Task 7 manual check)
5. **Draft opened in the Studio edit page and saved without picking a visibility** → validation message "Chọn chế độ hiển thị để đăng video", no 400 from the server. (Task 6)

---

### Task 1: Shorts rule and `DRAFT` status (backend)

**Files:**
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/video/ShortsRule.java`
- Create: `WeTube-service-dev/src/test/java/com/wetube/wetube_service/service/video/ShortsRuleTest.java`
- Modify: `.../enumeration/ActiveStatus.java` (append `DRAFT`)
- Modify: `.../job/VideoSchemaInitializer.java:24` (`REQUIRED_STATUSES` adds `"DRAFT"` last)
- Modify: `.../repository/video/VideoRepository.java:85-93` (`findShorts` drops `maxSeconds`, condition `v.isShort = true` only)
- Modify: `.../service/video/impl/VideoServiceImpl.java:65,147,278` (remove `SHORT_MAX_SECONDS`; callers updated)

**Interfaces:**
- Produces: `ShortsRule.MAX_SECONDS = 180f`; `static boolean ShortsRule.isShort(Float duration, Integer width, Integer height)`; `ActiveStatus.DRAFT`.

- [ ] **Step 1: Write `ShortsRuleTest`** (plain JUnit 5 + AssertJ, like `JacksonConfigTest`):
  - `isShort(30f, 1080, 1920)` true; `isShort(170f, 1080, 1920)` true; `isShort(60f, 1080, 1080)` true; `isShort(180f, 720, 1280)` true
  - `isShort(60f, 1920, 1080)` false; `isShort(190f, 1080, 1920)` false
  - `isShort(30f, null, null)` false; `isShort(0f, 1080, 1920)` false; `isShort(null, 1080, 1920)` false
- [ ] **Step 2: Run** `./gradlew --no-daemon test --tests '*ShortsRuleTest*'` → FAIL (class missing)
- [ ] **Step 3: Implement `ShortsRule`** (final class, private constructor, Vietnamese Javadoc citing the rule).
- [ ] **Step 4: Run the test** → PASS
- [ ] **Step 5: Append `DRAFT`** to `ActiveStatus` and `REQUIRED_STATUSES`; change `findShorts(ActiveStatus status, Pageable pageable)` with `AND v.isShort = true`; in `createVideo` set `entity.setShort(false)` for now (Task 2 replaces it with Cloudinary metadata); update the `findShorts` call at line 278.
- [ ] **Step 6: Compile** `./gradlew --no-daemon compileJava compileTestJava -q` → no errors
- [ ] **Step 7: Commit** `git commit -m "Shorts: duration <= 180 s and vertical/square; add DRAFT status"`

### Task 2: `POST /api/videos/drafts` (backend)

**Files:**
- Modify: `.../service/CloudinaryService.java` (new record + method; `uploadVideo` delegates)
- Modify: `.../service/video/VideoService.java`, `.../service/video/impl/VideoServiceImpl.java`
- Modify: `.../controller/VideoController.java` (new endpoint next to `uploadFile`)
- Create: `WeTube-service-dev/src/test/java/com/wetube/wetube_service/service/video/VideoDraftTest.java`

**Interfaces:**
- Consumes: `ShortsRule.isShort`, `ActiveStatus.DRAFT` (Task 1).
- Produces:
  - `public record UploadedVideo(String url, Integer width, Integer height, Float duration)` nested in `CloudinaryService`; `public UploadedVideo uploadVideoWithMeta(MultipartFile videoFile)` reading `width`, `height`, `duration` from the Cloudinary result map (absent → null). `uploadVideo(file)` returns `uploadVideoWithMeta(file).url()`.
  - `VideoDto VideoService.createDraft(MultipartFile videoFile, Float duration, Integer width, Integer height, UUID userId)`.
  - `POST /api/videos/drafts` multipart: `videoFile` (required), `duration`, `width`, `height` (optional) → `201` + `VideoDto`. `@PreAuthorize("isAuthenticated()")`, user from `CurrentUser.id(jwt)`.

- [ ] **Step 1: Write `VideoDraftTest`** with Mockito (`@ExtendWith(MockitoExtension.class)`, `@InjectMocks VideoServiceImpl`, mocks for its constructor deps). `createDraft(file "clip.mp4", 45f, 1080, 1920, userId)` with `uploadVideoWithMeta` returning `new UploadedVideo("https://x/v.mp4", 1080, 1920, 45f)`:
  - saved entity (captor on `videoRepository.save`) has `videosStatus == DRAFT`, `title == "clip"`, `isShort() == true`, `videoUrl == "https://x/v.mp4"`
  - `verify(videoIndexer, never()).index(any())`; `verify(notificationService, never()).notifyNewVideo(any(), any(), any())`
  - Cloudinary metadata wins: client sends `(45f, 1920, 1080)` but Cloudinary returns `1080x1920` → `isShort() == true`
- [ ] **Step 2: Run** `./gradlew --no-daemon test --tests '*VideoDraftTest*'` → FAIL
- [ ] **Step 3: Implement `uploadVideoWithMeta`, `createDraft`** (title = original filename without extension, fallback "Video không có tiêu đề"; duration/width/height = Cloudinary value else client value; `isShort` via `ShortsRule`; add to `USER_UPLOADED` playlist like `createVideo`; no tags, no index, no notify; on DB failure delete the Cloudinary video like `createVideo`). In `createVideo` replace the Task 1 placeholder with `ShortsRule.isShort` on Cloudinary metadata (fallback `meta.duration`).
- [ ] **Step 4: Add the controller endpoint** (no debug `System.out` lines).
- [ ] **Step 5: Run tests + compile** → PASS, no errors
- [ ] **Step 6: Commit** `git commit -m "Upload: create a private draft as soon as the file is uploaded"`

### Task 3: Client upload helpers

**Files:**
- Create: `WeTube-client-app-dev/src/lib/uploadDraft.ts`
- Create: `WeTube-client-app-dev/src/lib/shorts.ts`

**Interfaces:**
- Produces:
  - `MAX_UPLOAD_BYTES = 100 * 1024 * 1024`
  - `uploadDraft(file: File, meta: { duration?: number; width?: number; height?: number }, onProgress: (percent: number) => void, signal: AbortSignal): Promise<{ id: string }>` — `apiClient.post("/videos/drafts", formData, { onUploadProgress, signal })`; percent = `Math.round(loaded / total * 100)`.
  - `SHORTS_MAX_SECONDS = 180`; `isShortVideo(duration: number, width: number, height: number): boolean` (Global Constraints rule); `shortsReason(duration, width, height): string` e.g. `"Dọc · 0:45"`, `"Vuông · 1:00"`, `"Ngang 16:9"`, `"Dài hơn 3 phút"` (use `formatDuration`-style m:ss).

- [ ] **Step 1: Write both files.**
- [ ] **Step 2: Verify** `npx tsc --noEmit` (no new errors), `npx eslint src/lib/uploadDraft.ts src/lib/shorts.ts` → clean
- [ ] **Step 3: Commit** `git commit -m "Client: draft upload with progress; Shorts rule"`

### Task 4: File picker screen

**Files:**
- Modify: `WeTube-client-app-dev/src/components/video-upload-modal.tsx`

**Interfaces:**
- Consumes: `MAX_UPLOAD_BYTES` (Task 3); renders `<UploadWizard file onClose onDone />` (Task 5).

- [ ] **Step 1: On file pick/drop:** reject non-`video/*` or > 100MB with `toastEmitter.error` ("Vui lòng chọn tệp video." / "Tệp quá lớn, tối đa 100MB."); remove `alert` and `console.log`.
- [ ] **Step 2: Lottie one-shot:** once a valid file is set, the icon plays `uploadAnimation` once (`loop={false}`, `autoplay`), and `onComplete` (or a 1200 ms fallback timer) switches to `UploadWizard`.
- [ ] **Step 3: Copy:** subtitle line → "Video sẽ được lưu dưới dạng bản nháp cho đến khi bạn đăng."
- [ ] **Step 4: Verify** tsc / eslint on the file → clean (build in Task 5).
- [ ] **Step 5: Commit** with Task 5 (the wizard it renders lands there).

### Task 5: Upload wizard shell, upload state, publish

**Files:**
- Create: `WeTube-client-app-dev/src/components/upload/upload-wizard.tsx` (state, footer, publish, close)
- Create: `WeTube-client-app-dev/src/components/upload/upload-stepper.tsx`
- Create: `WeTube-client-app-dev/src/components/upload/upload-preview.tsx`
- Delete: `WeTube-client-app-dev/src/components/video-details-modal.tsx` (no longer used)

**Interfaces:**
- Consumes: `uploadDraft`, `isShortVideo`, `shortsReason` (Task 3); step components (Task 6); `useUpdateVideoDetailsMutation` (`{ videoId, formData }` → `PUT /videos/{id}`); `playlistService.addVideo(playlistId, videoId)`; `POST /videos/{id}/subtitles/ai?targetLang=` via `apiFetch`.
- Produces:
  - `type UploadState = { phase: "uploading"; percent: number } | { phase: "processing" } | { phase: "done"; videoId: string } | { phase: "error"; message: string }`
  - `type WizardForm = { title: string; description: string; tags: string; thumbnailFile: File | null; playlistIds: string[]; targetLang: string; visibility: "ACTIVE" | "UNLISTED" | "PRIVATE" }` — defaults: title = file name without extension, `visibility: "ACTIVE"`, `targetLang: ""`.
  - `type StepId = "details" | "elements" | "checks" | "visibility"`; stepper labels "Chi tiết", "Các thành phần của video", "Kiểm tra", "Chế độ hiển thị".
  - `UploadStepper({ steps: { id: StepId; label: string; error: boolean }[]; current: StepId; onSelect(id) })`
  - `UploadPreview({ src: string; isShort: boolean; reason: string; fileName: string })`

- [ ] **Step 1: Wizard state:** read `duration`, `videoWidth`, `videoHeight` from a hidden `<video preload="metadata">` of the object URL, then start `uploadDraft`; percent 100 → phase `processing`; resolved → `done`; rejected (not aborted) → `error` with `apiErrorMessage(err, "Tải lên thất bại")`. "Thử lại" restarts with a new `AbortController`.
- [ ] **Step 2: Footer:** left: "Đang tải lên 45%…" with a bar / "Đang xử lý…" with spinner / "Đã tải lên" / error + "Thử lại". Right: "Quay lại", "Tiếp", and on the last step "Đăng" (label "Đăng khi tải lên xong" while not `done`).
- [ ] **Step 3: Publish:** `FormData` = title, description, tags, `status` = visibility, `thumbnailFile` if set → `updateVideoDetails`; then `playlistService.addVideo` for each id; then subtitles request if `targetLang`. Later-step failures → `toastEmitter.error` naming the part that failed, the video stays published. Success → `toastEmitter.success("Đã đăng video")`, invalidate `Playlist` and `VideoList` like the old modal, `onDone()`.
- [ ] **Step 4: Publish before done:** clicking "Đăng" while uploading stores `publishRequested = true` (after title validation); an effect publishes when phase becomes `done`.
- [ ] **Step 5: Close:** uploading/processing → `window.confirm("Huỷ tải lên?")`, yes → abort + close; `done` and not published → close + `toastEmitter.success("Đã lưu bản nháp trong Studio")`. Wire the existing close button.
- [ ] **Step 6: Stepper errors:** `details` has error when title is empty; `checks` has none of its own.
- [ ] **Step 7: Verify** `npx tsc --noEmit`, `npx eslint src/components/upload src/components/video-upload-modal.tsx`, `npx next build` → clean / success
- [ ] **Step 8: Commit** (with Task 4 and 6) `git commit -m "Upload wizard: upload on select, real progress, 4 steps, publish"`

### Task 6: Step contents

**Files:**
- Create: `WeTube-client-app-dev/src/components/upload/step-details.tsx`
- Create: `WeTube-client-app-dev/src/components/upload/step-elements.tsx`
- Create: `WeTube-client-app-dev/src/components/upload/step-checks.tsx`
- Create: `WeTube-client-app-dev/src/components/upload/step-visibility.tsx`
- Create: `WeTube-client-app-dev/src/components/upload/playlist-picker.tsx`

**Interfaces:**
- Consumes: `WizardForm`, `UploadState`, `StepId` (Task 5); `ThumbnailSelector` (existing); `playlistService.getCreated(userId)`, `playlistService.create(...)` or the `fetchCreate` body in `save-to-playlist-dialog.tsx` (move it to `playlistService.createUserPlaylist(userId, title, privacy)` if reused by both).
- Each step: `({ form, setForm }: { form: WizardForm; setForm: (patch: Partial<WizardForm>) => void })`; `StepChecks` also gets `upload: UploadState` and `goTo(id: StepId)`.

- [ ] **Step 1: `StepDetails`:** title (required, max 100 chars), description, tags input + "Gợi ý thẻ bằng AI" (needs title; merge rule in Global Constraints), `ThumbnailSelector` with auto frame capture moved from the old modal, `PlaylistPicker`.
- [ ] **Step 2: `PlaylistPicker`:** shadcn `DropdownMenu` with checkbox items of the user's playlists + "Tạo danh sách phát mới" (inline title input, privacy private by default); trigger shows "Chọn" or the selected names.
- [ ] **Step 3: `StepElements`:** "Phụ đề AI (dịch sang)" as shadcn `Select` (ignore empty `onValueChange`, see CLAUDE.md), options: Không tạo phụ đề / English / Tiếng Việt / 한국어 / 日本語 / 中文 (values `none`, `en`, `vi`, `ko`, `ja`, `zh`; `none` → `targetLang: ""`).
- [ ] **Step 4: `StepChecks`:** rows with ✓/!: "Có tiêu đề", "Tệp đã tải lên xong" (or current phase), "Loại: Shorts / Video thường" (reason), "Ảnh thu nhỏ"; a failing row links to its step via `goTo`.
- [ ] **Step 5: `StepVisibility`:** radio cards Công khai (`ACTIVE`) / Không công khai (`UNLISTED`) / Riêng tư (`PRIVATE`) with one-line descriptions as in the old `<select>`.
- [ ] **Step 6: Verify** tsc / eslint / `next build` → clean
- [ ] **Step 7: Commit** together with Task 5.

### Task 7: Studio shows drafts

**Files:**
- Modify: `WeTube-client-app-dev/src/modules/studio/ui/sections/video-section.tsx:35-40` (add `DRAFT: { label: "Bản nháp", className: "bg-blue-100 text-blue-700" }`)
- Modify: `WeTube-client-app-dev/src/modules/studio/ui/sections/form-section.tsx:52,441-473`

- [ ] **Step 1:** `status` schema adds `"DRAFT"` with `.refine(v => v !== "DRAFT", "Chọn chế độ hiển thị để đăng video")`; the `Select` shows a disabled `<SelectItem value="DRAFT">Bản nháp (chưa đăng)</SelectItem>` only when the video is a draft.
- [ ] **Step 2: Verify** tsc / eslint / `next build`.
- [ ] **Step 3: Commit** `git commit -m "Studio: label drafts; publish a draft from the edit page"`

### Task 8: Final verification and docs

- [ ] **Step 1:** Backend tests + compile; client tsc / eslint / build (Global Constraints).
- [ ] **Step 2:** Update `YOUTUBE_PARITY.md` (new package section, E3 ✅, what was / was not verified) and commit.
- [ ] **Step 3:** **Ask anh before push/deploy.** After approval: push, deploy backend, check log for "Đã mở rộng videos.videos_status" and no Exception, `curl -X POST .../api/videos/drafts` → 401; deploy client, Studio returns 200. Hand anh the manual checklist from spec §5.
