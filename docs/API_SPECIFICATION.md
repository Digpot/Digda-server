# 📘 API 명세서 — DigDa (그룹 다이어리)

> 이 문서는 `dev` 브랜치 컨트롤러에서 재생성되었습니다 (기준일 2026-06-17).
> 실제 런타임 스펙은 Swagger UI(`/swagger-ui.html`, `/v3/api-docs`)가 단일 소스입니다.

---

## 📌 개요

| 항목 | 값 |
|------|----|
| 프로젝트 | DigDa — 그룹 기반 공유 다이어리 앱 |
| 서버 | Spring Boot (Kotlin) + JPA(MySQL) + Redis |
| 인증 방식 | 소셜 로그인(카카오·네이버·애플) + 관리자 계정 + JWT (Access/Refresh) |
| API 스타일 | RESTful JSON |
| Base URL | 호스트 직하 (context-path 없음). 예: `https://api.digda.app` |
| 날짜 형식 | ISO 8601 (`LocalDate` `2026-06-17`, `LocalTime` `09:00`, `LocalDateTime`) |
| 식별자 | 사용자 = UUID(BINARY16), 그 외 도메인 = Long(auto-increment) |
| 에러 응답 | `ErrorCode` 기반 `{ code, message }` (HTTP status 동봉) |
| 이미지 업로드 | Multipart form-data → S3 URL 반환, 이후 `imageId`/URL로 참조 |
| 그룹 삭제 | Soft Delete (24시간 후 영구 삭제), 그 외 Hard Delete |

### 경로 컨벤션

| 영역 | 경로 접두 | 인증 |
|------|-----------|------|
| 앱 일반 API | `/auth/*`, `/users/*`, `/group-rooms/*`, `/character*`, `/titles`, `/blocks/*`, `/reports`, `/inquiries`, `/notifications`, `/devices`, `/uploads/*`, `/nickname-exhibits`, `/app-config` | JWT (로그인/토큰갱신 제외) |
| 어드민 API | `/api/admin/**` | JWT + `ROLE_ADMIN` |
| 공개(비로그인) | `/api/web/public/**`, `/auth/login`, `/auth/refresh`, `/api/app/reissue`, `/api/healthcheck` | 불필요 |
| 테스트 전용 | `/api/test/**`, `/api/callback/**` | 불필요 (⚠️ 운영 비활성 대상) |

### 인증 헤더

```
Authorization: Bearer {accessToken}
```

---

## 📂 도메인 목록

### 앱(클라이언트) 도메인

| # | 도메인 | 엔드포인트 | 설명 |
|---|--------|:---:|------|
| 1 | Auth | 7 | 소셜 로그인, 토큰 갱신, 약관, 로그아웃, 회원 탈퇴 |
| 2 | User | 4 | 프로필 조회/수정, 알림 설정 |
| 3 | GroupRoom | 7 | 그룹방 CRUD, 홈 대시보드, 삭제 예약/복구 |
| 4 | Invite | 3 | 초대 코드 생성/검증/참여 |
| 5 | Membership | 4 | 구성원 목록, 내보내기, 방장 양도, 탈퇴 |
| 6 | Schedule | 5 | 일정 CRUD, 참여자, 기간 조회 |
| 7 | Diary | 10 | 일기 CRUD, 캘린더, 지역지도, 좋아요, 리액션 |
| 8 | Comment | 4 | 일정·일기 댓글 작성/삭제 |
| 9 | Todo | 4 | 할 일 CRUD, 완료 토글 |
| 10 | Notification | 4 | 알림 목록, 읽음, 전체 읽음, 삭제 |
| 11 | Device | 2 | FCM 디바이스 토큰 등록/해제 |
| 12 | Upload | 1 | 이미지 업로드 |
| 13 | Character | 6 | 모찌 상태, 경험치, 진화 트리, 마스터 게임, 광고 보상 |
| 14 | CharacterQuiz | 4 | 퀴즈 생성/목록/랜덤/응시 |
| 15 | CharacterShop | 4 | 상점 조회, 구매, 장착, 해제 |
| 16 | Title | 5 | 칭호 카탈로그/보유/획득/장착 |
| 17 | Block | 5 | 사용자 차단, 게시물 숨김 |
| 18 | Report | 1 | 신고 |
| 19 | Inquiry | 2 | 고객센터 문의 작성/목록 |
| 20 | NicknameExhibit | 2 | 역대 별명 전시관 접근/목록 |
| 21 | AppConfig | 1 | 앱 운영 설정 조회 |
| 22 | Public(DeletionRequest) | 2 | 비로그인 계정/데이터 삭제 요청 |
| 23 | Infra | 2 | 헬스체크, 인증 체크 |
| | **앱 합계** | **84** | (테스트 전용 3개 별도) |

### 어드민 도메인 (`/api/admin/**`, `ROLE_ADMIN`)

