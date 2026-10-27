# L1 Livestream Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chủ kênh phát trực tiếp (webcam hoặc OBS) qua LiveKit; người xem xem, chat, bình chọn Poll; người đăng ký được báo; có trang `/live`, chip "Trực tiếp", tab "Trực tiếp" của kênh và lịch sử buổi phát.

**Architecture:** Backend Spring Boot giữ buổi phát trong MySQL (`live_streams`, `live_polls`, `live_poll_votes`), ký token LiveKit, bọc SDK LiveKit sau một interface (`LiveKitGateway`) để test bằng Mockito, và hỏi LiveKit mỗi 15 giây để chuyển trạng thái. Chat và Poll đi qua backend bằng STOMP sẵn có (topic `/topic/live/{id}`), chat giữ trong bộ nhớ + Redis như phòng xem chung. Client Next.js dùng `livekit-client` / `@livekit/components-react` (đã có) cho hình, RTK Query cho REST, STOMP cho chat.

**Tech Stack:** Spring Boot 3, JPA/MySQL, Redis (`RedisService`), STOMP (`SimpMessagingTemplate`), `io.livekit:livekit-server:0.10.0`; Next.js 15, RTK Query, shadcn/Tailwind v4, `@stomp/stompjs`.

**Spec:** `docs/superpowers/specs/2026-10-27-livestream-design.md` (anh duyệt 27/10; gói 0 đã deploy).

## Global Constraints

Giá trị lấy nguyên văn từ spec; mọi task đều phải theo.

- Tiêu đề ≤ **100** ký tự (bắt buộc), mô tả ≤ **5.000**. Tin chat ≤ **200** ký tự (quá thì **từ chối**, không cắt), giữ **100** tin gần nhất. Poll: câu hỏi ≤ **150**, **2–4** lựa chọn, mỗi lựa chọn ≤ **60**.
- Token LiveKit hạn **6 giờ**. Chủ kênh webcam: `CanPublish` + `CanSubscribe`. Người xem / khách: chỉ `CanSubscribe` (không `CanPublishData`). Identity: chủ kênh `u:{ownerId}`, người xem `v:{uuid ngẫu nhiên}` mỗi lần lấy token. Phòng LiveKit tên `live-{id}`.
- Job trạng thái chạy mỗi **15 giây**. Tự kết thúc: LIVE mà mất người phát > **2 phút**; CREATED không lên sóng sau **1 giờ** thì CANCELED; LIVE quá **12 giờ**.
- Mỗi kênh tối đa **một** buổi CREATED/LIVE (409 kèm id buổi cũ). Kênh `ActiveStatus.INACTIVE` → 403 (cùng kiểu luật với `VideoServiceImpl` dòng ~728).
- Tối đa **40** người xem bằng hình mỗi buổi; người thứ 41 trở đi nhận `{full:true, token:null}`, vẫn chat/bình chọn được.
- `RateLimiter.Limit` mới: `LIVE_CREATE` 10 / 1 giờ / người; `LIVE_TOKEN` 30 / 1 phút / IP; `LIVE_VOTE` 10 / 1 phút / người; `LIVE_CHAT` 5 / 10 giây / người (chat vượt thì báo riêng người đó, không ném lỗi).
- Redis: khoá `live:chat:{id}`, TTL **13 giờ**; Redis lỗi thì chỉ dùng bộ nhớ (như `WatchRoomService`).
- OBS: giao thức mặc định **WHIP** (`enableTranscoding=false`, không truyền video/audio options), RTMP là lựa chọn phụ (`enableTranscoding=true`, preset `H264_1080P_30FPS_3_LAYERS` + `OPUS_STEREO_96KBPS` như code cũ).
- Cột enum (`status`, `source`) dùng `@Enumerated(EnumType.STRING)` **kèm** `@JdbcTypeCode(SqlTypes.VARCHAR)` để Hibernate (`ddl-auto=update` trên prod) không tạo ENUM của MySQL. Thêm cột/chỉ mục bằng `CommentSchemaInitializer` (`addColumn`, `addIndex` có sẵn).
- Mọi giờ là UTC (`LocalDateTime.now()`, JVM chạy UTC). Thông báo lỗi cho người dùng bằng **tiếng Việt**.
- Commit message tiếng Anh, kèm dòng `Co-Authored-By` của phiên. Mỗi gói một nhóm commit. **Không push / deploy khi chưa hỏi anh**; deploy backend trước, client sau.
- Backend kiểm: `./gradlew --no-daemon compileJava compileTestJava -q` rồi `./gradlew --no-daemon test`. Client kiểm: `npx tsc --noEmit` (đúng 12 lỗi TS7006 cũ, không thêm), `npx eslint <file đã sửa>`, `npx next build`. Client chưa có test tự động.

## Review Focus

Các tình huống spec ngụ ý nhưng không có task nào tự nhiên kiểm; mỗi dòng đã có test ở task ghi trong ngoặc.

1. LiveKit lỗi hoặc hết hạn mức khi tạo buổi / tạo Ingress / cấp token: người dùng thấy thông báo tiếng Việt (503), **không để lại dòng buổi phát treo** (T3, T5).
2. Backend deploy lại giữa buổi phát: buổi LIVE vẫn còn trong DB, chat khôi phục từ Redis, job tiếp tục; phòng LiveKit đã biến mất thì sau 2 phút tự kết thúc (T5, T8).
3. Người không phải chủ kênh gọi sửa / kết thúc / mở-đóng Poll / xoá tin → 403; admin kết thúc được (T3, T6, T9).
4. Khách (chưa đăng nhập) không SEND được và không nghe được topic khác `/topic/live/*` (T7).
5. Văn bản chat/Poll chứa HTML hoặc ký tự lạ: server lưu nguyên văn, client hiển thị dạng text (không `dangerouslySetInnerHTML`) (T8, T14).

---

## GÓI 1 – Backend: buổi phát

### Task 1: Entity, repository, chỉ mục

**Files:**
- Create: `WeTube-service-dev/src/main/java/com/wetube/wetube_service/enumeration/LiveStatus.java` (`CREATED, LIVE, ENDED, CANCELED`), `LiveSource.java` (`BROWSER, ENCODER`), `LiveProtocol.java` (`WHIP, RTMP`)
- Create: `.../entity/live/LiveStream.java`, `LivePoll.java`, `LivePollVote.java`, `LivePollVoteId.java`
- Create: `.../repository/live/LiveStreamRepository.java`, `LivePollRepository.java`, `LivePollVoteRepository.java`
- Modify: `.../job/CommentSchemaInitializer.java` (thêm chỉ mục và cột)
- Test: `src/test/java/com/wetube/wetube_service/repository/live/LiveRepositoryTest.java` (`@DataJpaTest` H2, mẫu `job/SelfSubscriptionCleanupTest.java`)

