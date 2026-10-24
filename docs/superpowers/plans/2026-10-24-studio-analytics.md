# Số liệu phân tích trong Studio: kế hoạch thực hiện

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Trang `/studio/analytics` có 4 ô số (Lượt xem, Thời gian xem, Người đăng ký mới, Bình luận) và biểu đồ theo ngày cho khoảng 7 / 28 / 90 ngày.

**Architecture:** Backend ghi lượt xem và thời gian xem theo ngày vào bảng `video_daily_stats` (SQL thẳng, upsert cộng dồn). Người đăng ký và bình luận đọc thẳng từ `created_at` của bảng có sẵn. Một API `GET /api/studio/analytics` gộp cả bốn theo ngày giờ VN. Client vẽ bằng Chart của shadcn.

**Tech Stack:** Spring Boot 3 / JPA / MySQL / Redis; Next.js 15, RTK Query, shadcn Chart (recharts).

**Spec:** `docs/superpowers/specs/2026-10-24-studio-analytics-design.md`

## Global Constraints
- Ngày = `LocalDate` theo `ZoneId.of("Asia/Ho_Chi_Minh")`; JVM và các cột `created_at` là UTC.
- `days` chỉ nhận 7, 28, 90; giá trị khác thì trả 400 (`IllegalArgumentException` → `GlobalExceptionHandler` trả 400).
- Thời gian xem: cộng `delta` khi `0 < delta ≤ 35` giây; chưa có vị trí trước thì cộng `min(vị trí, 35)`; Redis key `wt:{viewerKey}:{videoId}`, TTL 1 ngày.
- `viewerKey` = `u:{userId}` / `a:{anonId}` (anonId không rỗng, ≤ 64 ký tự) / `ip:{IP}`, giống `VideoController.registerView`.
- Ghi số liệu lỗi thì log `warn` rồi bỏ qua; không bao giờ làm hỏng việc xem video. Câu ghi chạy trong `REQUIRES_NEW`.
- Schema: runner idempotent `@Order(0)`, mẫu `job/SearchHistoryTableInitializer.java`. Không dùng entity JPA cho bảng mới.
- Chữ giao diện tiếng Việt. Toast qua `lib/toastEmitter.ts`.
- Kiểm tra trước commit: backend `./gradlew --no-daemon test -q`; client `npx tsc --noEmit` (chỉ 12 lỗi TS7006 cũ), `npx eslint <file>`, `npx next build`.
- Tính năng mới: commit ở máy, **hỏi anh trước khi push / deploy**.

## Review Focus
1. Xem ở tốc độ 2x: 15 giây báo một lần, vị trí tăng 30 giây → vẫn phải được cộng (test ở Task 2).
2. Lặp video / xem lại từ đầu: vị trí giảm → lần đó cộng 0, không âm (Task 2).
3. Bình luận lúc 23:30 UTC phải rơi vào ngày hôm sau ở VN (Task 3).
4. Người chưa có kênh mở trang phân tích: mọi số 0, không 500 (Task 3).
5. Ghi `video_daily_stats` lỗi (bảng chưa có, DB chập chờn): `registerView` vẫn trả `counted=true` và tổng lượt xem tăng (Task 1).

---

### Task 1: Bảng `video_daily_stats` + ghi lượt xem theo ngày

**Files:**
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/job/VideoDailyStatsTableInitializer.java`
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/analytics/AnalyticsTime.java`
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/analytics/VideoStatsStore.java`
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/video/impl/VideoServiceImpl.java` (`registerView`)
- Test: `WeTube-service-dev/src/test/java/com/wetube/wetube_service/service/video/ViewStatsTest.java`

**Interfaces:**
- Produces:
  - `AnalyticsTime.ZONE` (`ZoneId`), `static LocalDate today()`, `static LocalDateTime startOfDayUtc(LocalDate day)` (đầu ngày VN đổi sang `LocalDateTime` UTC), `static LocalDate dayOf(LocalDateTime utc)`.
  - `VideoStatsStore` (`@Component`): `void add(UUID videoId, UUID ownerId, LocalDate day, int views, long watchSeconds)` — `@Transactional(propagation = REQUIRES_NEW)`, tự bắt lỗi và log `warn`; `List<Object[]> dailyTotals(UUID ownerId, LocalDate from, LocalDate to)` — mỗi dòng `[java.sql.Date day, Number views, Number watchSeconds]`, đã `SUM ... GROUP BY day`; `Optional<LocalDate> firstDay(UUID ownerId)`.