| # | 도메인 | 엔드포인트 | 설명 |
|---|--------|:---:|------|
| A1 | Auth | 1 | 관리자 로그인 |
| A2 | Dashboard | 1 | 요약 통계 |
| A3 | User | 4 | 사용자 목록/상세/권한/이용제한 |
| A4 | GroupRoom | 3 | 그룹방 목록/상세/상태변경 |
| A5 | Diary | 3 | 일기 목록/상세/삭제 |
| A6 | Schedule | 2 | 일정 목록/상세 |
| A7 | Character | 3 | 모찌 목록/상세/보정 |
| A8 | Report | 2 | 신고 목록/처리 |
| A9 | Inquiry | 2 | 문의 목록/답변 |
| A10 | DeletionRequest | 2 | 삭제 요청 목록/완료 |
| A11 | Announcement | 2 | 공지 발송/목록 |
| A12 | Notification | 1 | 알림 목록 |
| A13 | AppConfig | 2 | 운영 설정 조회/수정 |
| A14 | Title | 4 | 칭호 카탈로그/보유/부여/회수 |
| A15 | NicknameExhibit | 7 | 별명 카드 CRUD + 접근 권한 관리 |
| A16 | RegionMap | 4 | 시그니처 지도 채움 관리 |
| A17 | DB | 6 | 테이블/컬럼/행 조회·수정·삭제 |
| A18 | Log | 1 | 유저 행동 로그 조회 |
| | **어드민 합계** | **50** | |

> **전체 ~134개 엔드포인트** (앱 84 + 어드민 50, 테스트 3 별도).

---

## 📦 앱 도메인 상세

### 🔐 1. Auth (`/auth`, oauth2)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|:---:|------|
| POST | `/auth/login` | ❌ | 소셜 로그인. 최초 로그인 시 계정 자동 생성 (소셜 ID+provider 식별) |
| POST | `/auth/terms` | ✅ | 약관 동의 (신규 가입 시) |
| GET | `/auth/terms/{type}` | ✅ | 약관 문서 조회 (⚠️ 폐지 예정) |
| POST | `/auth/refresh` | ❌ | 토큰 갱신 (Refresh Rotation) |
| POST | `/api/app/reissue` | ❌ | 토큰 재발급 (레거시 호환) |
| POST | `/auth/logout` | ✅ | 로그아웃 (Refresh 무효화 + 디바이스 해제) |
| DELETE | `/auth/account` | ✅ | 회원 탈퇴 (소유 그룹 있으면 양도 필요) |

**`POST /auth/login` Request** — `LoginRequest`

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| provider | string | ✅ | `kakao` · `naver` · `apple` |
| accessToken | string | ✅ | 소셜 액세스 토큰 |
| idToken | string | 조건부 | Apple Sign In 시 |

**`POST /auth/terms` Request** — `TermsAgreeRequest`: `termsOfService`✅, `privacyPolicy`✅, `ageConfirmation`(기본 true), `marketingConsent`, `pushConsent`

**관련 에러**: `REQUIRED_TERMS_NOT_AGREED`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `REFRESH_TOKEN_INVALID`, `OWNS_ACTIVE_GROUP_ROOM`, `SOCIAL_AUTH_FAILED`, `INVALID_PROVIDER`

---

### 👤 2. User (`/users`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/users/me` | 내 프로필 조회 |
| PUT | `/users/me` | 프로필 수정 (닉네임·프로필 이미지) |
| GET | `/users/me/notification-settings` | 알림 설정 조회 |
| PUT | `/users/me/notification-settings` | 알림 설정 수정 |

**`PUT /users/me` Request** — `UpdateProfileRequest`

| 필드 | 타입 | 설명 |
|------|------|------|
| name | string? | 표시 이름(displayName). 소셜 원본 name은 변경 안 됨 |
| profileImageId | `Optional<string>`? | 이미지 ID. 빈 Optional = 기본 아바타로 초기화 |

**`PUT /users/me/notification-settings` Request** — `UpdateNotificationSettingRequest` (변경 필드만): `pushEnabled`, `scheduleNotification`, `diaryNotification`, `commentNotification`, `marketingConsent`

**관련 에러**: `NAME_TOO_SHORT`, `NAME_TOO_LONG`, `IMAGE_NOT_FOUND`, `NOTIFICATION_SETTING_NOT_FOUND`

---

### 🏠 3. GroupRoom (`/group-rooms`)

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|:---:|------|
| POST | `/group-rooms` | 멤버 | 그룹방 생성 (생성자가 방장, 초대 코드 자동 발급) |
| GET | `/group-rooms` | 멤버 | 내 그룹방 목록 (최근 활동순) |
| GET | `/group-rooms/{groupRoomId}` | 멤버 | 그룹방 상세 |
| GET | `/group-rooms/{groupRoomId}/home` | 멤버 | 그룹 홈 대시보드 (오늘 요약 + 활성 그룹) |
| PUT | `/group-rooms/{groupRoomId}` | 방장 | 그룹방 수정 |
| DELETE | `/group-rooms/{groupRoomId}` | 방장 | 삭제 예약 (24시간 후 영구 삭제) |
| POST | `/group-rooms/{groupRoomId}/recover` | 방장 | 삭제 예약 복구 (24시간 내) |

**`POST /group-rooms` Request** — `CreateGroupRoomRequest`: `name`✅(2~20자), `maxMembers`✅, `thumbnailImageId`?

**`PUT /group-rooms/{id}` Request** — `UpdateGroupRoomRequest`: `name`?, `maxMembers`?, `thumbnailImageId`(`Optional<string>` — 빈 값=썸네일 제거)

**관련 에러**: `GROUP_ROOM_NOT_FOUND`, `GROUP_ROOM_NAME_TOO_SHORT/LONG`, `MAX_MEMBERS_BELOW_CURRENT`, `GROUP_ROOM_NOT_SCHEDULED_FOR_DELETION`, `GROUP_ROOM_ALREADY_DELETED`, `GROUP_ROOM_LIMIT_EXCEEDED`(최대 6개), `NOT_GROUP_ROOM_MEMBER`, `NOT_GROUP_ROOM_OWNER`