**Interfaces:**
- Produces `LiveStream` (UUID id `@JdbcTypeCode(VARCHAR)`/`VARCHAR(36)` như `Notification`; `UUID channelId`, `UUID ownerId` là cột thường, không quan hệ): `String title`, `String description (TEXT)`, `LiveSource source`, `LiveProtocol protocol` (null với BROWSER), `LiveStatus status`, `boolean chatEnabled`, `String livekitRoom`, `String ingressId`, `String ingressUrl`, `String ingressKey`, `LocalDateTime createdAt, startedAt, endedAt, lastPublisherAt`, `int currentViewers, peakViewers`.
- Produces `LivePoll`: `UUID id, streamId`, `String question`, `String options` (JSON mảng chuỗi, TEXT), `boolean open`, `LocalDateTime createdAt, closedAt`. `LivePollVote` khoá `@EmbeddedId LivePollVoteId(UUID pollId, UUID userId)`, `int optionIndex`, `LocalDateTime createdAt`.
- Produces `LiveStreamRepository`: `Optional<LiveStream> findFirstByChannelIdAndStatusIn(UUID, Collection<LiveStatus>)`, `List<LiveStream> findByStatusIn(Collection<LiveStatus>)`, `List<LiveStream> findByStatusOrderByCurrentViewersDesc(LiveStatus)`, `boolean existsByStatus(LiveStatus)`, `Page<LiveStream> findByChannelIdAndStatusOrderByEndedAtDesc(UUID, LiveStatus, Pageable)`.
- Produces `LivePollRepository`: `Optional<LivePoll> findFirstByStreamIdAndOpenTrue(UUID)`, `List<LivePoll> findByStreamIdOrderByCreatedAtAsc(UUID)`. `LivePollVoteRepository extends JpaRepository<LivePollVote, LivePollVoteId>`: thêm `@Query("select v.optionIndex, count(v) from LivePollVote v where v.id.pollId = :pollId group by v.optionIndex") List<Object[]> countByOption(@Param("pollId") UUID pollId)` trả `[optionIndex, count]`, và `Optional<LivePollVote> findById(LivePollVoteId)` (sẵn có) để lấy `myVote`.

- [ ] **Step 1: Write the failing test** `LiveRepositoryTest`: `findsOnlyUnfinishedStreamOfChannel` (lưu CREATED, ENDED cùng channel → `findFirstByChannelIdAndStatusIn(ch, [CREATED, LIVE])` trả đúng CREATED), `pollVoteCountsGroupByOption` (3 phiếu: option 0,0,2 → `[[0,2],[2,1]]`), `secondVoteSamePollSameUserViolatesPrimaryKey` (lưu hai `LivePollVote` cùng `(pollId,userId)` → `DataIntegrityViolationException` khi flush). Dùng cấu hình `@DataJpaTest(properties = {ddl-auto=create-drop, H2 MODE=MySQL, driver org.h2.Driver})` + `@AutoConfigureTestDatabase(replace = NONE)` y như `SelfSubscriptionCleanupTest`.
- [ ] **Step 2: Run** `./gradlew --no-daemon test --tests '*LiveRepositoryTest' -q` → FAIL (class chưa có).
- [ ] **Step 3: Implement** enum, entity, repository đúng chữ ký ở trên. Enum field có `@JdbcTypeCode(SqlTypes.VARCHAR)`.
- [ ] **Step 4: Thêm vào `CommentSchemaInitializer.run`**: `addColumn("notifications", "live_id", "VARCHAR(36) NULL")`, `addIndex("live_streams", "idx_live_streams_status", "status")`, `addIndex("live_streams", "idx_live_streams_channel", "channel_id")`, `addIndex("live_polls", "idx_live_polls_stream", "stream_id")`. (Bảng do Hibernate tạo vì prod chạy `ddl-auto=update`; `addIndex` bỏ qua êm nếu bảng chưa có, chỉ ghi log lỗi.)
- [ ] **Step 5: Run** test → PASS; `compileJava compileTestJava` sạch.
- [ ] **Step 6: Commit** `git commit -m "Live: entities, repositories and indexes"`.

### Task 2: Cổng LiveKit và token

**Files:**
- Create: `.../service/live/LiveKitGateway.java` (interface), `LiveKitGatewayImpl.java`, `LiveKitUnavailableException.java` (RuntimeException, mang `boolean quota`)
- Modify: `.../configuration/LivekitConfig.java` (giữ nguyên hai bean; không đổi)
- Test: `src/test/java/.../service/live/LiveKitGatewayImplTest.java`

**Interfaces:**
- Produces `LiveKitGateway`:
  - `void createRoom(String room, String metadataJson)`; `void deleteRoom(String room)`
  - `List<Participant> participants(String room)` với `record Participant(String identity, boolean publishingVideo)` (rỗng nếu phòng không tồn tại)
  - `String publisherToken(String room, String identity, String displayName)`; `String viewerToken(String room, String identity)`; cả hai hạn `Duration.ofHours(6)`
  - `IngressDetails createIngress(String room, String identity, String displayName, LiveProtocol protocol)` với `record IngressDetails(String ingressId, String url, String streamKey)`
  - `void deleteIngress(String ingressId)`; `int deleteInactiveIngress()`; `String wsUrl()`
- Mọi lỗi từ SDK / mạng (kể cả HTTP 403/429 của LiveKit) → `LiveKitUnavailableException` (`quota=true` khi mã 403/429 hoặc thân lỗi chứa "quota"/"limit").

- [ ] **Step 1: Write the failing test** `LiveKitGatewayImplTest` (khoá giả `key`/`secret` 32 ký tự, không gọi mạng): `viewerTokenCannotPublish` giải JWT bằng `Nimbus`/`JwtDecoder` HS256 với secret và khẳng định `video.canPublish == false`, `video.canSubscribe == true`, `video.canPublishData` không true, `video.room == "live-x"`, `sub` bắt đầu bằng `v:`; `publisherTokenCanPublish` (`canPublish == true`, `sub == "u:owner"`); `tokenExpiresInAboutSixHours` (`exp - iat` trong 21.500–21.700 giây).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** `LiveKitGatewayImpl` (`@Service`, dùng `RoomServiceClient`, `IngressServiceClient`, `LivekitProperties` có sẵn). Token: `AccessToken` + `RoomJoin/RoomName/CanSubscribe` (+ `CanPublish` cho chủ). `createRoom` gọi `roomClient.createRoom(room, null, null, null, metadata, null, null, null, null)` như code cũ. `createIngress`: WHIP → `IngressInput.WHIP_INPUT`, audio/video `null`, bypass `null`, `enableTranscoding=false`; RTMP → `RTMP_INPUT` + hai preset + `enableTranscoding=true`; `participantIdentity` = identity truyền vào. `participants`: `roomClient.listParticipants(room)`; `publishingVideo` = có track `TrackType.VIDEO`. Bọc mọi `IOException`/`Response` không thành công thành `LiveKitUnavailableException`.
- [ ] **Step 4: Run** test → PASS.
- [ ] **Step 5: Commit** `git commit -m "Live: LiveKit gateway with viewer/publisher tokens"`.