- [ ] **Step 1: Viết test `ViewStatsTest`** (Mockito, mẫu `VideoDraftTest`; mock thêm `RedisService`, `VideoStatsStore`):
  - `countedViewAddsOneViewForOwnerToday`: `redisService.setIfAbsent` trả `true` → `verify(videoStatsStore).add(videoId, ownerId, AnalyticsTime.today(), 1, 0L)`.
  - `repeatedViewAddsNothing`: `setIfAbsent` trả `false` → `verify(videoStatsStore, never()).add(any(), any(), any(), anyInt(), anyLong())`.
  - `statsFailureDoesNotBreakView`: `doThrow(RuntimeException)` khi `add` → `registerView` vẫn trả `counted=true`.
- [ ] **Step 2:** `./gradlew --no-daemon test --tests '*ViewStatsTest' -q` → FAIL (chưa có `VideoStatsStore`).
- [ ] **Step 3:** Viết `AnalyticsTime`, `VideoStatsStore` (EntityManager + `createNativeQuery`, `INSERT … ON DUPLICATE KEY UPDATE views = views + ?, watch_seconds = watch_seconds + ?`; UUID lưu bằng `toString()`), runner tạo bảng đúng SQL ở spec mục 3. Trong `registerView`, khi `counted`: gọi `videoStatsStore.add(...)` trong `try/catch` (lỗi → `log.warn`), owner = `video.getUser().getId()`.
- [ ] **Step 4:** Chạy lại test → PASS; chạy cả bộ `./gradlew --no-daemon test -q` → xanh.
- [ ] **Step 5:** Commit `Analytics: daily view counts in video_daily_stats`.

### Task 2: Thời gian xem (đã đăng nhập + khách)

**Files:**
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/analytics/WatchTimeTracker.java`
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/security/VideoAccess.java` (`requireViewable` trả `UUID` chủ video thay vì `void`; nơi gọi cũ không đổi)
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/controller/VideoController.java` (`saveProgress` + endpoint mới `watchTime`)
- Modify: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/configuration/SecurityConfig.java` (`POST /api/videos/*/watch-time` permitAll, cạnh `/view`)
- Test: `WeTube-service-dev/src/test/java/com/wetube/wetube_service/service/analytics/WatchTimeTrackerTest.java`

**Interfaces:**
- Consumes: `VideoStatsStore.add`, `AnalyticsTime.today()` (Task 1).
- Produces: `WatchTimeTracker.record(String viewerKey, UUID videoId, UUID ownerId, float position)`; package-private `static long deltaSeconds(Float previous, float position)` (thuần, để test).

- [ ] **Step 1: Viết `WatchTimeTrackerTest`:**
  - `deltaSeconds(100f, 115f) == 15`; `deltaSeconds(100f, 130f) == 30` (2x); `deltaSeconds(100f, 136f) == 0` (tua tới); `deltaSeconds(100f, 40f) == 0` (tua lui / lặp lại); `deltaSeconds(100f, 100f) == 0`; `deltaSeconds(null, 3f) == 3`; `deltaSeconds(null, 500f) == 35`.
  - `recordAddsDeltaAndRemembersPosition`: Redis `get("wt:u:x:" + videoId)` trả `"100.0"`, position 115 → `verify(store).add(videoId, ownerId, AnalyticsTime.today(), 0, 15L)` và `verify(redis).set("wt:u:x:" + videoId, "115.0", Duration.ofDays(1))`.
  - `zeroDeltaWritesNothing`: delta 0 → `store.add` không được gọi.
  - `redisDownCountsNothing`: `redis.get` ném lỗi → không gọi `store.add`, không ném ra ngoài.
- [ ] **Step 2:** Chạy test → FAIL.
- [ ] **Step 3:** Viết `WatchTimeTracker` (làm tròn delta xuống giây, `Math.floor`). `saveProgress`: lấy `ownerId = videoAccess.requireViewable(id, jwt)`, gọi `watchTimeTracker.record("u:" + userId, id, ownerId, seconds)` **trước** `watchProgressService.record(...)`. Endpoint mới:
  `@PostMapping("/{id}/watch-time") ResponseEntity<Void> watchTime(@PathVariable UUID id, @RequestParam("t") float seconds, @RequestParam(value = "anonId", required = false) String anonId, HttpServletRequest request)` — `rateLimiter.check(Limit.PROGRESS, clientIp(request))`, `t` không hữu hạn / âm → `IllegalArgumentException`, `requireViewable(id, null)`, viewerKey theo Global Constraints, trả 204.