---

### 🔗 4. Invite

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|:---:|------|
| POST | `/group-rooms/{groupRoomId}/invites` | 방장 | 초대 코드 재발급 (기존 무효화) |
| POST | `/invites/validate` | 멤버 | 초대 코드 검증 (그룹 미리보기) |
| POST | `/invites/join` | 멤버 | 초대 코드로 참여 |

**Request** — `InviteCodeRequest`: `code`✅ (6자리)

**관련 에러**: `INVITE_CODE_INVALID`, `INVITE_CODE_EXPIRED`, `GROUP_ROOM_FULL`, `ALREADY_JOINED`, `GROUP_ROOM_LIMIT_EXCEEDED`

---

### 👥 5. Membership

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|:---:|------|
| GET | `/group-rooms/{groupRoomId}/memberships` | 멤버 | 구성원 목록 |
| DELETE | `/group-rooms/{groupRoomId}/memberships/{targetUserId}` | 방장 | 구성원 내보내기 |
| PUT | `/group-rooms/{groupRoomId}/memberships/{targetUserId}/role` | 방장 | 역할 변경(방장 양도) |
| POST | `/group-rooms/{groupRoomId}/leave` | 멤버 | 그룹방 탈퇴 |

**`PUT .../role` Request** — `ChangeRoleRequest`: `role`✅ (`owner`)

**관련 에러**: `CANNOT_REMOVE_OWNER`, `USER_NOT_IN_GROUP_ROOM`, `OWNER_CANNOT_LEAVE`

---

### 📅 6. Schedule

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|:---:|------|
| GET | `/group-rooms/{groupRoomId}/schedules` | 멤버 | 일정 목록 (기간: `startDate`,`endDate`) |
| GET | `/group-rooms/{groupRoomId}/schedules/{scheduleId}` | 멤버 | 일정 상세 (+ 댓글) |
| POST | `/group-rooms/{groupRoomId}/schedules` | 멤버 | 일정 생성 |
| PUT | `/group-rooms/{groupRoomId}/schedules/{scheduleId}` | 작성자/방장 | 일정 수정 |
| DELETE | `/group-rooms/{groupRoomId}/schedules/{scheduleId}` | 작성자/방장 | 일정 삭제 |

**`POST` Request** — `CreateScheduleRequest`: `title`✅(≤50자), `color`✅(hex), `startDate`✅, `endDate`✅, `startTime`?, `endTime`?, `allDay`✅, `participantIds`?(UUID[])

**관련 에러**: `SCHEDULE_NOT_FOUND`, `END_DATE_BEFORE_START`, `END_TIME_BEFORE_START`, `INVALID_PARTICIPANT`

---

### 📓 7. Diary

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|:---:|------|
| GET | `/group-rooms/{groupRoomId}/diaries` | 멤버 | 일기 목록 (월 필터·페이지네이션) |
| GET | `/group-rooms/{groupRoomId}/diaries/calendar` | 멤버 | 일기 존재 날짜 배열 |
| GET | `/group-rooms/{groupRoomId}/diaries/region-map` | 멤버 | 시그니처 지도 — region_key별 일기 수 집계 |
| GET | `/group-rooms/{groupRoomId}/diaries/by-region` | 멤버 | 특정 지역(regionKey)의 일기 목록 |
| GET | `/group-rooms/{groupRoomId}/diaries/{diaryId}` | 멤버 | 일기 상세 (+ 댓글) |
| POST | `/group-rooms/{groupRoomId}/diaries` | 멤버 | 일기 작성 (이미지 0~10장) |
| PUT | `/group-rooms/{groupRoomId}/diaries/{diaryId}` | 작성자/방장 | 일기 수정 |
| DELETE | `/group-rooms/{groupRoomId}/diaries/{diaryId}` | 작성자/방장 | 일기 삭제 |
| POST | `/group-rooms/{groupRoomId}/diaries/{diaryId}/like` | 멤버 | 좋아요 토글 |
| POST | `/group-rooms/{groupRoomId}/diaries/{diaryId}/reactions` | 멤버 | 이모지 리액션 토글 |

**`POST` Request** — `CreateDiaryRequest`: `title`✅(≤20자), `content`✅(≤300자), `date`✅, `weather`✅(0~3), `mood`✅(0~3), `location`?, `regionKey`?, `regionSido`?, `regionSigungu`?, `imageIds`(string[], 최대 10)

**리액션 Request** — `ToggleDiaryReactionRequest`: `type` (`HEART`·`CRY`·`SPARKLE`·`LAUGH`·`FIRE`)

**관련 에러**: `DIARY_NOT_FOUND`, `FUTURE_DATE_NOT_ALLOWED`, `DIARY_DATE_TOO_OLD`(3개월), `DIARY_EDIT_WINDOW_EXPIRED`(3개월), `INVALID_WEATHER_VALUE`, `INVALID_MOOD_VALUE`

---