### Task 3: Buổi phát – tạo, sửa, kết thúc, đọc

**Files:**
- Create: `.../service/live/LiveStreamService.java`, `.../service/live/LiveEventPublisher.java`
- Create: `.../dto/live/CreateLiveRequest.java` (`title, description, source, protocol, chatEnabled`), `UpdateLiveRequest.java` (`title, description, chatEnabled`, mọi trường tuỳ chọn), `LiveView.java`, `LiveCard.java`, `ChannelLivesResponse.java`, `LiveEvent.java`
- Test: `.../service/live/LiveStreamServiceTest.java` (Mockito, `@ExtendWith(MockitoExtension.class)`, `@MockitoSettings(strictness = LENIENT)` như `NotifyNewVideoTest`)

**Interfaces:**
- Consumes: `LiveKitGateway` (T2), `LiveStreamRepository` (T1), `UserRepository`, `RateLimiter`, `SimpMessagingTemplate`.
- Produces `LiveEventPublisher`: `void toStream(UUID id, LiveEvent e)` (→ `/topic/live/{id}`), `void toUser(String userId, LiveEvent e)` (→ `/user/queue/live`).
- Produces `LiveEvent` (builder, `@JsonInclude(NON_NULL)`): `String type` (`CHAT, CHAT_DELETED, CHAT_STATE, VIEWERS, POLL, STATUS, ERROR`), `Integer viewers`, `String status`, `LiveChatMessage message`, `String messageId`, `Boolean chatEnabled`, `PollView poll`, `String text` (cho ERROR). `LiveChatMessage` / `PollView` do T8 / T9 định nghĩa; trong T3 tạo bản khung rỗng có đúng tên để biên dịch (T8, T9 điền nội dung).
- Produces `LiveStreamService`:
  - `LiveView create(UUID userId, CreateLiveRequest req)`: kiểm tiêu đề, `RateLimiter.check(LIVE_CREATE, userId)`, 403 nếu kênh INACTIVE, 409 (`ResponseStatusException` có message chứa id buổi cũ) nếu đã có buổi chưa kết thúc, tạo phòng (+ nếu `ENCODER`: gọi `gateway.deleteInactiveIngress()` trước, nuốt lỗi, như `LivekitController.createIngress` cũ, rồi tạo Ingress, mặc định WHIP); gateway lỗi → `ResponseStatusException(503, "Không kết nối được máy chủ phát trực tiếp, vui lòng thử lại" hoặc, khi `quota`, "Đã hết hạn mức phát trực tiếp của tháng này, sẽ có lại vào ngày 1")` và **dọn phòng đã tạo, không lưu dòng**.
  - `Optional<LiveView> mine(UUID userId)`; `LiveView update(UUID userId, UUID id, UpdateLiveRequest)` (403 nếu không phải chủ; 409 nếu đã kết thúc; đổi `chatEnabled` thì `toStream(CHAT_STATE)`); `void end(UUID userId, boolean admin, UUID id)` (chủ hoặc admin; idempotent nếu đã ENDED; xoá phòng, xoá Ingress, ghi `endedAt`, `toStream(STATUS "ENDED")`, rồi gọi `onEnded(id)` của mọi `LiveCleanup`; xem Step 3).
  - `LiveView get(UUID id, UUID viewerIdOrNull)` (404 nếu không có hoặc CANCELED); `List<LiveCard> listLive()` (LIVE, người xem nhiều trước); `ChannelLivesResponse byChannel(UUID channelId, Pageable)` (`current` + trang đã ENDED mới nhất trước).
- `LiveView`: `id, channelId, channelName, channelPicture, channelHandle, ownerId, title, description, status, source, protocol, chatEnabled, currentViewers, peakViewers, createdAt, startedAt, endedAt, ingress {url, streamKey}` (chỉ khi người gọi là chủ), `poll` (T9). `LiveCard`: `id, title, channelId, channelName, channelPicture, channelBackground, viewers, startedAt, endedAt, peakViewers, status`.

- [ ] **Step 1: Write the failing tests** `LiveStreamServiceTest`: `createRejectsSecondUnfinishedStreamWith409AndExistingId`, `createBlockedForHiddenChannel403`, `createWithEncoderReturnsIngressAndSavesCreated` (khẳng định `protocol==WHIP` mặc định, `ingressKey` có mặt trong `LiveView` của chủ), `createEncoderCleansInactiveIngressFirst` (`inOrder`: `deleteInactiveIngress` trước `createIngress`; `deleteInactiveIngress` ném lỗi vẫn tạo được), `createWhenGatewayThrowsQuotaReturns503WithMonthMessageAndSavesNothing` (`verify(repo, never()).save(any())`, và `gateway.deleteRoom` được gọi nếu phòng đã tạo), `updateByNonOwnerIs403`, `endByNonOwnerIs403ButAdminCanEnd`, `endIsIdempotent`, `titleLongerThan100Rejected400`, `listLiveOrdersByViewersDesc`, `viewForNonOwnerHidesIngress`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** theo Interfaces. Để `end` dọn Poll và chat mà không phụ thuộc vòng: định nghĩa trong T3 interface `LiveCleanup { void onEnded(UUID streamId); }` (một phương thức) và `LiveStreamService` nhận `List<LiveCleanup>` (Spring tiêm mọi bean); T8/T9 mỗi cái `implements LiveCleanup`. Thêm `LiveEventPublisher` và bản khung `LiveChatMessage`, `PollView` (record rỗng) cho biên dịch.
- [ ] **Step 4: Run** tests → PASS. `compileJava compileTestJava` sạch.
- [ ] **Step 5: Commit** `git commit -m "Live: stream service (create, update, end, read)"`.

### Task 4: Thông báo `LIVE_STARTED`

**Files:**
- Modify: `.../entity/notification/Notification.java` (thêm `Type.LIVE_STARTED`, cột `liveId` UUID `live_id`), `.../service/notification/NotificationService.java` (`NotificationView` thêm `UUID liveId` ở cuối record; thêm `void notifyLiveStarted(UUID liveId, UUID ownerId, UUID channelId, String title)`), `.../service/notification/impl/NotificationServiceImpl.java`, `.../repository/notification/NotificationRepository.java` (`existsByTypeAndLiveId(Notification.Type, UUID)`)
- Test: `src/test/java/.../service/notification/NotifyLiveStartedTest.java` (mẫu `NotifyNewVideoTest`)

**Interfaces:**
- `notifyLiveStarted`: `@Transactional(REQUIRES_NEW)`, bỏ qua nếu đã có thông báo `LIVE_STARTED` cho `liveId`; người nhận = `findNotifiableSubscriberIds(channelId, [ALL, PERSONALIZE])` trừ `ownerId`; `actorId=ownerId`, `liveId`, `text=title` cắt 200.
- `list()` trả thêm `liveId`; thông báo `LIVE_STARTED` không có `videoId` nên không bị bộ lọc video loại.
- Sửa mọi chỗ khởi tạo `NotificationView` (code chính và test) cho đủ tham số mới.