- [ ] **Step 4:** Test → PASS; cả bộ xanh; `compileJava` sạch.
- [ ] **Step 5:** Commit `Analytics: watch time from player progress, guests included`.

### Task 3: API `GET /api/studio/analytics`

**Files:**
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/dto/response/analytics/ChannelAnalyticsDto.java` (record: `String from, String to, String trackingSince, Totals totals, List<Day> daily`; lồng `record Totals(long views, long watchSeconds, long subscribers, long comments)`, `record Day(String date, long views, long watchSeconds, long subscribers, long comments)`)
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/service/analytics/StudioAnalyticsService.java`
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/controller/StudioAnalyticsController.java` (`@RequestMapping("/api/studio/analytics")`, `@GetMapping` nhận `@RequestParam(defaultValue = "28") int days`, `@PreAuthorize("isAuthenticated()")`, `CurrentUser.id(jwt)`)
- Modify: `SubscriptionRepository` (`@Query("SELECT s.createdAt FROM Subscription s WHERE s.id.tier.channel.id = :channelId AND s.createdAt >= :from AND s.createdAt < :to") List<LocalDateTime> createdAtBetween(...)`)
- Modify: `CommentRepository` (`@Query("SELECT c.createdAt FROM Comment c, Video v WHERE c.targetType = com.wetube.wetube_service.entity.interaction.Comment.TargetType.VIDEO AND c.targetId = v.id AND v.user.id = :ownerId AND v.videosStatus <> com.wetube.wetube_service.enumeration.ActiveStatus.DELETED AND c.createdAt >= :from AND c.createdAt < :to") List<LocalDateTime> videoCommentTimes(...)`)
- Test: `WeTube-service-dev/src/test/java/com/wetube/wetube_service/service/analytics/StudioAnalyticsServiceTest.java`

**Interfaces:**
- Consumes: `VideoStatsStore.dailyTotals`, `firstDay`; `AnalyticsTime.*` (Task 1).
- Produces: `ChannelAnalyticsDto get(UUID userId, int days)`; JSON đúng spec mục 4 (client Task 4 dựa vào tên trường).

- [ ] **Step 1: Viết `StudioAnalyticsServiceTest`** (mock store, 2 repository, `UserRepository` trả user có / không có channel):
  - `rejectsOtherRanges`: `days = 30` → `IllegalArgumentException`.
  - `fillsEveryDayOldestFirst`: `days = 7` → `daily.size() == 7`, `daily.get(6).date().equals(AnalyticsTime.today().toString())`, ngày không có dữ liệu là 0.
  - `bucketsByVietnamDay`: comment `created_at` = hôm qua (UTC) 23:30 → đếm vào `today()` (VN, +7h).
  - `sumsTotals`: store trả hôm nay `[views 3, watch 120]`, 2 sub, 1 comment → `totals == new Totals(3, 120, 2, 1)`.
  - `trackingSinceIsFirstStatsDayOrToday`: `firstDay` rỗng → `trackingSince == today`.
  - `userWithoutChannelGetsZeros`: không gọi `createdAtBetween`, mọi số 0.
- [ ] **Step 2:** Chạy test → FAIL.
- [ ] **Step 3:** Viết service: `from = today.minusDays(days - 1)`, `to = today`; khoảng UTC `[startOfDayUtc(from), startOfDayUtc(to.plusDays(1)))`; chia ngày bằng `AnalyticsTime.dayOf`. Controller như ở Files.
- [ ] **Step 4:** Test → PASS; cả bộ xanh.
- [ ] **Step 5:** Commit `Analytics: GET /api/studio/analytics`.

### Task 4: Trang `/studio/analytics` (client)

**Files:**
- Create (CLI): `WeTube-client-app-dev/src/components/ui/chart.tsx` qua `npx shadcn@latest add chart` (thêm `recharts` vào `package.json`; nếu CLI hỏi ghi đè file có sẵn thì chọn **không**).
- Create: `WeTube-client-app-dev/src/types/analytics.ts` (`ChannelAnalytics`, `AnalyticsDay`, `AnalyticsTotals` đúng tên trường Task 3; `AnalyticsRange = 7 | 28 | 90`)
- Create: `WeTube-client-app-dev/src/app/api/analyticsApi.ts` (mẫu `channelApi.ts`; `getChannelAnalytics: builder.query<ChannelAnalytics, AnalyticsRange>` → `/studio/analytics?days=`, `keepUnusedDataFor: 0`); đăng ký reducer + middleware trong `src/store/store.ts`.
- Create: `WeTube-client-app-dev/src/app/studio/analytics/page.tsx` (chỉ render section)
- Create: `WeTube-client-app-dev/src/modules/studio/ui/sections/analytics-section.tsx`
- Modify: `WeTube-client-app-dev/src/lib/enums/page-urls.ts` (`ANALYTICS: "/studio/analytics"`), `src/modules/studio/ui/components/studio-sidebar/index.tsx` (mục "Số liệu phân tích", icon `BarChart3`, giữa "Nội dung" và "Tuỳ chỉnh")

**Interfaces:**
- Consumes: JSON của Task 3.

- [ ] **Step 1:** Chạy `npx shadcn@latest add chart`; kiểm `git diff --stat` chỉ thêm `chart.tsx` + `package.json` / `package-lock.json`.
- [ ] **Step 2:** Viết types, API slice, store, page, sidebar.
- [ ] **Step 3:** `analytics-section.tsx`: tiêu đề "Số liệu phân tích của kênh" + `Select` (7 ngày / 28 ngày / 90 ngày, mặc định 28); 4 thẻ nút (`aria-pressed`), thẻ đang chọn viền dưới màu chính; giá trị: lượt xem / người đăng ký / bình luận dùng `formatCompact`, thời gian xem `(watchSeconds / 3600).toLocaleString("vi-VN", { maximumFractionDigits: 1 })` + " giờ"; `ChartContainer` + `LineChart` trục X `dd/MM`, `ChartTooltip`; với Lượt xem / Thời gian xem, nếu `trackingSince > from` thì `ReferenceArea` xám nhạt từ `from` tới ngày trước `trackingSince` và dòng chú thích "Chưa ghi dữ liệu trước dd/MM/yyyy"; đang tải = `Skeleton`; lỗi = chữ báo lỗi + nút "Thử lại" (`refetch`).
- [ ] **Step 4:** `npx tsc --noEmit` (chỉ 12 TS7006), `npx eslint` các file mới / đã sửa, `npx next build` → qua.
- [ ] **Step 5:** Commit `Studio analytics page with shadcn Chart`.

### Task 5: Trình phát gửi thời gian xem của khách

**Files:**
- Modify: `WeTube-client-app-dev/src/lib/viewTracking.ts` (thêm `saveAnonWatchTime(videoId: string, seconds: number): void` — `apiFetch(\`${API_PREFIX}/videos/${videoId}/watch-time?t=${t}&anonId=${encodeURIComponent(getAnonId())}\`, { method: "POST", keepalive: true })`, `t` làm tròn 0,1 như `saveWatchProgress`, lỗi chỉ `console.error`)
- Modify: `WeTube-client-app-dev/src/modules/watch/components/video/index.tsx` (`reportProgress`: bỏ điều kiện `!p.userId` khỏi lệnh `return`; cuối hàm `p.userId ? saveWatchProgress(...) : saveAnonWatchTime(p.videoId, t)`)

- [ ] **Step 1:** Sửa 2 file như trên.
- [ ] **Step 2:** tsc / eslint / `next build` → qua.
- [ ] **Step 3:** Commit `Player reports guests' watch time`.

### Task 6: Tài liệu, hỏi anh, deploy
- [ ] **Step 1:** Cập nhật `YOUTUBE_PARITY.md`: gói 13 (E1), đã / chưa kiểm tra; đánh ✅ E1.
- [ ] **Step 2:** Hỏi anh trước khi push. Được duyệt: push → deploy backend → `wait_for_job` → log có dòng tạo bảng, không `Exception` → deploy client.
- [ ] **Step 3:** Kiểm tra sau deploy: `curl` `GET /api/studio/analytics` → 401; `curl -X POST '/api/videos/<id video công khai>/watch-time?t=5&anonId=test'` → 204; `/studio/analytics` → 200.