### 💬 8. Comment

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|:---:|------|
| POST | `/group-rooms/{groupRoomId}/schedules/{scheduleId}/comments` | 멤버 | 일정 댓글 작성 |
| POST | `/group-rooms/{groupRoomId}/diaries/{diaryId}/comments` | 멤버 | 일기 댓글 작성 |
| DELETE | `/group-rooms/{groupRoomId}/schedules/{scheduleId}/comments/{commentId}` | 작성자/방장 | 일정 댓글 삭제 |
| DELETE | `/group-rooms/{groupRoomId}/diaries/{diaryId}/comments/{commentId}` | 작성자/방장 | 일기 댓글 삭제 |

**Request** — `CreateCommentRequest`: `text`✅ (≤200자). **에러**: `COMMENT_NOT_FOUND`, `COMMENT_TOO_LONG`

---

### ✅ 9. Todo

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|:---:|------|
| GET | `/group-rooms/{groupRoomId}/todos` | 멤버 | 할 일 목록 (미완료→완료) |
| POST | `/group-rooms/{groupRoomId}/todos` | 멤버 | 할 일 생성 |
| PATCH | `/group-rooms/{groupRoomId}/todos/{todoId}` | 멤버 | 완료 토글 |
| DELETE | `/group-rooms/{groupRoomId}/todos/{todoId}` | 작성자/방장 | 할 일 삭제 |

**Request** — 생성 `CreateTodoRequest`: `text`✅(≤100자) / 토글 `ToggleTodoRequest`: `completed`✅. **에러**: `TODO_NOT_FOUND`, `TODO_TEXT_TOO_LONG`

---

### 🔔 10. Notification

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/notifications` | 알림 목록 (최신순, 페이지네이션) |
| PATCH | `/notifications/{notificationId}` | 읽음 처리 (`UpdateNotificationReadRequest`: `isRead`) |
| POST | `/notifications/read-all` | 전체 읽음 |
| DELETE | `/notifications/{notificationId}` | 알림 삭제 |

**알림 유형(`type`)** — `NotificationType` enum (JSON 소문자 직렬화):
`schedule_created`, `schedule_updated`, `schedule_day_before`, `schedule_today`, `diary_written`,
`comment_on_schedule`, `comment_on_diary`, `member_joined`, `member_left`, `member_removed`,
`ownership_transferred`, `group_delete_scheduled`, `quiz_created`, `quiz_answered`,
`mochi_levelup`, `diko_unlocked`, `announcement`

**에러**: `NOTIFICATION_NOT_FOUND`

---

### 📱 11. Device

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/devices` | FCM 토큰 등록 (동일 토큰 upsert) |
| DELETE | `/devices/{deviceId}` | 디바이스 해제 |

**Request** — `RegisterDeviceRequest`: `token`✅, `platform`✅ (`ios`·`android`). **에러**: `DEVICE_NOT_FOUND`

---

### 📤 12. Upload

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/uploads/images` | 이미지 업로드 (multipart/form-data, PNG/JPEG) |

**Request**: `file`(binary)✅, `purpose`(`PROFILE`·`GROUP_THUMBNAIL`·`DIARY`·`QUIZ`).
**에러**: `FILE_TOO_LARGE`, `INVALID_FILE_TYPE`

---

### 🐣 13. Character (`/character`) — 그룹 공용 모찌

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/character?groupRoomId=` | 그룹 캐릭터 상태 (첫 진입 시 자동 생성) |
| POST | `/character/exp` | 경험치 가산 (레벨업/진화 계산 응답) |
| GET | `/character/stages?groupRoomId=` | 진화 트리 + 도달 여부 |
| POST | `/character/master-game-start` | 마스터 게임 시작 (입장료 코인 차감) |
| POST | `/character/master-game-reward` | 마스터 게임 점수 제출 → 코인 보상 |
| POST | `/character/ad-reward` | 광고 시청 보상 코인 적립 (하루 한도) |

**Request** — `AddExpRequest`: `amount`✅, `source`? / `MasterGameRewardRequest`: `score`✅

**진화 단계(`CharacterStage`)**: EGG(Lv.1) → SPROUT(3) → BLOOM(6) → BLOSSOM(10) → GLOW(15) → MASTER(20).
디코는 Lv.10에 해금, 마스터는 레벨 20 + 챔피언 챌린지 통과 시 진화.

**에러**: `CHARACTER_NOT_FOUND`, `INSUFFICIENT_COIN`, `NOT_MASTER_CHARACTER`, `INVALID_GAME_SCORE`, `AD_REWARD_LIMIT_EXCEEDED`(429)

---

### ❓ 14. CharacterQuiz (`/character-quizzes`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/character-quizzes` | 퀴즈 생성 (그룹 멤버, 4지선다, EXP 배수 1~3) |
| GET | `/character-quizzes?groupRoomId=` | 그룹 퀴즈 목록 (최신순) |
| GET | `/character-quizzes/random?groupRoomId=` | 랜덤 풀기 후보 1건 |
| POST | `/character-quizzes/{quizId}/attempt` | 퀴즈 응시 (정답+보상+캐릭터 상태) |

**Request** — `CreateQuizRequest`: `groupRoomId`✅, `category`✅, `question`✅, `options`✅(4개), `correctIndex`✅, `expMultiplier`✅, `imageUrl`? / `SubmitAttemptRequest`: `selectedIndex`✅, `practice`(기본 false)