- [ ] **Step 1: Write the failing test** `NotifyLiveStartedTest`: `sendsToAllAndPersonalizeNotToNoneNorOwner` (bắt `modes` truyền cho `findNotifiableSubscriberIds` = `{ALL, PERSONALIZE}`; owner trong danh sách bị loại), `doesNotSendTwiceForSameLive` (`existsByTypeAndLiveId` true → không `saveAll`), `savedNotificationHasTypeAndLiveId`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `NotifyLiveStartedTest` và `NotifyNewVideoTest` → PASS; `compileJava compileTestJava` sạch (bắt các chỗ `NotificationView` còn thiếu tham số).
- [ ] **Step 5: Commit** `git commit -m "Notifications: LIVE_STARTED"`.

### Task 5: Token xem, vòng đời theo thời gian, job 15 giây

**Files:**
- Modify: `.../service/live/LiveStreamService.java` (thêm `token`, `markStarted`, `refreshAll`)
- Create: `.../dto/live/LiveTokenResponse.java` (`String token, String wsUrl, String role (PUBLISHER|VIEWER), boolean full`), `.../job/LiveStatusJob.java`
- Test: `.../service/live/LiveStreamLifecycleTest.java`

**Interfaces:**
- Produces: `LiveTokenResponse token(UUID id, UUID viewerIdOrNull)` (410 nếu ENDED/CANCELED; chủ + BROWSER → `publisherToken`; ngược lại `viewerToken("v:"+uuid)`; đã 40 người xem → `full=true, token=null`; `RateLimiter.check(LIVE_TOKEN, ip)` do controller gọi, không ở service); `void markStarted(UUID userId, UUID id)` (403 không phải chủ; chỉ chuyển CREATED→LIVE nếu gateway thấy người phát có hình, nếu chưa thì bỏ qua êm); `void refreshAll(LocalDateTime now)` (public, để test cho giờ giả).
- `LiveStatusJob`: `@Scheduled(fixedDelay = 15000) tick()` gọi `refreshAll(LocalDateTime.now())` và nuốt lỗi từng buổi (log `warn`, buổi khác vẫn chạy); `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh") cleanIngress()` gọi `gateway.deleteInactiveIngress()` (thay `LiveKitJob`).
- `refreshAll` với mỗi buổi CREATED/LIVE: lấy `participants`; `publisherPresent = any(publishingVideo)`; `viewers = số người không phải publisher`; ghi `currentViewers`, `peakViewers = max`; CREATED + có người phát → LIVE, `startedAt=now`, `lastPublisherAt=now`, gọi `NotificationService.notifyLiveStarted` (T4, làm trước task này), `toStream(STATUS "LIVE")`; LIVE + có người phát → `lastPublisherAt=now`; LIVE + vắng > 2 phút → kết thúc (như `end`, hệ thống); LIVE sống > 12 giờ → kết thúc; CREATED tuổi > 1 giờ → `CANCELED`, xoá phòng/Ingress; luôn `toStream(VIEWERS)` khi `currentViewers` đổi; log `info` mỗi buổi: `id`, viewers.

- [ ] **Step 1: Write the failing tests** `LiveStreamLifecycleTest` (gateway mock, giờ `now` truyền vào): `createdBecomesLiveWhenPublisherHasVideoAndNotifiesOnce` (chạy `refreshAll` hai lần → `notifyLiveStarted` đúng 1 lần), `liveEndsAfterTwoMinutesWithoutPublisher` (vắng 119s vẫn LIVE, 121s thì ENDED), `createdIsCanceledAfterOneHour`, `liveEndsAfterTwelveHours`, `roomMissingFromLivekitCountsAsNoPublisher` (`participants` rỗng → áp luật 2 phút; mô phỏng backend deploy lại), `peakViewersOnlyGrows`, `tokenForViewerHasNoPublishRightAndAnonymousIdentity` (gọi `viewerToken` với identity bắt đầu `v:`), `ownerWebcamGetsPublisherToken`, `encoderOwnerGetsViewerToken`, `fortyFirstViewerGetsFullAndNoToken` (participants giả đủ 40 người xem → `full=true`, `token=null`; 39 người xem thì vẫn nhận token), `tokenForEndedStreamIs410`, `markStartedByNonOwner403`, `oneStreamFailingDoesNotStopOthers` (gateway ném lỗi ở buổi A, buổi B vẫn được cập nhật).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** theo Interfaces. Hằng số `GRACE = Duration.ofMinutes(2)`, `CREATED_TTL = Duration.ofHours(1)`, `MAX_LIFE = Duration.ofHours(12)`, `VIEWER_CAP = 40` ở đầu lớp.
- [ ] **Step 4: Run** tests → PASS.
- [ ] **Step 5: Commit** `git commit -m "Live: viewer tokens, lifecycle rules and 15s status job"`.

### Task 6: REST `/api/live`, SecurityConfig, giới hạn tần suất, xoá code LiveKit cũ

**Files:**
- Create: `.../controller/LiveController.java`
- Modify: `.../security/RateLimiter.java` (4 `Limit` mới theo Global Constraints), `.../configuration/SecurityConfig.java`, `.../service/video/HomeFeedService.java` (chip `LIVE`)
- Delete: `controller/LivekitController.java`, `service/livekit/LivekitRoomService.java`, `service/livekit/LivekitIngressService.java`, `job/LiveKitJob.java`, `dto/request/livekit/*`, `dto/response/livekit/*`, `mapper/ParticipantMapper.java`, `utility/ParticipantMapperHelper.java`, `entity/Room.java`, `repository/livekit/RoomRepository.java` — **trước khi xoá từng file, `grep -rn` tên lớp; chỉ xoá khi không còn ai dùng**.
- Test: `src/test/java/.../controller/LiveControllerTest.java` (`@WebMvcTest` hoặc gọi trực tiếp bằng Mockito, theo mẫu `RecommendationControllerTest`), cập nhật `HomeFeedServiceTest` nếu có.

**Interfaces:**
- Đường dẫn và quyền đúng bảng 3.3 của spec (trừ Poll/chat, thuộc T8–T9): `POST /api/live`, `GET /api/live/me` (204 nếu không có), `PATCH /api/live/{id}`, `POST /api/live/{id}/started`, `POST /api/live/{id}/end` (`CurrentUser.isAdmin(jwt)` → truyền `admin`), `GET /api/live/{id}`, `POST /api/live/{id}/token`, `GET /api/live`, `GET /api/live/channel/{channelId}?page&size`. `viewerIdOrNull` lấy từ `jwt` (null với khách). `token` gọi `rateLimiter.check(LIVE_TOKEN, ip)` (IP lấy như `VideoController` đang làm cho `VIEW`).
- `SecurityConfig`: thêm GET `"/api/live"`, `"/api/live/**"` vào nhóm GET công khai và `HttpMethod.POST, "/api/live/*/token"` `permitAll`; xoá hai dòng `/api/livekit/...` (dòng ~95–96).
- `HomeFeedService.chips`: thêm `new Chip("LIVE", "Trực tiếp")` ngay sau chip `SUBSCRIPTIONS`, **chỉ khi** `liveRepo.existsByStatus(LiveStatus.LIVE)`; tiêm `LiveStreamRepository`.

- [ ] **Step 1: Write the failing tests** `LiveControllerTest`: `anonymousCanReadLiveListAndStream`, `endPassesAdminFlagFromJwt` (gọi thẳng `controller.end(...)` với `Jwt` giả như `RecommendationControllerTest`, khẳng định cờ admin truyền xuống service), `tokenRateLimitedByIp` (`rateLimiter.check(LIVE_TOKEN, ip)` được gọi). `SecurityConfig` không có test tự động trong dự án: quyền công khai / đăng nhập kiểm bằng curl ở T9 Step 6; trong `HomeFeedServiceTest`: `liveChipAppearsOnlyWhenSomeoneIsLive` (đứng sau chip `SUBSCRIPTIONS`).
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** controller + `Limit` + `SecurityConfig` + chip; sau đó xoá file cũ (grep trước).
- [ ] **Step 4: Run** `./gradlew --no-daemon compileJava compileTestJava test` → toàn bộ PASS (đúng 221 test cũ + test mới, 1 bỏ qua).
- [ ] **Step 5: Commit** `git commit -m "Live: REST API, rate limits, home chip; remove old LiveKit endpoints"`. **Dừng, không deploy** (gộp với gói 2, hỏi anh deploy một lần ở T9).

---

## GÓI 2 – Backend: chat + Poll

### Task 7: Quy tắc STOMP cho khách

**Files:**
- Create: `.../configuration/StompAccessRule.java`
- Modify: `.../configuration/WebSocketStompConfig.java` (bộ chặn `preSend` dùng quy tắc mới)
- Test: `src/test/java/.../configuration/StompAccessRuleTest.java`

**Interfaces:**
- Produces: `static boolean allowed(StompCommand command, String destination, boolean authenticated)`: `CONNECT` luôn `true`; `SUBSCRIBE`: `true` nếu `authenticated`, hoặc khi chưa đăng nhập mà `destination` bắt đầu bằng `/topic/live/`; `SEND` chỉ `true` khi `authenticated`; lệnh khác (`DISCONNECT`, `UNSUBSCRIBE`, `ACK`...) `true`.

- [ ] **Step 1: Write the failing test** `StompAccessRuleTest`: `guestCanConnect`, `guestCanSubscribeLiveTopic`, `guestCannotSubscribeRoomsTopic` (`/topic/rooms/abc`), `guestCannotSubscribeUserQueue` (`/user/queue/live`), `guestCannotSend`, `memberCanDoAll`, `liveTopicPrefixIsNotFooledByLookalike` (`/topic/liveX/1` → khách bị từ chối).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement** rồi sửa `preSend` ném `MessageDeliveryException("Bạn cần đăng nhập")` khi `!allowed(...)`.
- [ ] **Step 4: Run** test mới và `WatchRoomServiceTest` → PASS. **Step 5: Commit** `git commit -m "STOMP: guests may only listen to live topics"`.

### Task 8: Chat

**Files:**
- Create: `.../service/live/LiveChatService.java` (`implements LiveCleanup`), `.../controller/LiveSocketController.java`, `.../dto/live/LiveChatMessage.java`, `.../dto/live/ChatTextRequest.java` (`text`), `.../dto/live/ChatDeleteRequest.java` (`messageId`)
- Modify: `.../controller/LiveController.java` (`GET /api/live/{id}/chat`)
- Test: `.../service/live/LiveChatServiceTest.java`

**Interfaces:**
- `LiveChatMessage`: `String id, userId, name, avatar, handle, text; boolean owner; long at (epoch ms)`.
- `LiveChatService`: `void post(UUID streamId, String userId, String text)`; `void delete(UUID streamId, String userId, boolean admin, String messageId)`; `List<LiveChatMessage> recent(UUID streamId)` (rỗng nếu chat tắt hoặc buổi đã kết thúc); `void onEnded(UUID)` xoá bộ nhớ + `redis.delete`.
- `post`: bỏ qua nếu buổi không LIVE hoặc `chatEnabled=false` (báo riêng `ERROR "Chat đã bị tắt"`); văn bản trống sau `strip()` bỏ qua; > 200 ký tự → báo riêng `ERROR "Tin nhắn tối đa 200 ký tự"`; `rateLimiter.tryAcquire(LIVE_CHAT, userId)` false → báo riêng `ERROR "Bạn nhắn quá nhanh, vui lòng chờ một chút"`; thông tin người gửi từ `UserRepository` (tên, ảnh, `channel.handle`); `owner = userId.equals(ownerId)`; giữ 100 tin; mỗi lần đổi `redis.set("live:chat:"+id, json, 13h)`; `toStream(CHAT)`. `delete` chỉ chủ hoặc admin, phát `CHAT_DELETED`.
- `recent` lần đầu sau khi khởi động đọc lại từ Redis nếu bộ nhớ trống.
- `LiveSocketController`: `@MessageMapping("/live/{id}/chat")` và `("/live/{id}/chat/delete")`, `Principal principal` (id người dùng như `RoomSocketController`).

- [ ] **Step 1: Write the failing tests** `LiveChatServiceTest`: `postBroadcastsMessageWithOwnerFlag`, `over200CharsRejectedWithPrivateError`, `rateLimitedGetsPrivateErrorAndNothingBroadcast`, `chatDisabledRejects`, `keepsOnly100MostRecent`, `htmlIsStoredVerbatim` (`"<img src=x onerror=alert(1)>"` lưu nguyên, không đổi), `nonOwnerCannotDelete`, `adminCanDelete`, `redisDownStillWorksInMemory` (`redis.set` ném lỗi / `redis.get` null → vẫn đăng và đọc được), `restoresFromRedisAfterRestart` (service mới, bộ nhớ trống, `redis.get` trả JSON → `recent` trả đúng), `endedStreamReturnsNoMessages`, `onEndedClearsRedisKey`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement.**
- [ ] **Step 4: Run** → PASS. **Step 5: Commit** `git commit -m "Live: chat over STOMP with Redis backup"`.

### Task 9: Poll

**Files:**
- Create: `.../service/live/LivePollService.java` (`implements LiveCleanup`), `.../dto/live/OpenPollRequest.java` (`question, options[]`), `.../dto/live/VoteRequest.java` (`option` int), điền `.../dto/live/PollView.java`
- Modify: `.../controller/LiveController.java` (3 endpoint Poll), `.../service/live/LiveStreamService.java` (`get` kèm `poll`)
- Test: `.../service/live/LivePollServiceTest.java`