**에러**: `QUIZ_NOT_FOUND`, `QUIZ_ALREADY_ATTEMPTED`, `QUIZ_CANNOT_ATTEMPT_OWN`, `QUIZ_NO_AVAILABLE`, `QUIZ_INVALID_OPTION_COUNT`, `QUIZ_OPTION_INVALID`, `QUIZ_QUESTION_INVALID`, `QUIZ_INVALID_CORRECT_INDEX`, `QUIZ_INVALID_MULTIPLIER`, `QUIZ_IMAGE_REQUIRES_DIKO`

---

### 🛍 15. CharacterShop (`/character/shop`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/character/shop?groupRoomId=` | 상점 조회 (카테고리별 + 보유/장착 + 잔액) |
| POST | `/character/shop/items/{itemKey}/buy` | 아이템 구매 (코인 차감, 영구 보유) |
| PUT | `/character/shop/equip` | 아이템 장착 (`EquipItemRequest`: `itemKey`) |
| DELETE | `/character/shop/equip/{itemType}` | 카테고리 슬롯 해제 (SKIN은 default 복귀) |

**카테고리(`ShopItemType`)**: SKIN, HAT, GLASSES, HAIRPIN, ACCESSORY, MISC.
**에러**: `SHOP_ITEM_NOT_FOUND`, `ALREADY_OWNED_ITEM`, `ITEM_NOT_OWNED`, `INSUFFICIENT_COIN`

---

### 🏅 16. Title (`/titles`) — 계정 단위 칭호

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/titles/catalog` | 칭호 카탈로그 (전체 정의) |
| GET | `/titles` | 내 칭호 목록 (조회 시 일기 수 칭호 자동 적재) |
| POST | `/titles/claim` | 칭호 획득 적재 (멱등) |
| GET | `/titles/equipped?groupRoomId=` | 그룹 모찌 장착 칭호 |
| PUT | `/titles/equip` | 그룹 모찌에 칭호 장착/해제 |

**Request** — `ClaimTitlesRequest`: `titles[]`(`code`, `groupRoomId`?) / `EquipTitleRequest`: `groupRoomId`✅, `code`?(null=해제)

**에러**: `TITLE_NOT_OWNED`

---

### 🚫 17. Block

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/blocks/users/{userId}` | 사용자 차단 (전역·단방향) |
| DELETE | `/blocks/users/{userId}` | 차단 해제 |
| GET | `/blocks/users` | 차단 목록 (최신순) |
| POST | `/blocks/content` | 게시물 숨기기 (`HideContentRequest`: `targetType`, `targetId`) |
| DELETE | `/blocks/content` | 게시물 숨김 해제 |

**숨김 대상(`HideTargetType`)**: DIARY, COMMENT, SCHEDULE. **에러**: `CANNOT_BLOCK_SELF`

---

### 🚨 18. Report

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/reports` | 신고 (콘텐츠 신고 시 신고자 본인에게서 자동 숨김) |

**Request** — `CreateReportRequest`: `targetType`✅(`DIARY`·`COMMENT`·`SCHEDULE`·`USER`), `targetId`✅, `reason`✅(`SPAM`·`ABUSE`·`SEXUAL`·`VIOLENCE`·`PRIVACY`·`ETC`), `detail`?, `groupRoomId`?

**에러**: `REPORT_INVALID_TARGET`, `CANNOT_REPORT_SELF`

---

### 📨 19. Inquiry

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/inquiries` | 고객센터 문의 작성 (하루 2건 제한) |
| GET | `/inquiries` | 내 문의 목록 (최신순) |

**Request** — `CreateInquiryRequest`: `content`✅(≤1000자). **에러**: `INQUIRY_CONTENT_REQUIRED`, `INQUIRY_DAILY_LIMIT`(429)

---

### 🖼 20. NicknameExhibit (`/nickname-exhibits`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/nickname-exhibits/access` | 전시관 접근 권한 조회 (버튼 노출 판단) |
| GET | `/nickname-exhibits` | 별명 카드 목록 (접근 허용자만, 미허용 시 403) |

**에러**: `EXHIBIT_ACCESS_DENIED`, `EXHIBIT_NOT_FOUND`

---

### ⚙️ 21. AppConfig

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/app-config` | 앱 운영 설정 조회 (대공지 노출/메시지 + 피드백 노출/URL) |

---

### 🌐 22. Public — DeletionRequest (`/api/web/public`, 비로그인)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/web/public/deletion-requests/account` | 계정 삭제 요청 (`email`) |
| POST | `/api/web/public/deletion-requests/data` | 데이터 삭제 요청 (`email`, `groupRoomName`, `content`) |

> Google Play 데이터 안전성 정책의 계정·데이터 삭제 URL 요건 충족용. 디그팟 어드민 공개 페이지에서 접수.

---

### 🩺 23. Infra (`/api`)

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|:---:|------|
| GET | `/api/healthcheck` | ❌ | 헬스 체크 |
| GET | `/api/authcheck` | ✅ | 인증 토큰 유효성 확인 |

---

## 🛠 어드민 도메인 상세 (`/api/admin/**`, `ROLE_ADMIN`)