**Interfaces:**
- `PollView`: `String id, question; List<String> options; List<Integer> counts; int total; boolean open; Integer myVote`.
- `LivePollService`: `PollView open(UUID ownerUserId, UUID streamId, OpenPollRequest)` (chủ, buổi LIVE hoặc CREATED, 409 nếu đã có Poll mở, 400 sai độ dài/số lựa chọn); `PollView close(UUID ownerUserId, UUID streamId, UUID pollId)`; `PollView vote(UUID userId, UUID streamId, UUID pollId, int option)` (`RateLimiter.check(LIVE_VOTE, userId)`, 404 nếu không phải Poll của buổi, 409 nếu Poll đã đóng hoặc đã bình chọn, 400 nếu `option` ngoài khoảng); `Optional<PollView> current(UUID streamId, UUID viewerIdOrNull)` (Poll mở, hoặc Poll vừa đóng trong 60 giây gần nhất); `void closeAll(UUID)` / `onEnded` đóng Poll đang mở.
- `LiveView` thêm `List<PollView> polls`: **chỉ khi buổi ENDED**, mọi Poll của buổi theo thứ tự tạo, kèm kết quả cuối (`LiveStreamService.get` gọi `LivePollService.history(UUID streamId)`); buổi chưa kết thúc để `null`.
- Phát `POLL` qua `toStream` khi mở / đóng ngay; khi có phiếu mới đánh dấu "bẩn" và `@Scheduled(fixedDelay = 1000) flush()` phát một lần mỗi giây (gộp). Phiếu trùng `(pollId,userId)` ở mức DB (`DataIntegrityViolationException`) cũng đổi thành 409 (hai yêu cầu đồng thời).

- [ ] **Step 1: Write the failing tests** `LivePollServiceTest`: `onlyOwnerCanOpenAndClose403`, `secondOpenWhileOneIsOpenIs409`, `needsBetweenTwoAndFourOptions`, `questionOver150OrOptionOver60Rejected`, `voteTwiceIs409`, `concurrentDuplicateVoteIs409` (repo ném `DataIntegrityViolationException` → 409), `voteOnClosedPollIs409`, `voteOptionOutOfRangeIs400`, `countsAndMyVoteInView`, `flushBroadcastsOnceForManyVotes` (50 phiếu → 1 lần `toStream(POLL)` sau `flush()`), `recentlyClosedPollStaysVisibleFor60Seconds`, `endingStreamClosesOpenPoll`, `endedStreamViewListsAllPollsWithFinalCounts`, `liveStreamViewHasNoPollHistory`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement.**
- [ ] **Step 4: Run** `./gradlew --no-daemon compileJava compileTestJava test` → toàn bộ PASS. **Step 5: Commit** `git commit -m "Live: polls"`.
- [ ] **Step 6: Hỏi anh** có deploy backend (gói 1+2) ngay không. Nếu có: `git push`, `redeploy_project` (backend `cmvkfhypr07ra0i5f5b51wed1`), `wait_for_job` lặp, rồi kiểm: log không có `Exception`; `curl /api/live` → 200 `[]`; `curl -X POST /api/live` (không đăng nhập) → 401; `curl /api/live/00000000-0000-0000-0000-000000000000` → 404; `curl -X POST .../api/live/x/token` không 401; `curl /api/livekit/rooms` → 404/401 (đã xoá). **Thử OBS WHIP thật** cần client (gói 3), ghi vào T17.

---

## GÓI 3 – Client: phát + xem

### Task 10: API client, kiểu dữ liệu, điều hướng

**Files:**
- Create: `WeTube-client-app-dev/src/app/api/liveApi.ts`, `src/modules/live/types.ts`
- Modify: `src/store/store.ts` (đăng ký `liveApi`), `src/lib/enums/page-urls.ts` (`LIVESTREAM: "/live"`, thêm `STUDIO_LIVE: "/studio/live"`), `src/modules/home/ui/components/home-navbar/index.tsx` (mục "Phát trực tiếp" của nút Tạo `router.push(pageUrls.STUDIO_LIVE)` + đóng menu), `src/modules/studio/ui/components/studio-sidebar/index.tsx` (mục "Phát trực tiếp", icon `RadioTower`)
- Verify: `tsc`, `eslint` các file đã sửa.

**Interfaces:**
- `types.ts` khớp DTO backend: `LiveView`, `LiveCard`, `ChannelLives`, `LiveTokenResponse`, `PollView`, `LiveChatMessage`, `LiveEvent` (đúng tên trường ở T3/T4/T8/T9).
- `liveApi` (RTK Query, mẫu `homeApi.ts`, `baseUrl: ${API_PREFIX}/live`, `credentials: "include", fetchFn: apiFetch`): `getLive(id)` (**`keepUnusedDataFor: 0`**), `getLiveList()`, `getChannelLives({channelId,page,size})`, `getMyLive()` (204 → `null`), `createLive(body)`, `updateLive({id,...})`, `markLiveStarted(id)`, `endLive(id)`, `getLiveToken(id)` (mutation, vì tạo token mới mỗi lần), `getLiveChat(id)`, `openPoll`, `closePoll`, `votePoll`. Xuất hook đúng tên chuẩn RTK (`useGetLiveQuery`, `useCreateLiveMutation`...).

- [ ] **Step 1: Implement** các file trên. **Step 2: Run** `npx tsc --noEmit` → đúng 12 lỗi TS7006 cũ; `npx eslint <các file>` sạch.
- [ ] **Step 3: Commit** `git commit -m "Live client: API slice, types and navigation entries"`.

### Task 11: Kết nối STOMP của buổi phát

**Files:**
- Create: `src/modules/live/lib/liveSocket.ts`, `src/modules/live/lib/useLiveChannel.ts`

**Interfaces:**
- `createLiveSocket(streamId: string, handlers: { onEvent(e: LiveEvent): void; onConnected(): void; onDisconnected(): void }, authenticated: boolean): { send(action: "chat" | "chat/delete", body: unknown): void; stop(): void }` — mẫu `modules/rooms/lib/roomSocket.ts`, khác: chỉ `client.subscribe("/topic/live/"+id)`, và `client.subscribe("/user/queue/live")` **chỉ khi `authenticated`**; `beforeConnect` làm mới phiên chỉ khi `authenticated`; **không** gửi `join`.
- `useLiveChannel(streamId: string | undefined, authenticated: boolean)` trả `{ connected, viewers: number | null, status: string | null, chatEnabled: boolean | null, poll: PollView | null, messages: LiveChatMessage[], privateError: string | null, send }`. Khi kết nối: **đăng ký topic trước**, rồi gọi `GET /api/live/{id}/chat` và gộp, bỏ tin trùng theo `id`; xử lý `CHAT`, `CHAT_DELETED`, `CHAT_STATE`, `VIEWERS`, `POLL`, `STATUS`, `ERROR`; giữ tối đa 200 tin trong state.

- [ ] **Step 1: Implement. Step 2:** `tsc` + `eslint`. **Step 3: Commit** `git commit -m "Live client: STOMP channel hook"`.

### Task 12: Trang xem `/live/[id]`

**Files:**
- Create: `src/app/live/layout.tsx` (sao từ `src/app/watch/layout.tsx`, sửa nội dung cho hợp), `src/app/live/[id]/page.tsx` (server component, `generateMetadata` lấy `GET {API}/live/{id}` bằng `fetch` ở server như `watch/[videoId]/page.tsx`; 404 → `notFound()`), `src/modules/live/ui/live-watch-view.tsx`, `live-player.tsx`, `live-badge.tsx`
- Modify: không.

**Interfaces:**
- `LiveWatchView({id})`: `useGetLiveQuery(id)` (poll mỗi 30 giây khi CREATED để tự sang LIVE); bố cục như `watch-page.tsx` (cột trái trình phát + tiêu đề + hàng kênh dùng `SubscribeControl` từ `modules/channel/ui/subscribeControl.tsx` + mô tả thu gọn + nút Chia sẻ dùng `ShareDialog` với `link={origin}/live/{id}`; cột phải: chat, Poll — gắn ở T14; điện thoại: chat bên dưới, cao cố định).
- `LivePlayer({id, status})`: `getLiveToken(id)` → `<LiveKitRoom serverUrl token connect video={false} audio={false}>` với `RoomAudioRenderer`, `VideoTrack` của người phát; nút Phát/Tạm dừng, âm lượng/tắt tiếng, toàn màn hình, PiP; autoplay bị chặn → phát không tiếng + nút "Bật tiếng" (mẫu trình phát Shorts); mất người phát → "Người phát đang gặp sự cố kết nối…"; `full=true` → "Buổi phát đang đầy người xem" (chat vẫn dùng); ẩn tab > 2 phút → ngắt kết nối LiveKit, quay lại → lấy token mới; trạng thái CREATED → "Buổi phát sắp bắt đầu"; ENDED → màn hình kết thúc (ngày, thời lượng, người xem cao nhất; kết quả Poll thêm ở T14); 410 → cũng màn hình kết thúc. Tài khoản admin (`user?.roles?.includes("ROLE_ADMIN")`, như `admin-reports-view.tsx`) thấy thêm nút **Kết thúc buổi phát (quản trị)** với hộp thoại xác nhận, gọi `endLive(id)`.
- Khách: xem được; ô chat/Poll hiện "Đăng nhập để …" (T14).

- [ ] **Step 1: Implement. Step 2:** `tsc`, `eslint`, `npx next build` qua. **Step 3:** (nếu có Claude in Chrome) mở `/live/{id}` của một buổi giả, kiểm bố cục desktop và 375px; nếu không, ghi vào danh sách "anh thử".
- [ ] **Step 4: Commit** `git commit -m "Live client: watch page"`.

### Task 13: Trang phát `/studio/live`

**Files:**
- Create: `src/app/studio/live/page.tsx`, `src/modules/live/ui/live-studio-view.tsx`, `live-setup-form.tsx`, `live-onair-panel.tsx`, `live-encoder-panel.tsx`; chuyển `src/modules/stream/components/media-device-settings.tsx` → `src/modules/live/ui/media-device-settings.tsx` (sửa import, bỏ phụ thuộc `@radix-ui/themes` nếu có)

**Interfaces:**
- `LiveStudioView`: `useGetMyLiveQuery()`; không có buổi → `LiveSetupForm`; có buổi → `LiveOnAirPanel`.
- `LiveSetupForm`: tiêu đề (≤100), mô tả (≤5.000), chọn nguồn Webcam / OBS (OBS: chọn WHIP mặc định hoặc RTMP kèm cảnh báo "RTMP chỉ còn dùng được khoảng 1 giờ/tháng trên gói miễn phí"), công tắc chat; webcam: xem trước camera + chọn thiết bị; nút **Bắt đầu**; lỗi 409 → chuyển sang buổi đang có; 503/429 → `toastEmitter` với `message` của API qua `lib/apiError.ts`.
- `LiveOnAirPanel` (webcam): `<LiveKitRoom>` với token `PUBLISHER`, bật camera + micro (`LocalParticipant.setCameraEnabled/setMicrophoneEnabled`), sau khi đăng luồng gọi `markLiveStarted(id)`; hiện số người xem, thời gian phát, nút Chép link `/live/{id}`, nút **Kết thúc buổi phát** (hộp thoại xác nhận shadcn `AlertDialog`), chat + Poll (T14). Rời trang không kết thúc buổi (cảnh báo `beforeunload` khi đang LIVE).
- `LiveEncoderPanel` (OBS): hiện `ingress.url` và `ingress.streamKey` (khoá ẩn, nút hiện + Chép), hướng dẫn OBS ngắn (WHIP: Service = WHIP, Server = url, Bearer Token = khoá; RTMP: Custom, Server, Stream key), trạng thái "Đang chờ tín hiệu từ phần mềm phát…" cho tới khi `status` thành LIVE (poll `getLive` mỗi 5 giây).
- Chưa đăng nhập → mời đăng nhập; chưa có kênh / kênh bị ẩn → thông báo.

- [ ] **Step 1: Implement. Step 2:** `tsc`, `eslint`, `next build`. **Step 3:** thử trong Chrome với camera giả (`--use-fake-device-for-media-stream --use-fake-ui-for-media-stream`) nếu có Claude in Chrome; không có thì ghi vào "anh thử". **Step 4: Commit** `git commit -m "Live client: studio page (webcam and encoder)"`.

---

## GÓI 4 – Client: chat, Poll, tìm thấy

### Task 14: Chat và Poll ở hai trang

**Files:**
- Create: `src/modules/live/ui/live-chat.tsx`, `live-poll-card.tsx`, `live-poll-composer.tsx`
- Modify: `live-watch-view.tsx`, `live-onair-panel.tsx` (gắn vào)

**Interfaces:**
- `LiveChat({channel: ReturnType<typeof useLiveChannel>, canModerate: boolean, loggedIn: boolean, chatEnabled: boolean})`: danh sách tin tự cuộn xuống (không giật khi người dùng cuộn lên), ảnh + tên + nhãn nền màu cho chủ kênh, `@handle` tô màu bằng code có sẵn (tìm cách bình luận tô `@handle`), **hiển thị text thuần** (không `dangerouslySetInnerHTML`), ô nhập 200 ký tự có bộ đếm, khách thấy "Đăng nhập để trò chuyện", `privateError` hiện dưới ô nhập, chủ kênh có nút xoá tin (gửi `chat/delete`), chat tắt → "Chat đã bị tắt".
- `LivePollCard({poll, loggedIn, onVote})`: thanh phần trăm + số phiếu, bấm chọn một lần (đã chọn thì khoá và đánh dấu `myVote`), khách bấm → mời đăng nhập, Poll đóng hiển thị "Đã đóng" (ẩn sau 60 giây theo `current` của API). `LivePollComposer`: câu hỏi + 2–4 lựa chọn, nút Mở Poll / Đóng Poll.
- Trang "đã kết thúc" hiện kết quả các Poll từ `LiveView.polls` (T9).