| 도메인 | 메서드 | 경로 | 설명 |
|--------|--------|------|------|
| Auth | POST | `/api/admin/auth/login` | 관리자 로그인 (이메일/비밀번호, 비인증 허용) |
| Dashboard | GET | `/api/admin/dashboard/summary` | 요약 통계 (사용자/그룹방/일기/일정/댓글/할일) |
| User | GET | `/api/admin/users` | 사용자 목록 (키워드/권한 필터, 페이징) |
| User | GET | `/api/admin/users/{userId}` | 사용자 상세 |
| User | PATCH | `/api/admin/users/{userId}/role` | 권한 변경 |
| User | PATCH | `/api/admin/users/{userId}/restriction` | 서비스 이용 제한 설정/해제 |
| GroupRoom | GET | `/api/admin/group-rooms` | 그룹방 목록 (삭제 포함 필터) |
| GroupRoom | GET | `/api/admin/group-rooms/{groupRoomId}` | 그룹방 상세 |
| GroupRoom | PATCH | `/api/admin/group-rooms/{groupRoomId}/status` | 상태 변경 (RECOVER/SCHEDULE_DELETE/HARD_DELETE) |
| Diary | GET | `/api/admin/diaries` | 일기 목록 (키워드) |
| Diary | GET | `/api/admin/diaries/{diaryId}` | 일기 상세 |
| Diary | DELETE | `/api/admin/diaries/{diaryId}` | 일기 영구 삭제 |
| Schedule | GET | `/api/admin/schedules` | 일정 목록 |
| Schedule | GET | `/api/admin/schedules/{scheduleId}` | 일정 상세 |
| Character | GET | `/api/admin/characters` | 모찌 목록 (키워드/삭제 포함) |
| Character | GET | `/api/admin/characters/{groupRoomId}` | 모찌 상세 |
| Character | PATCH | `/api/admin/characters/{groupRoomId}` | 모찌 보정 (레벨/코인/디코) |
| Report | GET | `/api/admin/reports` | 신고 목록 (상태/대상 필터) |
| Report | PATCH | `/api/admin/reports/{reportId}/status` | 신고 처리 (RESOLVED/DISMISSED) |
| Inquiry | GET | `/api/admin/inquiries` | 문의 목록 (상태 필터) |
| Inquiry | PATCH | `/api/admin/inquiries/{inquiryId}/answer` | 문의 답변 등록 |
| DeletionRequest | GET | `/api/admin/deletion-requests` | 삭제 요청 목록 (상태 필터) |
| DeletionRequest | PATCH | `/api/admin/deletion-requests/{deletionRequestId}/done` | 처리 완료 |
| Announcement | POST | `/api/admin/announcements` | 공지 발송 (ALL/USER_IDS, 알림+FCM) |
| Announcement | GET | `/api/admin/announcements` | 공지 목록 (키워드) |
| Notification | GET | `/api/admin/notifications` | 알림 목록 (type/groupRoomId/keyword 필터) |
| AppConfig | GET | `/api/admin/app-config` | 운영 설정 조회 |
| AppConfig | PUT | `/api/admin/app-config` | 운영 설정 수정 (`UpdateAppConfigRequest`) |
| Title | GET | `/api/admin/titles/catalog` | 칭호 카탈로그 |
| Title | GET | `/api/admin/titles/users/{userId}` | 사용자 보유 칭호 |
| Title | POST | `/api/admin/titles/grant` | 칭호 부여 (멱등) |
| Title | DELETE | `/api/admin/titles/users/{userId}/{code}` | 칭호 회수 |
| NicknameExhibit | GET | `/api/admin/nickname-exhibits` | 별명 카드 목록 |
| NicknameExhibit | POST | `/api/admin/nickname-exhibits` | 별명 카드 등록 |
| NicknameExhibit | PATCH | `/api/admin/nickname-exhibits/{id}` | 별명 카드 수정 |
| NicknameExhibit | DELETE | `/api/admin/nickname-exhibits/{id}` | 별명 카드 삭제 |
| NicknameExhibit | GET | `/api/admin/nickname-exhibits/access` | 접근 허용 사용자 목록 |
| NicknameExhibit | POST | `/api/admin/nickname-exhibits/access` | 접근 허용 추가 (멱등) |
| NicknameExhibit | DELETE | `/api/admin/nickname-exhibits/access/{userId}` | 접근 허용 해제 |
| RegionMap | GET | `/api/admin/region-map?groupRoomId=` | 채운 지역 목록 |
| RegionMap | POST | `/api/admin/region-map/fill` | 지역 채우기 (멱등) |
| RegionMap | POST | `/api/admin/region-map/unfill` | 지역 채움 해제 |
| RegionMap | DELETE | `/api/admin/region-map?groupRoomId=` | 그룹 채움 전체 해제 |
| DB | GET | `/api/admin/db/tables` | 테이블 목록 |
| DB | GET | `/api/admin/db/tables/{name}/columns` | 컬럼 정보 |
| DB | GET | `/api/admin/db/tables/{name}/rows` | 행 조회 (페이징·정렬, size≤200) |
| DB | POST | `/api/admin/db/tables/{name}/rows` | 행 추가 |
| DB | PATCH | `/api/admin/db/tables/{name}/rows` | 행 수정 (PK 매칭, 1행 강제) |
| DB | DELETE | `/api/admin/db/tables/{name}/rows` | 행 삭제 (PK 매칭, 1행 강제) |
| Log | GET | `/api/admin/logs` | 유저 행동 로그 (actor/action/기간/키워드 필터) |