- [ ] **Step 1: Implement. Step 2:** `tsc`, `eslint`, `next build`. **Step 3:** thử hai cửa sổ (chủ + người xem) trong Chrome nếu có; không thì "anh thử". **Step 4: Commit** `git commit -m "Live client: chat and polls"`.

### Task 15: Trang `/live`, chip, sidebar, chuyển hướng, dọn code cũ

**Files:**
- Create: `src/app/(home)/live/page.tsx`, `src/modules/live/ui/live-list-view.tsx`, `live-card.tsx`
- Modify: `src/modules/home/ui/views/home-view.tsx` (chip `LIVE` hiện `LiveList` thay cho `ChipVideoGrid`), `src/modules/home/ui/components/home-sidebar/main-section.tsx` (đã dùng `pageUrls.LIVESTREAM`, chỉ cần hằng số đổi ở T10), `next.config.ts` (`redirects`: `/livestream` và `/livestream/:path*` → `/live`, `permanent: false`)
- Delete: `src/app/livestream/`, `src/modules/stream/`, `src/lib/controller.ts`
- Modify: `package.json`/lockfile — gỡ `livekit-server-sdk`, `jsonwebtoken`, `@types/jsonwebtoken`, `js-confetti`, `@radix-ui/themes` **chỉ khi** `grep -rn` trong `src` không còn dùng

**Interfaces:**
- `LiveCard({live: LiveCard})`: ảnh bìa kênh (banner, không có thì nền tối + ảnh đại diện giữa), nhãn đỏ TRỰC TIẾP, "N người đang xem", tiêu đề, tên kênh; bấm → `/live/{id}`. `LiveListView`: lưới `useGetLiveListQuery` poll 30 giây; rỗng → "Hiện chưa có ai phát trực tiếp".

- [ ] **Step 1:** `grep` từng gói trước khi gỡ; gỡ bằng `npm uninstall` (cập nhật `package-lock.json`). **Step 2: Implement.**
- [ ] **Step 3:** `tsc` (12 lỗi cũ, không thêm), `eslint`, `next build`; kiểm bảng route của `next build` không còn `/livestream/*`. Lưu ý: hàng chip chỉ hiện với tài khoản đã có lịch sử xem (hành vi sẵn có của C6), nên chip Trực tiếp cũng vậy; ghi vào danh sách thử. **Step 4: Commit** (code mới) `git commit -m "Live client: /live page, home chip, redirects"` và (xoá cũ + gỡ gói) `git commit -m "Remove old livestream UI, controller and unused packages"`.

### Task 16: Kênh và chuông

**Files:**
- Modify: `src/modules/channel/ui/channelHeader.tsx` (vòng đỏ + nhãn TRỰC TIẾP quanh ảnh đại diện khi `current` khác null, bấm → `/live/{id}`), `src/modules/channel/ui/channelTabs.tsx` (tab "Trực tiếp" giữa Shorts và Danh sách phát, chỉ khi kênh có `current` hoặc `past.totalElements > 0`), tạo `src/modules/channel/ui/channelLives.tsx` + route con tương ứng (xem cách `shorts` / `playlists` được tạo dưới `app/(home)/channel/[channelId]/`), `src/modules/home/ui/components/home-navbar/notification-bell.tsx` (kiểu `LIVE_STARTED`, trường `liveId`, nội dung `"{who} đang phát trực tiếp: {text}"`, bấm → `/live/${liveId}`)

**Interfaces:**
- Dùng `useGetChannelLivesQuery({channelId, page, size})` (T10) cho cả header, tab và trang con (RTK Query chia sẻ cache).

- [ ] **Step 1: Implement. Step 2:** `tsc`, `eslint`, `next build`. **Step 3: Commit** `git commit -m "Live client: channel live ring, Live tab and bell notification"`.

---

## GÓI 5 – Kiểm tra, tài liệu, deploy

### Task 17: Kiểm tra cuối, ghi chép, deploy khi anh duyệt

**Files:**
- Modify: `TEST_CHECKLIST.md` (mục mới "28. Phát trực tiếp"), `YOUTUBE_PARITY.md` (mục 2.11 → "đã làm", bảng lộ trình L1 ✅ sau khi anh thử OK, chip C6 "Trực tiếp" xong)

- [ ] **Step 1: Backend:** `./gradlew --no-daemon compileJava compileTestJava test` → PASS toàn bộ. **Client:** `tsc` (12 lỗi cũ), `eslint` các file đã sửa, `next build`.
- [ ] **Step 2: Viết `TEST_CHECKLIST.md` mục 28** gồm: tạo buổi webcam; người xem thứ hai thấy hình trong ~1 giây (đo bằng đồng hồ hiển thị trên cả hai máy); thông báo ở chuông của người đăng ký; chat hai chiều + xoá tin; Poll mở / bình chọn / đóng; kết thúc → trang "đã kết thúc"; OBS WHIP (OBS ≥ 30): URL + khoá, hình lên; **kiểm LiveKit không tính phút transcode cho WHIP** (xem Usage trong LiveKit Cloud sau buổi thử); khách xem được, không chat được; tab kênh + vòng đỏ; chip Trực tiếp; `/livestream` chuyển về `/live`; điện thoại 375px.
- [ ] **Step 3: Hỏi anh trước khi push / deploy.** Khi anh đồng ý: `git push`, deploy **backend trước** (`cmvkfhypr07ra0i5f5b51wed1`) rồi **client** (`cmvkg1ilo084w0i5fhrmxxz90`), mỗi lần `wait_for_job` lặp tới khi `finished`.
- [ ] **Step 4: Sau deploy:** `get_runtime_logs` lọc `Exception` (bỏ qua `UnauthorizedEntryPoint`); curl `/api/live` 200, `POST /api/live` không đăng nhập 401, client `/live` 200, `/livestream` 3xx → `/live`, `/studio/live` 200; đo thời gian tạo token. Dùng Chrome camera giả nếu có để chạy một buổi thật; không thì giao danh sách cho anh.
- [ ] **Step 5: Cập nhật `YOUTUBE_PARITY.md`** (việc đã làm, commit, giờ deploy, đã / chưa kiểm tra, số đo độ trễ nếu có) và nhắc anh xoá `LIVEKIT_API_SECRET`, `LIVEKIT_API_KEY`, `LIVEKIT_wsUrl` khỏi biến môi trường **client** trên Vibe Host. **Commit** `git commit -m "YOUTUBE_PARITY.md: L1 livestream deployed"` và push (chỉ tài liệu).