**관련 어드민 에러**: `ADMIN_NOT_FOUND`, `ADMIN_PASSWORD_MISMATCH`, `NOT_ADMIN_USER`, `INVALID_ROLE`, `USER_RESTRICTED`, `ADMIN_TABLE_NOT_ALLOWED`, `ADMIN_TABLE_NOT_FOUND`, `ADMIN_COLUMN_NOT_ALLOWED`, `ADMIN_PK_NOT_FOUND`, `ADMIN_PK_VALUE_MISSING`, `ADMIN_ROW_NOT_FOUND`, `ADMIN_ROW_AFFECTED_INVALID`, `ADMIN_NO_FIELDS_TO_UPDATE`

---

## 🔒 공통 사항

### 에러 응답

`ErrorCode` enum 기반. 본문:

| 필드 | 타입 | 설명 |
|------|------|------|
| code | string | 에러 코드 (예: `INVITE_CODE_EXPIRED`) |
| message | string | 사용자에게 노출 가능한 메시지 |

### 전체 ErrorCode 목록

| 코드 | HTTP | 메시지 |
|------|:---:|------|
| `SERVER_ERROR` | 500 | 서버 내부 오류가 발생했습니다. |
| `INVALID_PARAMETER` | 400 | 잘못된 파라미터입니다. |
| `PARAMETER_VALIDATION_ERROR` | 400 | 파라미터 검증 에러입니다. |
| `PARAMETER_GRAMMAR_ERROR` | 400 | 잘못된 요청 형식입니다. |
| `RESOURCE_NOT_FOUND` | 404 | 요청한 리소스를 찾을 수 없습니다. |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. |
| `FORBIDDEN` | 403 | 권한이 없습니다. |
| `TOKEN_EXPIRED` | 401 | 토큰이 만료되었습니다. 재로그인이 필요합니다. |
| `TOKEN_INVALID` | 401 | 유효하지 않은 토큰입니다. |
| `ACCESS_TOKEN_INVALID` | 401 | Access Token이 유효하지 않습니다. |
| `REFRESH_TOKEN_INVALID` | 401 | Refresh Token이 유효하지 않습니다. |
| `REFRESH_TOKEN_NOT_FOUND` | 401 | Refresh Token이 존재하지 않습니다. |
| `DUPLICATE_LOGIN` | 409 | 이미 로그인된 상태입니다. |
| `INVALID_PROVIDER` | 400 | 지원하지 않는 소셜 로그인 제공자입니다. |
| `APPLE_JWT_ERROR` | 500 | Apple JWT 처리 중 오류가 발생했습니다. |
| `APPLE_KEY_PARSE_ERROR` | 401 | Apple 공개키 파싱에 실패했습니다. |
| `ID_TOKEN_INVALID` | 401 | 잘못된 ID 토큰입니다. |
| `SOCIAL_AUTH_FAILED` | 401 | 소셜 인증에 실패했습니다. |
| `REQUIRED_TERMS_NOT_AGREED` | 400 | 필수 약관에 동의해야 합니다. |
| `USER_NOT_FOUND` | 404 | 존재하지 않는 사용자입니다. |
| `DUPLICATE_EMAIL` | 409 | 이미 사용 중인 이메일입니다. |
| `NAME_TOO_SHORT` / `NAME_TOO_LONG` | 400 | 닉네임 길이 제한 (2~20자) |
| `INVALID_ROLE` | 400 | 유효하지 않은 역할입니다. |
| `USER_RESTRICTED` | 403 | 서비스 이용이 제한된 계정입니다. |
| `GROUP_ROOM_NOT_FOUND` | 404 | 존재하지 않는 그룹방입니다. |
| `GROUP_ROOM_NAME_TOO_SHORT` / `_TOO_LONG` | 400 | 그룹방명 길이 제한 (2~20자) |
| `MAX_MEMBERS_BELOW_CURRENT` | 400 | 현재 구성원 수보다 적은 값 설정 불가. |
| `GROUP_ROOM_NOT_SCHEDULED_FOR_DELETION` | 400 | 삭제 예약되지 않은 그룹방. |
| `GROUP_ROOM_ALREADY_DELETED` | 410 | 이미 삭제된 그룹방. |
| `OWNS_ACTIVE_GROUP_ROOM` | 409 | 소유 그룹방이 있어 탈퇴 불가 (양도 필요). |
| `GROUP_ROOM_LIMIT_EXCEEDED` | 409 | 참여 가능한 그룹방은 최대 6개. |
| `INVITE_CODE_INVALID` | 404 | 존재하지 않는 초대 코드. |
| `INVITE_CODE_EXPIRED` | 410 | 만료된 초대 코드. |
| `GROUP_ROOM_FULL` | 409 | 그룹방 인원 초과. |
| `ALREADY_JOINED` | 409 | 이미 참여 중인 그룹방. |
| `NOT_GROUP_ROOM_MEMBER` | 403 | 그룹방 구성원이 아님. |
| `NOT_GROUP_ROOM_OWNER` | 403 | 방장 권한 필요. |
| `CANNOT_REMOVE_OWNER` | 400 | 방장은 내보낼 수 없음. |
| `USER_NOT_IN_GROUP_ROOM` | 404 | 그룹방 구성원이 아님. |
| `OWNER_CANNOT_LEAVE` | 400 | 방장은 양도 후 탈퇴 가능. |
| `SCHEDULE_NOT_FOUND` | 404 | 존재하지 않는 일정. |
| `END_DATE_BEFORE_START` | 400 | 종료일이 시작일보다 이전. |
| `END_TIME_BEFORE_START` | 400 | 종료 시간이 시작 시간보다 이전. |
| `INVALID_PARTICIPANT` | 400 | 참여자가 그룹 구성원이 아님. |
| `DIARY_NOT_FOUND` | 404 | 존재하지 않는 일기. |
| `FUTURE_DATE_NOT_ALLOWED` | 400 | 미래 날짜 작성 불가. |
| `DIARY_DATE_TOO_OLD` | 400 | 3개월 이전 날짜 작성 불가. |
| `DIARY_EDIT_WINDOW_EXPIRED` | 400 | 3개월 지난 일기 수정/삭제 불가. |
| `INVALID_WEATHER_VALUE` / `INVALID_MOOD_VALUE` | 400 | 0~3 범위 위반. |
| `COMMENT_NOT_FOUND` | 404 | 존재하지 않는 댓글. |
| `COMMENT_TOO_LONG` | 400 | 댓글 200자 초과. |
| `TODO_NOT_FOUND` | 404 | 존재하지 않는 할 일. |
| `TODO_TEXT_TOO_LONG` | 400 | 할 일 100자 초과. |
| `NOTIFICATION_NOT_FOUND` | 404 | 존재하지 않는 알림. |
| `NOTIFICATION_SETTING_NOT_FOUND` | 404 | 알림 설정 없음. |
| `DEVICE_NOT_FOUND` | 404 | 존재하지 않는 디바이스. |
| `FILE_TOO_LARGE` | 413 | 사진 용량 초과 (100MB 이하). |
| `INVALID_FILE_TYPE` | 400 | PNG/JPEG만 허용. |
| `IMAGE_NOT_FOUND` | 404 | 존재하지 않는 이미지. |
| `CHARACTER_NOT_FOUND` | 404 | 캐릭터 정보 없음. |
| `INSUFFICIENT_COIN` | 400 | 코인 부족. |
| `SHOP_ITEM_NOT_FOUND` | 404 | 존재하지 않는 아이템. |
| `ALREADY_OWNED_ITEM` | 409 | 이미 보유한 아이템. |
| `ITEM_NOT_OWNED` | 400 | 미보유 아이템 장착 불가. |
| `NOT_MASTER_CHARACTER` | 400 | 마스터 단계만 보상 가능. |
| `INVALID_GAME_SCORE` | 400 | 유효하지 않은 게임 점수. |
| `AD_REWARD_LIMIT_EXCEEDED` | 429 | 오늘 광고 보상 한도 초과. |
| `QUIZ_NOT_FOUND` | 404 | 존재하지 않는 퀴즈. |
| `QUIZ_ALREADY_ATTEMPTED` | 409 | 이미 응시한 퀴즈. |
| `QUIZ_CANNOT_ATTEMPT_OWN` | 400 | 직접 만든 퀴즈 응시 불가. |
| `QUIZ_NO_AVAILABLE` | 404 | 풀 수 있는 퀴즈 없음. |
| `QUIZ_INVALID_OPTION_COUNT` | 400 | 선택지는 정확히 4개. |
| `QUIZ_OPTION_INVALID` | 400 | 선택지 1~100자. |
| `QUIZ_QUESTION_INVALID` | 400 | 문제 1~200자. |
| `QUIZ_INVALID_CORRECT_INDEX` | 400 | 정답 번호 1~4. |
| `QUIZ_INVALID_MULTIPLIER` | 400 | EXP 배수 1~3. |
| `QUIZ_IMAGE_REQUIRES_DIKO` | 400 | 사진 퀴즈는 디코 등장 그룹만. |
| `EXHIBIT_NOT_FOUND` | 404 | 존재하지 않는 전시관 카드. |
| `EXHIBIT_ACCESS_DENIED` | 403 | 전시관 접근 권한 없음. |
| `TITLE_NOT_OWNED` | 400 | 미획득 칭호 장착 불가. |
| `REPORT_INVALID_TARGET` | 400 | 잘못된 신고 대상. |
| `CANNOT_REPORT_SELF` | 400 | 자기 자신 신고 불가. |
| `CANNOT_BLOCK_SELF` | 400 | 자기 자신 차단 불가. |
| `INQUIRY_CONTENT_REQUIRED` | 400 | 문의 내용 필요. |
| `INQUIRY_DAILY_LIMIT` | 429 | 문의는 하루 2건. |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 횟수 초과. |
| `ADMIN_*` | 400~500 | 어드민 인증/DB 작업 관련 (위 어드민 섹션 참고) |

### HTTP 상태 코드 요약

| 코드 | 의미 |
|:---:|------|
| 400 | 잘못된 요청 (검증 실패) |
| 401 | 인증 실패 (토큰 없음/만료/무효) |
| 403 | 권한 없음 (비구성원/비방장/이용제한) |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복/이미 참여 등) |
| 410 | 만료 (초대 코드/삭제된 그룹) |
| 413 | 파일 크기 초과 |
| 429 | 요청/한도 초과 |
| 500 | 서버 오류 |

---

## 📊 데이터베이스

DB 구조는 [`ERD.md`](./ERD.md) 참고 — **39개 MySQL 테이블 + 2개 Redis 엔티티**.
주요 도메인: 사용자/인증, 그룹방·일정·일기·할일·댓글, 그룹 캐릭터(모찌)·퀴즈·상점, 칭호,
신고/차단/숨김, 문의, 삭제 요청, 어드민(자격증명·공지·감사 로그·앱 설정).
