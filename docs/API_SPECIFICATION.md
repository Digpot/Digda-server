# API 명세서 — DigDa (그룹 다이어리)

---

## 개요

**프로젝트**: DigDa — 그룹 기반 공유 다이어리 앱
**플랫폼**: Flutter (iOS/Android)
**인증 방식**: 소셜 로그인 (카카오, 네이버, 애플) + 관리자 계정 + JWT
**API 스타일**: RESTful JSON API
**Base URL**: `https://api.digda.app/v1`

### 도메인 네이밍 규칙

| 앱 용어 | API 도메인명 | 리소스 경로 | 설명 |
|---------|-------------|------------|------|
| 그룹방 (방) | **GroupRoom** | `/group-rooms` | 다이어리 방 단위 |
| 일기 (글) | **Diary** | `/group-rooms/:id/diaries` | 개별 일기 글 |
| 구성원 | **Membership** | `/group-rooms/:id/memberships` | 그룹방 내 구성원 관리 |
| 사용자 | **User** | `/users` | 로그인한 사용자 본인 |

> **왜 Member가 아니라 Membership인가?**
> `User`는 "나 자신"의 프로필/설정, `Membership`은 "특정 그룹방 안에서의 소속 관계"를 의미합니다.
> Member라고 하면 User와 혼동되므로, 관계(소속)를 나타내는 Membership으로 명명합니다.

### API 설계 기준

| 항목 | 기준 |
|------|------|
| 인증 | Bearer JWT (Access Token 1시간 + Refresh Token 30일) |
| 날짜 형식 | ISO 8601 (`2026-03-19T09:00:00Z`) |
| 페이지네이션 | Offset 기반 (`?limit=20&offset=0`) |
| 에러 응답 | `{ "error": { "code": "...", "message": "..." } }` |
| 이미지 업로드 | Multipart form-data |
| 삭제 전략 | 그룹방: Soft Delete (7일 복구), 나머지: Hard Delete |
| 권한 체크 | 그룹방 구성원 여부 → 리소스 소유자/방장 여부 순서 |
| HTTP 메서드 | GET(조회), POST(생성), PUT(전체수정), PATCH(부분수정), DELETE(삭제) |

---

## 도메인 목록

| # | 도메인 | 브랜치 | 엔드포인트 수 | 설명 |
|---|--------|--------|:---:|------|
| 1 | Auth | `feature/auth-api` | 6 | 소셜 로그인, 토큰 갱신, 약관 동의, 로그아웃, 회원 탈퇴 |
| 2 | User | `feature/user-api` | 6 | 프로필 조회/수정, 알림 설정, 개인정보 설정 |
| 3 | GroupRoom | `feature/group-room-api` | 6 | 그룹방 생성, 조회, 수정, 삭제, 복구 |
| 4 | Invite | `feature/invite-api` | 3 | 초대 코드 생성, 검증, 참여 |
| 5 | Membership | `feature/membership-api` | 4 | 구성원 목록, 내보내기, 역할 변경, 탈퇴 |
| 6 | Schedule | `feature/schedule-api` | 5 | 일정 CRUD, 참여자 관리 |
| 7 | Diary | `feature/diary-api` | 6 | 일기 CRUD, 이미지 첨부, 캘린더 조회 |
| 8 | Comment | `feature/comment-api` | 4 | 일정/일기 댓글 작성/삭제 |
| 9 | Todo | `feature/todo-api` | 4 | 할 일 CRUD, 완료 토글 |
| 10 | Notification | `feature/notification-api` | 4 | 알림 목록, 읽음 처리, 삭제 |
| 11 | Device | `feature/device-api` | 2 | 푸시 알림용 디바이스 토큰 등록/해제 |
| 12 | Upload | `feature/upload-api` | 1 | 이미지 업로드 (단일) |
| 13 | Admin | `feature/admin-api` | 22 | 관리자 로그인, 대시보드, 도메인 관리, DB 조회/편집, 로그, 공지 발송/조회 |
| | | **합계** | **73** | |

---

## 도메인별 상세

---

### 1. Auth (인증)

- **브랜치**: `feature/auth-api`
- **설명**: 소셜 로그인, 온보딩(약관 동의), JWT 토큰 관리, 로그아웃, 회원 탈퇴
- **인증 필요**: 로그인/약관조회/토큰갱신 제외 전부

---

#### 1-1. 소셜 로그인

**POST** `/auth/login`

소셜 프로바이더 토큰으로 로그인. 최초 로그인 시 계정 자동 생성.
유저 식별은 **소셜 고유 ID + provider 조합**으로 수행 (email 기반 아님).

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| provider | string | O | `kakao` / `naver` / `apple` |
| accessToken | string | O | 소셜 프로바이더 액세스 토큰 |
| idToken | string | 조건부 | Apple Sign In 시 필수 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | string | JWT 액세스 토큰 (만료: 1시간) |
| refreshToken | string | 리프레시 토큰 (만료: 30일) |
| user | User | 사용자 정보 |
| isNewUser | boolean | 신규 가입 여부 (true면 약관 동의 화면으로) |

---

#### 1-2. 약관 동의 (온보딩)

**POST** `/auth/terms`

신규 가입자가 필수/선택 약관에 동의. `isNewUser: true`일 때만 호출.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| termsOfService | boolean | O | 이용약관 동의 (필수) |
| privacyPolicy | boolean | O | 개인정보처리방침 동의 (필수) |
| ageConfirmation | boolean | O | 만 14세 이상 확인 (필수) |
| marketingConsent | boolean | - | 마케팅 수신 동의 (선택) |
| pushConsent | boolean | - | 푸시 알림 수신 동의 (선택) |

**Response** `200 OK`

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `REQUIRED_TERMS_NOT_AGREED` | 400 | 필수 약관 미동의 |

---

#### 1-3. 약관 문서 조회

**GET** `/auth/terms/:type`

약관 전문 HTML 조회. 인증 불필요.

**Path Parameters**

| 파라미터 | 설명 |
|----------|------|
| type | `terms-of-service` / `privacy-policy` / `marketing` |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| title | string | 약관 제목 |
| content | string | 약관 본문 (HTML) |
| version | string | 약관 버전 (`v1.0`) |
| updatedAt | string | 최종 수정일 |

---

#### 1-4. 토큰 갱신

**POST** `/auth/refresh`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| refreshToken | string | O | 기존 리프레시 토큰 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | string | 새 액세스 토큰 |
| refreshToken | string | 새 리프레시 토큰 (Rotation 적용) |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `TOKEN_EXPIRED` | 401 | 리프레시 토큰 만료 → 재로그인 필요 |
| `TOKEN_INVALID` | 401 | 유효하지 않은 토큰 |

---

#### 1-5. 로그아웃

**POST** `/auth/logout`

서버 측 리프레시 토큰 무효화 + 소셜 토큰 삭제.

**Response** `200 OK`

---

#### 1-6. 회원 탈퇴

**DELETE** `/auth/account`

계정 영구 삭제. 소유 중인 그룹방이 있으면 먼저 양도 필요.

**Response** `200 OK`

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `OWNS_ACTIVE_GROUP_ROOM` | 409 | 소유 중인 그룹방이 있어 탈퇴 불가. 방장 양도 후 재시도. |

---

### 2. User (사용자)

- **브랜치**: `feature/user-api`
- **설명**: 내 프로필 조회/수정, 알림 설정, 개인정보 설정
- **인증 필요**: 전부

---

#### 2-1. 내 프로필 조회

**GET** `/users/me`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 사용자 UUID |
| name | string | 닉네임 (2~20자) |
| email | string? | 소셜 계정 이메일 (nullable — provider가 미제공 시 null) |
| profileImage | string? | 프로필 이미지 URL (null이면 기본 아바타) |
| statusMessage | string? | 상태 메시지 (최대 100자) |
| provider | string | `kakao` / `naver` / `apple` / `admin` |
| createdAt | string | 가입일 |

---

#### 2-2. 프로필 수정

**PUT** `/users/me`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| name | string | - | 닉네임 (2~20자) |
| statusMessage | string? | - | 상태 메시지 (빈 문자열 = 삭제, 최대 100자) |
| profileImageId | string? | - | Upload API로 받은 이미지 ID (null = 기본 아바타로 초기화) |

**Response** `200 OK` — 수정된 User 객체

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `NAME_TOO_SHORT` | 400 | 닉네임 2자 미만 |
| `NAME_TOO_LONG` | 400 | 닉네임 20자 초과 |
| `STATUS_MESSAGE_TOO_LONG` | 400 | 상태 메시지 100자 초과 |

---

#### 2-3. 알림 설정 조회

**GET** `/users/me/notification-settings`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| pushEnabled | boolean | 푸시 알림 전체 ON/OFF |
| scheduleNotification | boolean | 일정 관련 알림 |
| diaryNotification | boolean | 일기 관련 알림 |
| commentNotification | boolean | 댓글 알림 |
| marketingConsent | boolean | 마케팅 수신 동의 |

---

#### 2-4. 알림 설정 수정

**PUT** `/users/me/notification-settings`

**Request Body** — 위 응답과 동일 구조 (변경할 필드만 전송)

**Response** `200 OK` — 수정된 설정 객체

---

#### 2-5. 개인정보 설정 조회

**GET** `/users/me/privacy-settings`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| profilePublic | boolean | 프로필 공개 여부 |
| activityVisible | boolean | 활동 내역 공개 여부 |

---

#### 2-6. 개인정보 설정 수정

**PUT** `/users/me/privacy-settings`

**Request Body** — 위 응답과 동일 구조 (변경할 필드만 전송)

**Response** `200 OK` — 수정된 설정 객체

---

### 3. GroupRoom (그룹방)

- **브랜치**: `feature/group-room-api`
- **설명**: 그룹방 생성, 목록 조회, 상세 조회, 수정, 삭제(Soft Delete 7일), 복구
- **인증 필요**: 전부
- **권한**: 수정/삭제/복구는 방장(owner)만

---

#### 3-1. 그룹방 생성

**POST** `/group-rooms`

생성 시 초대 코드 자동 발급. 생성자가 방장(owner)이 됨.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| name | string | O | 그룹방명 (2~20자) |
| maxMembers | int | O | 최대 인원 (2~99) |
| thumbnailImageId | string | - | Upload API로 받은 이미지 ID |

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRoom | GroupRoom | 생성된 그룹방 |
| inviteCode | string | 자동 발급된 6자리 초대 코드 |
| inviteCodeExpiresAt | string | 초대 코드 만료 시각 (24시간 후) |

---

#### 3-2. 내 그룹방 목록

**GET** `/group-rooms`

내가 속한 모든 그룹방. 최근 활동순 정렬.

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRooms | GroupRoomListItem[] | 그룹방 배열 |

GroupRoomListItem 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 그룹방 ID |
| name | string | 그룹방명 |
| thumbnailImage | string? | 썸네일 이미지 URL |
| memberCount | int | 현재 구성원 수 |
| maxMembers | int | 최대 인원 |
| myRole | string | 내 역할 (`owner` / `member`) |
| lastActivityAt | string | 마지막 활동 시각 |
| isDeleteScheduled | boolean | 삭제 예약 여부 |

---

#### 3-3. 그룹방 상세 조회

**GET** `/group-rooms/:groupRoomId`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRoom | GroupRoom | 그룹방 정보 |
| memberships | MembershipSummary[] | 구성원 목록 (간략) |
| myRole | string | 내 역할 |
| inviteCode | string? | 현재 유효한 초대 코드 (방장에게만 노출) |

---

#### 3-4. 그룹방 수정

**PUT** `/group-rooms/:groupRoomId`

방장만 가능.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| name | string | - | 그룹방명 (2~20자) |
| maxMembers | int | - | 최대 인원 (현재 구성원 수 이상이어야 함) |
| thumbnailImageId | string? | - | 새 썸네일 이미지 ID (null = 삭제) |

**Response** `200 OK` — 수정된 GroupRoom 객체

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `MAX_MEMBERS_BELOW_CURRENT` | 400 | 현재 구성원 수보다 적은 값으로 설정 시도 |

---

#### 3-5. 그룹방 삭제 (Soft Delete)

**DELETE** `/group-rooms/:groupRoomId`

방장만 가능. 즉시 삭제가 아닌 7일 후 영구 삭제 예약.

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| deleteScheduledAt | string | 영구 삭제 예정 시각 (7일 후) |

---

#### 3-6. 그룹방 복구

**POST** `/group-rooms/:groupRoomId/recover`

방장만 가능. 삭제 예약 기간(7일) 내에만 복구 가능.

**Response** `200 OK` — 복구된 GroupRoom 객체

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `GROUP_ROOM_NOT_SCHEDULED_FOR_DELETION` | 400 | 삭제 예약되지 않은 그룹방 |
| `GROUP_ROOM_ALREADY_DELETED` | 410 | 이미 영구 삭제됨 |

---

### 4. Invite (초대)

- **브랜치**: `feature/invite-api`
- **설명**: 초대 코드 생성/검증/참여
- **인증 필요**: 전부

---

#### 4-1. 초대 코드 생성 (재발급)

**POST** `/group-rooms/:groupRoomId/invites`

방장만 가능. 기존 코드 무효화 후 새 코드 발급. 유효기간 24시간.

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| code | string | 6자리 영숫자 초대 코드 (대소문자 무시) |
| expiresAt | string | 만료 시각 |

---

#### 4-2. 초대 코드 검증

**POST** `/invites/validate`

코드 입력 시 그룹방 미리보기. 참여 전 확인용.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| code | string | O | 6자리 초대 코드 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRoomName | string | 그룹방명 |
| thumbnailImage | string? | 썸네일 이미지 URL |
| memberCount | int | 현재 구성원 수 |
| maxMembers | int | 최대 인원 |
| expiresAt | string | 코드 만료 시각 |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `INVITE_CODE_INVALID` | 404 | 존재하지 않는 코드 |
| `INVITE_CODE_EXPIRED` | 410 | 만료된 코드 |
| `GROUP_ROOM_FULL` | 409 | 인원 초과 |
| `ALREADY_JOINED` | 409 | 이미 참여 중인 그룹방 |

---

#### 4-3. 초대 코드로 참여

**POST** `/invites/join`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| code | string | O | 6자리 초대 코드 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRoom | GroupRoom | 참여한 그룹방 정보 |
| memberships | MembershipSummary[] | 전체 구성원 목록 |

> 참여 성공 시 기존 구성원에게 `member_joined` 푸시 알림 발송

---

### 5. Membership (구성원 관리)

- **브랜치**: `feature/membership-api`
- **설명**: 그룹방 내 구성원 목록 조회, 내보내기(추방), 역할 변경(방장 양도), 자발적 탈퇴
- **인증 필요**: 전부
- **권한**: 내보내기/역할 변경은 방장만

---

#### 5-1. 구성원 목록

**GET** `/group-rooms/:groupRoomId/memberships`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| memberships | Membership[] | 구성원 배열 |

Membership 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | string | 사용자 UUID |
| name | string | 닉네임 |
| profileImage | string? | 프로필 이미지 URL |
| color | string | 구성원 컬러 (hex, 캘린더 표시용) |
| role | string | `owner` / `member` |
| joinedAt | string | 참여일 |

---

#### 5-2. 구성원 내보내기 (추방)

**DELETE** `/group-rooms/:groupRoomId/memberships/:userId`

방장만 가능. 클라이언트에서 2단계 확인 UI 처리.

**Response** `204 No Content`

> 내보내진 사용자에게 `member_removed` 푸시 알림 발송

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `CANNOT_REMOVE_OWNER` | 400 | 방장은 내보낼 수 없음 |
| `USER_NOT_IN_GROUP_ROOM` | 404 | 해당 그룹방 구성원이 아님 |

---

#### 5-3. 역할 변경 (방장 양도)

**PUT** `/group-rooms/:groupRoomId/memberships/:userId/role`

현재 방장만 가능. 지정한 구성원에게 방장을 양도하면 기존 방장은 일반 구성원이 됨.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| role | string | O | `owner` (양도 대상) |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| memberships | Membership[] | 변경된 전체 구성원 목록 |

---

#### 5-4. 그룹방 탈퇴

**POST** `/group-rooms/:groupRoomId/leave`

자발적 탈퇴. 방장은 양도 후에만 탈퇴 가능.

**Response** `204 No Content`

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `OWNER_CANNOT_LEAVE` | 400 | 방장은 양도 후 탈퇴 가능 |

---

### 6. Schedule (일정)

- **브랜치**: `feature/schedule-api`
- **설명**: 그룹방 일정 CRUD, 참여자 지정, 월별 조회
- **인증 필요**: 전부
- **권한**: 수정/삭제는 작성자 또는 방장

---

#### 6-1. 일정 목록 (기간 조회)

**GET** `/group-rooms/:groupRoomId/schedules`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:---:|------|
| startDate | string | O | 조회 시작일 (`2026-03-01`) |
| endDate | string | O | 조회 종료일 (`2026-03-31`) |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| schedules | Schedule[] | 기간 내 일정 목록 |

Schedule 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 일정 ID |
| title | string | 제목 (최대 50자) |
| color | string | 색상 hex |
| startDate | string | 시작일 |
| endDate | string | 종료일 |
| startTime | string? | 시작 시간 `HH:mm` (종일이면 null) |
| endTime | string? | 종료 시간 `HH:mm` |
| allDay | boolean | 종일 여부 |
| participants | UserSummary[] | 참여자 목록 |
| createdBy | UserSummary | 작성자 |
| commentCount | int | 댓글 수 |
| createdAt | string | 작성일시 |

---

#### 6-2. 일정 상세

**GET** `/group-rooms/:groupRoomId/schedules/:scheduleId`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| schedule | Schedule | 일정 전체 정보 |
| comments | Comment[] | 댓글 목록 (시간순) |

---

#### 6-3. 일정 생성

**POST** `/group-rooms/:groupRoomId/schedules`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| title | string | O | 제목 (1~50자) |
| color | string | O | 색상 hex |
| startDate | string | O | 시작일 (`YYYY-MM-DD`) |
| endDate | string | O | 종료일 (`YYYY-MM-DD`, 시작일 이상) |
| startTime | string | - | 시작 시간 (`HH:mm`, allDay=false일 때) |
| endTime | string | - | 종료 시간 (`HH:mm`) |
| allDay | boolean | O | 종일 여부 |
| participantIds | string[] | - | 참여자 사용자 UUID 배열 |

**Response** `201 Created` — Schedule 객체

> 그룹방 구성원에게 `schedule_created` 푸시 알림 발송

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `END_DATE_BEFORE_START` | 400 | 종료일이 시작일보다 이전 |
| `END_TIME_BEFORE_START` | 400 | 같은 날인데 종료 시간이 시작 시간보다 이전 |
| `INVALID_PARTICIPANT` | 400 | 참여자가 그룹방 구성원이 아님 |

---

#### 6-4. 일정 수정

**PUT** `/group-rooms/:groupRoomId/schedules/:scheduleId`

작성자 또는 방장만 가능. Request Body는 생성과 동일 (변경할 필드만).

**Response** `200 OK` — 수정된 Schedule 객체

> 참여자에게 `schedule_updated` 푸시 알림 발송

---

#### 6-5. 일정 삭제

**DELETE** `/group-rooms/:groupRoomId/schedules/:scheduleId`

작성자 또는 방장만 가능.

**Response** `204 No Content`

---

### 7. Diary (일기)

- **브랜치**: `feature/diary-api`
- **설명**: 개별 일기 글 CRUD, 이미지 첨부(1장), 날씨/기분 선택, 캘린더용 날짜 조회
- **인증 필요**: 전부
- **권한**: 수정/삭제는 작성자 또는 방장

---

#### 7-1. 일기 목록 (최신순)

**GET** `/group-rooms/:groupRoomId/diaries`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:---:|------|
| month | string | - | 월 필터 (`2026-03`) |
| limit | int | - | 페이지 크기 (기본 20, 최대 100) |
| offset | int | - | 오프셋 (기본 0) |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| diaries | DiarySummary[] | 일기 목록 (본문 미포함) |
| total | int | 전체 개수 |

DiarySummary 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 일기 ID |
| title | string | 제목 |
| date | string | 날짜 |
| weather | int | 날씨 (0=맑음, 1=흐림, 2=비, 3=눈) |
| mood | int | 기분 (0=행복, 1=사랑, 2=웃음, 3=뿌듯) |
| imageUrl | string? | 이미지 URL (썸네일용) |
| createdBy | UserSummary | 작성자 |
| commentCount | int | 댓글 수 |
| createdAt | string | 작성일시 |

---

#### 7-2. 일기 캘린더 (날짜별 존재 여부)

**GET** `/group-rooms/:groupRoomId/diaries/calendar`

캘린더에 dot marker를 표시하기 위한 경량 API.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:---:|------|
| month | string | O | 조회 월 (`2026-03`) |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| dates | string[] | 일기가 존재하는 날짜 배열 (`["2026-03-01", "2026-03-05", ...]`) |

---

#### 7-3. 일기 상세

**GET** `/group-rooms/:groupRoomId/diaries/:diaryId`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| diary | Diary | 일기 전체 정보 |
| comments | Comment[] | 댓글 목록 (시간순) |

Diary 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 일기 ID |
| title | string | 제목 (최대 20자) |
| content | string | 본문 (최대 300자) |
| date | string | 날짜 |
| weather | int | 날씨 |
| mood | int | 기분 |
| imageUrl | string? | 이미지 URL |
| createdBy | UserSummary | 작성자 |
| createdAt | string | 작성일시 |
| updatedAt | string | 수정일시 |

---

#### 7-4. 일기 작성

**POST** `/group-rooms/:groupRoomId/diaries`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| title | string | O | 제목 (1~20자) |
| content | string | O | 본문 (1~300자) |
| date | string | O | 날짜 (`YYYY-MM-DD`, 오늘 이전만 가능) |
| weather | int | O | 0~3 |
| mood | int | O | 0~3 |
| imageId | string? | - | Upload API로 받은 이미지 ID (1장) |

**Response** `201 Created` — Diary 객체

> 그룹방 구성원에게 `diary_written` 푸시 알림 발송

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `FUTURE_DATE_NOT_ALLOWED` | 400 | 미래 날짜에 일기 작성 시도 |
| `INVALID_WEATHER_VALUE` | 400 | weather가 0~3 범위 밖 |
| `INVALID_MOOD_VALUE` | 400 | mood가 0~3 범위 밖 |

---

#### 7-5. 일기 수정

**PUT** `/group-rooms/:groupRoomId/diaries/:diaryId`

작성자 또는 방장만 가능. Request Body는 작성과 동일 (변경할 필드만).

**Response** `200 OK` — 수정된 Diary 객체

---

#### 7-6. 일기 삭제

**DELETE** `/group-rooms/:groupRoomId/diaries/:diaryId`

작성자 또는 방장만 가능.

**Response** `204 No Content`

---

### 8. Comment (댓글)

- **브랜치**: `feature/comment-api`
- **설명**: 일정/일기에 댓글 작성/삭제
- **인증 필요**: 전부
- **권한**: 삭제는 댓글 작성자 또는 방장

---

#### 8-1. 일정 댓글 작성

**POST** `/group-rooms/:groupRoomId/schedules/:scheduleId/comments`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| text | string | O | 댓글 내용 (1~200자) |

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 댓글 ID |
| text | string | 내용 |
| createdBy | UserSummary | 작성자 |
| createdAt | string | 작성 시각 |

> 일정 작성자 + 기존 댓글 작성자에게 `comment_on_schedule` 푸시 알림 발송

---

#### 8-2. 일기 댓글 작성

**POST** `/group-rooms/:groupRoomId/diaries/:diaryId/comments`

Request/Response는 일정 댓글과 동일.

> 일기 작성자 + 기존 댓글 작성자에게 `comment_on_diary` 푸시 알림 발송

---

#### 8-3. 일정 댓글 삭제

**DELETE** `/group-rooms/:groupRoomId/schedules/:scheduleId/comments/:commentId`

**Response** `204 No Content`

---

#### 8-4. 일기 댓글 삭제

**DELETE** `/group-rooms/:groupRoomId/diaries/:diaryId/comments/:commentId`

**Response** `204 No Content`

---

### 9. Todo (할 일)

- **브랜치**: `feature/todo-api`
- **설명**: 그룹방 공유 할 일 목록 — 생성, 완료 토글, 삭제
- **인증 필요**: 전부
- **권한**: 삭제는 작성자 또는 방장, 토글은 모든 구성원

---

#### 9-1. 할 일 목록

**GET** `/group-rooms/:groupRoomId/todos`

미완료 → 완료 순서, 각각 생성일 기준 정렬.

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| todos | Todo[] | 할 일 배열 |
| progress | Progress | 진행률 |

Todo 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 할 일 ID |
| text | string | 내용 (최대 100자) |
| completed | boolean | 완료 여부 |
| completedAt | string? | 완료 시각 |
| completedBy | UserSummary? | 완료 처리한 사용자 |
| createdBy | UserSummary | 작성자 |
| createdAt | string | 작성일 |

Progress 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| total | int | 전체 개수 |
| completed | int | 완료 개수 |
| percent | int | 진행률 (0~100) |

---

#### 9-2. 할 일 생성

**POST** `/group-rooms/:groupRoomId/todos`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| text | string | O | 내용 (1~100자) |

**Response** `201 Created` — Todo 객체

---

#### 9-3. 할 일 완료 토글

**PATCH** `/group-rooms/:groupRoomId/todos/:todoId`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| completed | boolean | O | 완료 여부 |

**Response** `200 OK` — 수정된 Todo 객체

---

#### 9-4. 할 일 삭제

**DELETE** `/group-rooms/:groupRoomId/todos/:todoId`

**Response** `204 No Content`

---

### 10. Notification (알림)

- **브랜치**: `feature/notification-api`
- **설명**: 인앱 알림 목록 조회, 읽음 처리, 전체 읽음, 삭제
- **인증 필요**: 전부

---

#### 10-1. 알림 목록

**GET** `/notifications`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:---:|------|
| limit | int | - | 페이지 크기 (기본 20, 최대 100) |
| offset | int | - | 오프셋 (기본 0) |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| notifications | Notification[] | 알림 배열 (최신순) |
| total | int | 전체 개수 |
| unreadCount | int | 읽지 않은 수 |

Notification 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 알림 ID |
| type | string | 알림 유형 |
| title | string | 알림 제목 |
| message | string | 알림 메시지 |
| groupRoomId | string | 관련 그룹방 ID |
| groupRoomName | string | 그룹방명 |
| relatedId | string? | 관련 리소스 ID (일정/일기) |
| relatedType | string? | 관련 리소스 타입 (`schedule` / `diary` / `comment`) |
| isRead | boolean | 읽음 여부 |
| createdAt | string | 생성 시각 |

**알림 유형 (type) 전체 목록**

| type | 트리거 시점 | 수신 대상 | 푸시 발송 |
|------|-----------|----------|:---:|
| `schedule_created` | 일정 생성 | 그룹방 전체 구성원 (작성자 제외) | O |
| `schedule_updated` | 일정 수정 | 해당 일정 참여자 (수정자 제외) | O |
| `diary_written` | 일기 작성 | 그룹방 전체 구성원 (작성자 제외) | O |
| `comment_on_schedule` | 일정에 댓글 | 일정 작성자 + 기존 댓글 작성자 (본인 제외) | O |
| `comment_on_diary` | 일기에 댓글 | 일기 작성자 + 기존 댓글 작성자 (본인 제외) | O |
| `member_joined` | 새 구성원 참여 | 기존 구성원 전체 | O |
| `member_removed` | 구성원 내보내기 | 내보내진 사용자 | O |
| `group_delete_scheduled` | 그룹방 삭제 예약 | 전체 구성원 | O |

---

#### 10-2. 알림 읽음 처리

**PATCH** `/notifications/:notificationId`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| isRead | boolean | O | `true` |

**Response** `204 No Content`

---

#### 10-3. 전체 읽음 처리

**POST** `/notifications/read-all`

**Response** `204 No Content`

---

#### 10-4. 알림 삭제

**DELETE** `/notifications/:notificationId`

**Response** `204 No Content`

---

### 11. Device (디바이스)

- **브랜치**: `feature/device-api`
- **설명**: 푸시 알림 발송을 위한 디바이스 토큰(FCM) 등록/해제
- **인증 필요**: 전부

---

#### 11-1. 디바이스 토큰 등록

**POST** `/devices`

앱 시작 시 또는 토큰 갱신 시 호출. 동일 토큰이면 upsert.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| token | string | O | FCM 디바이스 토큰 |
| platform | string | O | `ios` / `android` |

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| deviceId | string | 등록된 디바이스 ID |

---

#### 11-2. 디바이스 토큰 해제

**DELETE** `/devices/:deviceId`

로그아웃 시 또는 토큰 만료 시 호출.

**Response** `204 No Content`

---

### 12. Upload (업로드)

- **브랜치**: `feature/upload-api`
- **설명**: 이미지 업로드 (프로필, 그룹방 썸네일, 일기 사진)
- **인증 필요**: 전부

---

#### 12-1. 이미지 업로드

**POST** `/uploads/images`

Multipart form-data 방식.

**Request**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| file | binary | O | 이미지 파일 (PNG/JPEG, 최대 5MB) |
| purpose | string | O | `profile` / `group_thumbnail` / `diary` |

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 이미지 ID (다른 API에서 참조용) |
| url | string | CDN 접근 URL |
| width | int | 너비 (px) |
| height | int | 높이 (px) |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `FILE_TOO_LARGE` | 413 | 5MB 초과 |
| `INVALID_FILE_TYPE` | 400 | PNG/JPEG 외 파일 |

---

### 13. Admin (관리자)

- **브랜치**: `feature/admin-api`
- **설명**: 관리자(ADMIN) 전용 API. 관리자 로그인, 대시보드 요약, 사용자/그룹방/일기/일정 관리, DB 메타·데이터 조회 및 행 편집, 유저 행동 로그 조회, 공지 발송/이력 조회
- **인증 필요**: 로그인(`/api/admin/auth/login`) 제외 전부. 모든 엔드포인트는 `ROLE_ADMIN` 권한을 요구하며 일반 사용자 토큰으로는 접근 불가
- **공통 페이지네이션 응답 형식**

  ```json
  {
    "page": 0,
    "size": 20,
    "totalElements": 123,
    "totalPages": 7,
    "content": [ ... ]
  }
  ```

---

#### 13-1. 관리자 로그인

**POST** `/api/admin/auth/login`

이메일/비밀번호 기반 관리자 로그인. `admin_credential` 테이블에서 이메일로 조회 후 BCrypt로 비밀번호 검증하고, 연결된 `user.role == ADMIN`일 때만 JWT 토큰을 발급한다. 인증 불필요.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| email | string | O | 관리자 이메일 |
| password | string | O | 관리자 비밀번호 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| adminId | string | 관리자 사용자 ID(UUID) |
| email | string | 관리자 이메일 |
| name | string | 관리자 이름 |
| accessToken | string | JWT Access Token |
| refreshToken | string | JWT Refresh Token |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `ADMIN_NOT_FOUND` | 404 | 해당 이메일의 관리자 계정 없음 |
| `ADMIN_PASSWORD_MISMATCH` | 401 | 비밀번호 불일치 |
| `NOT_ADMIN_USER` | 403 | 연결된 유저가 ADMIN 권한이 아님 |

---

#### 13-2. 대시보드 요약 통계

**GET** `/api/admin/dashboard/summary`

총 사용자/그룹방/일기/일정/댓글/할 일/알림 등 핵심 지표를 한 번에 반환.

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| totalUsers | long | 총 사용자 수 |
| adminUsers | long | 관리자(ADMIN) 수 |
| totalGroupRooms | long | 총 그룹방 수 |
| activeGroupRooms | long | 활성 그룹방 수 (삭제되지 않음) |
| deleteScheduledGroupRooms | long | 삭제 예약된 그룹방 수 |
| totalDiaries | long | 총 일기 수 |
| totalSchedules | long | 총 일정 수 |
| totalComments | long | 총 댓글 수 |
| totalTodos | long | 총 할 일 수 |
| totalNotifications | long | 총 알림 수 |

---

#### 13-3. 사용자 목록 조회

**GET** `/api/admin/users`

페이징·키워드(이름/이메일)·권한 필터. `createdAt DESC` 정렬.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:---:|:------:|------|
| keyword | string | - | - | 이름/이메일 부분일치 검색 |
| role | string | - | - | `USER` / `ADMIN` |
| page | int | - | 0 | 0-base 페이지 |
| size | int | - | 20 | 페이지 크기 |

**Response** `200 OK` — `AdminPageResponse<AdminUserResponse>` 참조.

`AdminUserResponse` 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | string(UUID) | 사용자 ID |
| email | string | 이메일 (소셜 로그인 시 null 가능) |
| name | string | 이름 |
| statusMessage | string | 상태 메시지 |
| profileImage | string | 프로필 이미지 URL |
| socialProvider | string | `KAKAO` / `NAVER` / `APPLE` |
| role | string | `USER` / `ADMIN` |
| createdAt | string(datetime) | 가입일시 |
| updatedAt | string(datetime) | 수정일시 |

---

#### 13-4. 사용자 상세 조회

**GET** `/api/admin/users/:userId`

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| userId | string(UUID) | 사용자 ID |

**Response** `200 OK` — `AdminUserResponse` (13-3 참조)

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `USER_NOT_FOUND` | 404 | 사용자 없음 |

---

#### 13-5. 사용자 권한 변경

**PATCH** `/api/admin/users/:userId/role`

관리자가 특정 사용자의 권한을 변경한다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| userId | string(UUID) | 사용자 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| role | string | O | `USER` / `ADMIN` |

**Response** `200 OK` — `AdminUserResponse` (변경 후 상태)

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `USER_NOT_FOUND` | 404 | 사용자 없음 |

---

#### 13-6. 그룹방 목록 조회

**GET** `/api/admin/group-rooms`

페이징·키워드(그룹방 이름)·삭제 포함 여부. `createdAt DESC` 정렬.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:---:|:------:|------|
| keyword | string | - | - | 그룹방 이름 부분일치 |
| includeDeleted | boolean | - | true | 삭제/예약 삭제된 그룹방 포함 여부 |
| page | int | - | 0 | 0-base 페이지 |
| size | int | - | 20 | 페이지 크기 |

**Response** `200 OK` — `AdminPageResponse<AdminGroupRoomResponse>`

`AdminGroupRoomResponse` 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRoomId | long | 그룹방 ID |
| name | string | 그룹방 이름 |
| thumbnailImage | string | 썸네일 이미지 URL |
| maxMembers | int | 최대 인원 |
| ownerId | string(UUID) | 방장 ID |
| ownerName | string | 방장 이름 |
| lastActivityAt | string(datetime) | 마지막 활동 시각 |
| deleteScheduledAt | string(datetime) | 삭제 예약 시각 (null이면 미예약) |
| deletedAt | string(datetime) | 삭제 시각 (null이면 미삭제) |
| createdAt | string(datetime) | 생성 시각 |
| updatedAt | string(datetime) | 수정 시각 |

---

#### 13-7. 그룹방 상세 조회

**GET** `/api/admin/group-rooms/:groupRoomId`

**Response** `200 OK` — `AdminGroupRoomResponse` (13-6 참조)

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `GROUP_ROOM_NOT_FOUND` | 404 | 그룹방 없음 |

---

#### 13-8. 그룹방 상태 변경

**PATCH** `/api/admin/group-rooms/:groupRoomId/status`

관리자가 그룹방을 복구/삭제 예약/즉시 삭제 처리한다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| action | string | O | `RECOVER`(복구) / `SCHEDULE_DELETE`(삭제 예약) / `HARD_DELETE`(즉시 삭제) |

**Response** `200 OK` — `AdminGroupRoomResponse` (변경 후 상태)

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `GROUP_ROOM_NOT_FOUND` | 404 | 그룹방 없음 |

---

#### 13-9. 일기 목록 조회

**GET** `/api/admin/diaries`

페이징·키워드(제목/내용). `createdAt DESC` 정렬.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:---:|:------:|------|
| keyword | string | - | - | 제목/내용 부분일치 |
| page | int | - | 0 | 0-base 페이지 |
| size | int | - | 20 | 페이지 크기 |

**Response** `200 OK` — `AdminPageResponse<AdminDiaryResponse>`

`AdminDiaryResponse` 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| diaryId | long | 일기 ID |
| groupRoomId | long | 그룹방 ID |
| groupRoomName | string | 그룹방 이름 |
| createdBy | string(UUID) | 작성자 ID |
| authorName | string | 작성자 이름 |
| title | string | 제목 |
| content | string | 내용 |
| date | string(date) | 일기 날짜 |
| weather | int | 날씨 (0~3) |
| mood | int | 기분 (0~3) |
| imageUrl | string | 이미지 URL |
| createdAt | string(datetime) | 생성 시각 |
| updatedAt | string(datetime) | 수정 시각 |

---

#### 13-10. 일기 상세 조회

**GET** `/api/admin/diaries/:diaryId`

**Response** `200 OK` — `AdminDiaryResponse` (13-9 참조)

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `DIARY_NOT_FOUND` | 404 | 일기 없음 |

---

#### 13-11. 일기 삭제

**DELETE** `/api/admin/diaries/:diaryId`

관리자 권한으로 일기를 영구 삭제한다.

**Response** `204 No Content`

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `DIARY_NOT_FOUND` | 404 | 일기 없음 |

---

#### 13-12. 일정 목록 조회

**GET** `/api/admin/schedules`

페이징·키워드(제목). `createdAt DESC` 정렬.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:---:|:------:|------|
| keyword | string | - | - | 제목 부분일치 |
| page | int | - | 0 | 0-base 페이지 |
| size | int | - | 20 | 페이지 크기 |

**Response** `200 OK` — `AdminPageResponse<AdminScheduleResponse>`

`AdminScheduleResponse` 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| scheduleId | long | 일정 ID |
| groupRoomId | long | 그룹방 ID |
| groupRoomName | string | 그룹방 이름 |
| createdBy | string(UUID) | 작성자 ID |
| authorName | string | 작성자 이름 |
| title | string | 제목 |
| color | string | 색상 (#RRGGBB) |
| startDate | string(date) | 시작 일자 |
| endDate | string(date) | 종료 일자 |
| startTime | string(time) | 시작 시간 (allDay 시 null) |
| endTime | string(time) | 종료 시간 (allDay 시 null) |
| allDay | boolean | 종일 여부 |
| participantCount | int | 참여자 수 |
| createdAt | string(datetime) | 생성 시각 |
| updatedAt | string(datetime) | 수정 시각 |

---

#### 13-13. 일정 상세 조회

**GET** `/api/admin/schedules/:scheduleId`

**Response** `200 OK` — `AdminScheduleResponse` (13-12 참조)

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `SCHEDULE_NOT_FOUND` | 404 | 일정 없음 |

---

#### 13-14. DB 테이블 목록 조회

**GET** `/api/admin/db/tables`

`INFORMATION_SCHEMA.TABLES`를 조회하여 현재 스키마의 `BASE TABLE` 전체 목록을 반환한다.

**Response** `200 OK` — `List<AdminTableInfoResponse>`

| 필드 | 타입 | 설명 |
|------|------|------|
| tableName | string | 테이블명 |
| tableComment | string | 테이블 주석 (없으면 null) |
| approxRowCount | long | 통계 기반 근사 행 수 (MySQL `TABLE_ROWS`) |

---

#### 13-15. DB 테이블 컬럼 정보 조회

**GET** `/api/admin/db/tables/:name/columns`

`INFORMATION_SCHEMA.COLUMNS`를 조회해 특정 테이블의 컬럼 메타데이터를 반환한다. 테이블명은 식별자 화이트리스트(`^[A-Za-z_][A-Za-z0-9_]{0,63}$`)로 검증 후 존재 여부를 확인한다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| name | string | 테이블명 |

**Response** `200 OK` — `List<AdminColumnInfoResponse>` (`ORDINAL_POSITION` 오름차순)

| 필드 | 타입 | 설명 |
|------|------|------|
| columnName | string | 컬럼명 |
| dataType | string | 데이터 타입 (varchar, bigint 등) |
| columnType | string | 컬럼 전체 타입 (varchar(255) 등) |
| nullable | boolean | NULL 허용 여부 |
| defaultValue | string | 기본값 |
| columnKey | string | 키 종류 (PRI/UNI/MUL) |
| comment | string | 컬럼 주석 |
| ordinalPosition | int | 정렬 순서 |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `ADMIN_TABLE_NOT_ALLOWED` | 400 | 테이블명이 화이트리스트 정규식에 맞지 않음 |
| `ADMIN_TABLE_NOT_FOUND` | 404 | 현재 스키마에 해당 테이블 없음 |

---

#### 13-16. DB 테이블 데이터 조회

**GET** `/api/admin/db/tables/:name/rows`

테이블 데이터를 컬럼 기반 페이지 응답으로 반환한다. 테이블/컬럼명은 정규식 화이트리스트로 검증 후 백틱으로 이스케이프하며, `size`는 최대 200으로 제한된다. 정렬은 실제 컬럼 존재 여부 및 방향(ASC/DESC) 검증 후 `ORDER BY \`col\` DIR`로 구성한다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| name | string | 테이블명 |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:---:|:------:|------|
| page | int | - | 0 | 0-base 페이지 |
| size | int | - | 20 | 페이지 크기 (최대 200) |
| orderBy | string | - | - | 정렬 기준 컬럼명 |
| direction | string | - | ASC | `ASC` / `DESC` |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| tableName | string | 테이블명 |
| columns | string[] | 컬럼 순서 (rows 객체의 키 순서) |
| page | int | 현재 페이지 |
| size | int | 페이지 크기 |
| totalElements | long | 총 행 수 |
| totalPages | int | 총 페이지 수 |
| rows | object[] | 각 행은 `{ 컬럼명: 값 }` Map |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `ADMIN_TABLE_NOT_ALLOWED` | 400 | 테이블명이 화이트리스트 정규식에 맞지 않음 |
| `ADMIN_TABLE_NOT_FOUND` | 404 | 현재 스키마에 해당 테이블 없음 |
| `ADMIN_COLUMN_NOT_ALLOWED` | 400 | `orderBy` 컬럼명/형식 또는 `direction` 값이 허용되지 않음 |

---

#### 13-17. 유저 행동 로그 조회

**GET** `/api/admin/logs`

`user_action_log` 테이블을 `createdAt DESC`로 페이징 조회. 행위자/액션/기간/키워드 필터 지원. 키워드는 `detail`, `targetId`에 대한 부분일치 검색이다. (관리자 전용 행위 로그가 아닌, 모든 사용자 행동 로그를 통합 조회한다.)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:---:|:------:|------|
| actorId | string(UUID) | - | - | 행위자 ID (null은 시스템 로그) |
| action | string | - | - | `LOGIN` / `SIGNUP` / `LOGOUT` / `CREATE_DIARY` / `DELETE_DIARY` / `CREATE_SCHEDULE` / `DELETE_SCHEDULE` / `CREATE_COMMENT` / `CREATE_GROUP_ROOM` / `JOIN_GROUP_ROOM` / `LEAVE_GROUP_ROOM` / `TRANSFER_OWNER` / `CREATE_TODO` / `OTHER` |
| from | string(datetime) | - | - | 조회 시작 (ISO-8601) |
| to | string(datetime) | - | - | 조회 종료 (ISO-8601) |
| keyword | string | - | - | `detail`/`targetId` 부분일치 |
| page | int | - | 0 | 0-base 페이지 |
| size | int | - | 20 | 페이지 크기 |

**Response** `200 OK` — `AdminPageResponse<AdminUserLogResponse>`

`AdminUserLogResponse` 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| logId | long | 로그 ID |
| actorId | string(UUID) | 행위자 ID (시스템 로그는 null) |
| action | string | 액션 타입 |
| targetType | string | 대상 타입 (USER/GROUP_ROOM/DIARY/SCHEDULE/COMMENT/TODO 등) |
| targetId | string | 대상 ID |
| detail | string | 상세 메시지 |
| createdAt | string(datetime) | 생성 시각 |

---

#### 13-18. DB 테이블 행 추가

**POST** `/api/admin/db/tables/:name/rows`

`values` 맵의 컬럼만 `INSERT`. 테이블/컬럼명은 정규식 화이트리스트로 검증 후 백틱 이스케이프된다. 값은 문자열로 전달하며 서버가 컬럼 타입에 맞게 변환한다. NULL은 JSON `null`로 전달.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| name | string | 테이블명 |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| values | object | O | `{ 컬럼명: 값(string \| null) }` |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| affected | int | 영향받은 행 수 |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `ADMIN_TABLE_NOT_ALLOWED` | 400 | 테이블명 화이트리스트 위반 |
| `ADMIN_TABLE_NOT_FOUND` | 404 | 테이블 없음 |
| `ADMIN_COLUMN_NOT_ALLOWED` | 400 | 컬럼명/형식 위반 |
| `INVALID_PARAMETER` | 400 | values 비어있거나 값 변환 실패 |

---

#### 13-19. DB 테이블 행 수정

**PATCH** `/api/admin/db/tables/:name/rows`

PK로만 매칭하여 1행 수정. PK 컬럼은 변경 불가하며 `values`에 PK가 포함되어도 무시된다. 영향받는 행이 정확히 1행이 아니면 롤백된다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| name | string | 테이블명 |

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `pk[컬럼명]` | string | 매칭할 PK 컬럼명/값 (다중 PK 가능) |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| values | object | O | 변경할 컬럼 → 값 |

**Response** `200 OK` — `{ affected: int }`

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `ADMIN_TABLE_NOT_ALLOWED` | 400 | 테이블명 화이트리스트 위반 |
| `ADMIN_TABLE_NOT_FOUND` | 404 | 테이블 없음 |
| `ADMIN_COLUMN_NOT_ALLOWED` | 400 | 컬럼명/형식 위반 |
| `INVALID_PARAMETER` | 400 | PK 누락, 영향 행 수 ≠ 1 |

---

#### 13-20. DB 테이블 행 삭제

**DELETE** `/api/admin/db/tables/:name/rows`

PK로만 매칭하여 1행 삭제. WHERE 없는 전체 삭제는 불가. 영향받는 행이 정확히 1행이 아니면 롤백된다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| name | string | 테이블명 |

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `pk[컬럼명]` | string | 매칭할 PK 컬럼명/값 (다중 PK 가능) |

**Response** `200 OK` — `{ affected: int }`

**에러 케이스** — 13-19와 동일.

---

#### 13-21. 공지 발송

**POST** `/api/admin/announcements`

전체 사용자 또는 지정 유저들에게 공지를 발송한다. `notification` 레코드 생성 + FCM 푸시 멀티캐스트 발송이 동시에 일어나며, 발송 이력은 `announcement` 테이블에 1행 저장된다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| title | string | O | 공지 제목 (최대 100자) |
| body | string | O | 공지 본문 (최대 1000자) |
| target | string | O | `ALL` / `USER_IDS` (기본값 `ALL`) |
| userIds | string(UUID)[] | △ | `target=USER_IDS`일 때 필수, 비어있을 수 없음 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| recipientCount | int | 실제 발송된 수신자 수 |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `INVALID_PARAMETER` | 400 | `target` 값 위반 또는 `USER_IDS`인데 `userIds`가 비어있음 |

---

#### 13-22. 공지 목록 조회

**GET** `/api/admin/announcements`

지금까지 발송된 공지 이력을 `createdAt DESC`로 페이징 조회한다. 제목/본문 키워드 검색을 지원한다.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:---:|:------:|------|
| keyword | string | - | - | 제목/본문 부분일치 |
| page | int | - | 0 | 0-base 페이지 |
| size | int | - | 20 | 페이지 크기 |

**Response** `200 OK` — `AdminPageResponse<AdminAnnouncementResponse>`

`AdminAnnouncementResponse` 필드:

| 필드 | 타입 | 설명 |
|------|------|------|
| announcementId | long | 공지 ID |
| title | string | 제목 |
| body | string | 본문 |
| targetType | string | `ALL` / `USER_IDS` |
| recipientCount | int | 발송 당시 수신자 수 |
| createdAt | string(datetime) | 발송 시각 |

---

## 공통 사항

### 인증 헤더

모든 인증 필요 API:

```
Authorization: Bearer {accessToken}
```

### 공통 에러 응답

| HTTP 코드 | 의미 |
|:---------:|------|
| 400 | 잘못된 요청 (유효성 검증 실패, 잘못된 파라미터) |
| 401 | 인증 실패 (토큰 없음, 만료, 유효하지 않음) |
| 403 | 권한 없음 (구성원이 아니거나 방장이 아님) |
| 404 | 리소스를 찾을 수 없음 |
| 409 | 충돌 (이미 참여 중, 중복 데이터) |
| 410 | 만료됨 (초대 코드 만료, 삭제된 리소스) |
| 413 | 파일 크기 초과 |
| 429 | 요청 횟수 초과 (Rate Limit) |
| 500 | 서버 내부 오류 |

에러 응답 본문:

| 필드 | 타입 | 설명 |
|------|------|------|
| error.code | string | 에러 코드 (예: `INVITE_CODE_EXPIRED`) |
| error.message | string | 사용자에게 보여줄 수 있는 메시지 |
| error.details | object? | 유효성 검증 시 필드별 에러 |

### 공통 객체

**UserSummary** — 목록/댓글 등에서 사용자를 간략히 표현

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 사용자 UUID |
| name | string | 닉네임 |
| profileImage | string? | 프로필 이미지 URL |

**Comment** — 일정/일기 댓글 공통 구조

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 댓글 ID |
| text | string | 내용 (최대 200자) |
| createdBy | UserSummary | 작성자 |
| createdAt | string | 작성 시각 |

### Rate Limiting

| 대상 | 제한 |
|------|------|
| 일반 API | 100 req/min |
| 인증 API (`/auth/*`) | 10 req/min |
| 업로드 API (`/uploads/*`) | 30 req/min |

응답 헤더:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1679234567
```

### 페이지네이션 응답 형식

페이지네이션이 적용된 API의 응답:

| 필드 | 타입 | 설명 |
|------|------|------|
| total | int | 전체 결과 수 |
| limit | int | 요청한 페이지 크기 |
| offset | int | 현재 오프셋 |
| hasMore | boolean | 다음 페이지 존재 여부 |

---

## 브랜치 전략

### 브랜치 구조

```
main
 └── develop
      ├── feature/auth-api          ← 1순위
      ├── feature/user-api          ← 2순위
      ├── feature/upload-api        ← 3순위
      ├── feature/group-room-api    ← 4순위
      ├── feature/invite-api        ← 5순위
      ├── feature/membership-api    ← 6순위
      ├── feature/device-api        ← 7순위 (독립)
      ├── feature/schedule-api      ← 8순위
      ├── feature/diary-api         ← 9순위
      ├── feature/comment-api       ← 10순위
      ├── feature/todo-api          ← 11순위 (독립)
      ├── feature/notification-api  ← 12순위
      └── feature/admin-api         ← 13순위
```

### 개발 순서 (의존성 기반)

| 순서 | 브랜치 | 의존 대상 | 이유 |
|:---:|--------|----------|------|
| 1 | `feature/auth-api` | — | 모든 API의 인증 기반, JWT 발급 |
| 2 | `feature/user-api` | Auth | 프로필/설정, 인증된 사용자 필요 |
| 3 | `feature/upload-api` | Auth | 이미지 업로드, 이후 도메인에서 참조 |
| 4 | `feature/group-room-api` | Auth, Upload | 그룹방 CRUD, 썸네일 이미지 |
| 5 | `feature/invite-api` | GroupRoom | 초대 코드는 그룹방에 종속 |
| 6 | `feature/membership-api` | GroupRoom | 구성원 관리는 그룹방에 종속 |
| 7 | `feature/device-api` | Auth | FCM 토큰 등록, Auth만 의존 (병렬 가능) |
| 8 | `feature/schedule-api` | GroupRoom, Membership | 일정은 그룹방+구성원 필요 |
| 9 | `feature/diary-api` | GroupRoom, Upload | 일기는 그룹방+이미지 필요 |
| 10 | `feature/comment-api` | Schedule, Diary | 댓글은 일정/일기에 종속 |
| 11 | `feature/todo-api` | GroupRoom | 할 일은 그룹방에만 종속 (병렬 가능) |
| 12 | `feature/notification-api` | 전체 | 모든 도메인 이벤트를 수신하여 알림+푸시 생성 |
| 13 | `feature/admin-api` | 전체 | 관리자 기능. 각 도메인 Repository 재사용 |

> 7번(Device)과 11번(Todo)은 의존성이 적어 다른 도메인과 병렬 개발 가능

### 머지 규칙

- 각 feature 브랜치 → `develop`으로 PR 생성
- 코드 리뷰 필수 (최소 1인)
- CI 파이프라인 통과 후 머지 (lint + test + build)
- `develop` → `main`은 릴리스 시점에 머지
- 핫픽스: `main`에서 `hotfix/*` 브랜치 생성 → `main` + `develop` 양쪽 머지

---

## 데이터베이스 ERD 요약

| 테이블 | 주요 관계 | 비고 |
|--------|-----------|------|
| user | — | 소셜 로그인 정보 포함 |
| user_terms | user 1:1 | 약관 동의 내역 |
| user_notification_setting | user 1:1 | 알림 설정 |
| user_privacy_setting | user 1:1 | 개인정보 설정 |
| group_room | user N:1 (owner) | Soft Delete 지원 |
| membership | user N:M group_room | 조인 테이블, role/color 포함 |
| invite_code | group_room 1:N | 24시간 만료 |
| schedule | group_room 1:N | 색상, 종일 여부 포함 |
| schedule_participant | user N:M schedule | 일정 참여자 |
| diary | group_room 1:N | 날씨, 기분, 이미지 URL 포함 |
| comment | schedule/diary 다형성 1:N | targetType으로 구분 |
| todo | group_room 1:N | 완료 여부, 완료자 |
| notification | user 1:N | type, relatedId로 연결 |
| device | user 1:N | FCM 토큰, 플랫폼 |
| uploaded_image | user 1:N | purpose별 분류 |
| admin_credential | user 1:1 | 관리자 이메일/비밀번호 (BCrypt) |
| user_action_log | user N:1 (actor, nullable) | 유저 행동 감사 로그 |
| announcement | (독립) | 관리자 공지 발송 이력 |

---

## 엔드포인트 전체 목록 (68개)

| # | 메서드 | 엔드포인트 | 도메인 | 설명 |
|:---:|:---:|------|------|------|
| 1 | POST | `/auth/login` | Auth | 소셜 로그인 |
| 2 | POST | `/auth/terms` | Auth | 약관 동의 |
| 3 | GET | `/auth/terms/:type` | Auth | 약관 문서 조회 |
| 4 | POST | `/auth/refresh` | Auth | 토큰 갱신 |
| 5 | POST | `/auth/logout` | Auth | 로그아웃 |
| 6 | DELETE | `/auth/account` | Auth | 회원 탈퇴 |
| 7 | GET | `/users/me` | User | 내 프로필 조회 |
| 8 | PUT | `/users/me` | User | 프로필 수정 |
| 9 | GET | `/users/me/notification-settings` | User | 알림 설정 조회 |
| 10 | PUT | `/users/me/notification-settings` | User | 알림 설정 수정 |
| 11 | GET | `/users/me/privacy-settings` | User | 개인정보 설정 조회 |
| 12 | PUT | `/users/me/privacy-settings` | User | 개인정보 설정 수정 |
| 13 | POST | `/group-rooms` | GroupRoom | 그룹방 생성 |
| 14 | GET | `/group-rooms` | GroupRoom | 내 그룹방 목록 |
| 15 | GET | `/group-rooms/:groupRoomId` | GroupRoom | 그룹방 상세 |
| 16 | PUT | `/group-rooms/:groupRoomId` | GroupRoom | 그룹방 수정 |
| 17 | DELETE | `/group-rooms/:groupRoomId` | GroupRoom | 그룹방 삭제 |
| 18 | POST | `/group-rooms/:groupRoomId/recover` | GroupRoom | 그룹방 복구 |
| 19 | POST | `/group-rooms/:groupRoomId/invites` | Invite | 초대 코드 생성 |
| 20 | POST | `/invites/validate` | Invite | 초대 코드 검증 |
| 21 | POST | `/invites/join` | Invite | 초대 코드로 참여 |
| 22 | GET | `/group-rooms/:groupRoomId/memberships` | Membership | 구성원 목록 |
| 23 | DELETE | `/group-rooms/:groupRoomId/memberships/:userId` | Membership | 구성원 내보내기 |
| 24 | PUT | `/group-rooms/:groupRoomId/memberships/:userId/role` | Membership | 역할 변경 |
| 25 | POST | `/group-rooms/:groupRoomId/leave` | Membership | 그룹방 탈퇴 |
| 26 | GET | `/group-rooms/:groupRoomId/schedules` | Schedule | 일정 목록 |
| 27 | GET | `/group-rooms/:groupRoomId/schedules/:scheduleId` | Schedule | 일정 상세 |
| 28 | POST | `/group-rooms/:groupRoomId/schedules` | Schedule | 일정 생성 |
| 29 | PUT | `/group-rooms/:groupRoomId/schedules/:scheduleId` | Schedule | 일정 수정 |
| 30 | DELETE | `/group-rooms/:groupRoomId/schedules/:scheduleId` | Schedule | 일정 삭제 |
| 31 | GET | `/group-rooms/:groupRoomId/diaries` | Diary | 일기 목록 |
| 32 | GET | `/group-rooms/:groupRoomId/diaries/calendar` | Diary | 일기 캘린더 |
| 33 | GET | `/group-rooms/:groupRoomId/diaries/:diaryId` | Diary | 일기 상세 |
| 34 | POST | `/group-rooms/:groupRoomId/diaries` | Diary | 일기 작성 |
| 35 | PUT | `/group-rooms/:groupRoomId/diaries/:diaryId` | Diary | 일기 수정 |
| 36 | DELETE | `/group-rooms/:groupRoomId/diaries/:diaryId` | Diary | 일기 삭제 |
| 37 | POST | `/group-rooms/:groupRoomId/schedules/:scheduleId/comments` | Comment | 일정 댓글 작성 |
| 38 | DELETE | `/group-rooms/:groupRoomId/schedules/:scheduleId/comments/:commentId` | Comment | 일정 댓글 삭제 |
| 39 | POST | `/group-rooms/:groupRoomId/diaries/:diaryId/comments` | Comment | 일기 댓글 작성 |
| 40 | DELETE | `/group-rooms/:groupRoomId/diaries/:diaryId/comments/:commentId` | Comment | 일기 댓글 삭제 |
| 41 | GET | `/group-rooms/:groupRoomId/todos` | Todo | 할 일 목록 |
| 42 | POST | `/group-rooms/:groupRoomId/todos` | Todo | 할 일 생성 |
| 43 | PATCH | `/group-rooms/:groupRoomId/todos/:todoId` | Todo | 할 일 토글 |
| 44 | DELETE | `/group-rooms/:groupRoomId/todos/:todoId` | Todo | 할 일 삭제 |
| 45 | GET | `/notifications` | Notification | 알림 목록 |
| 46 | PATCH | `/notifications/:notificationId` | Notification | 알림 읽음 |
| 47 | POST | `/notifications/read-all` | Notification | 전체 읽음 |
| 48 | DELETE | `/notifications/:notificationId` | Notification | 알림 삭제 |
| 49 | POST | `/devices` | Device | 디바이스 토큰 등록 |
| 50 | DELETE | `/devices/:deviceId` | Device | 디바이스 토큰 해제 |
| 51 | POST | `/uploads/images` | Upload | 이미지 업로드 |
| 52 | POST | `/api/admin/auth/login` | Admin | 관리자 로그인 (이메일/비밀번호) |
| 53 | GET | `/api/admin/dashboard/summary` | Admin | 대시보드 주요 통계 |
| 54 | GET | `/api/admin/users` | Admin | 사용자 목록 (페이징/키워드/role) |
| 55 | GET | `/api/admin/users/:userId` | Admin | 사용자 상세 |
| 56 | PATCH | `/api/admin/users/:userId/role` | Admin | 사용자 권한 변경 |
| 57 | GET | `/api/admin/group-rooms` | Admin | 그룹방 목록 (페이징/키워드/삭제 포함) |
| 58 | GET | `/api/admin/group-rooms/:groupRoomId` | Admin | 그룹방 상세 |
| 59 | PATCH | `/api/admin/group-rooms/:groupRoomId/status` | Admin | 그룹방 상태 변경 |
| 60 | GET | `/api/admin/diaries` | Admin | 일기 목록 (페이징/키워드) |
| 61 | GET | `/api/admin/diaries/:diaryId` | Admin | 일기 상세 |
| 62 | DELETE | `/api/admin/diaries/:diaryId` | Admin | 일기 삭제 |
| 63 | GET | `/api/admin/schedules` | Admin | 일정 목록 (페이징/키워드) |
| 64 | GET | `/api/admin/schedules/:scheduleId` | Admin | 일정 상세 |
| 65 | GET | `/api/admin/db/tables` | Admin | DB 전체 테이블 목록 |
| 66 | GET | `/api/admin/db/tables/:name/columns` | Admin | 테이블 컬럼 정보 |
| 67 | GET | `/api/admin/db/tables/:name/rows` | Admin | 테이블 데이터(페이징) |
| 68 | POST | `/api/admin/db/tables/:name/rows` | Admin | 테이블 행 추가 |
| 69 | PATCH | `/api/admin/db/tables/:name/rows` | Admin | 테이블 행 수정 (PK 매칭) |
| 70 | DELETE | `/api/admin/db/tables/:name/rows` | Admin | 테이블 행 삭제 (PK 매칭) |
| 71 | GET | `/api/admin/logs` | Admin | 유저 행동 로그 조회 |
| 72 | POST | `/api/admin/announcements` | Admin | 공지 발송 (ALL/USER_IDS) |
| 73 | GET | `/api/admin/announcements` | Admin | 공지 발송 이력 조회 |

---

## 권한 매트릭스

그룹방 관련 행위는 `방장`/`구성원`/`비구성원` 기준이며, 별도로 **ADMIN**(시스템 관리자 — `user.role == ADMIN`)은 아래 모든 행위 권한과 무관하게 `/api/admin/**` 엔드포인트를 통해 사용자/그룹방/일기/일정/DB/로그 전반을 관리할 수 있다.

| 행동 | 방장 (owner) | 구성원 (member) | 비구성원 | 관리자 (ADMIN) |
|------|:---:|:---:|:---:|:---:|
| 그룹방 조회 | O | O | X | O (admin API) |
| 그룹방 수정/삭제/복구 | O | X | X | O (상태 변경 API) |
| 초대 코드 생성 | O | X | X | - |
| 구성원 내보내기 | O | X | X | - |
| 역할 변경 (방장 양도) | O | X | X | - |
| 일정 생성 | O | O | X | - |
| 일정 수정/삭제 (본인 것) | O | O | X | - |
| 일정 수정/삭제 (타인 것) | O | X | X | - |
| 일기 작성 | O | O | X | - |
| 일기 수정/삭제 (본인 것) | O | O | X | - |
| 일기 수정/삭제 (타인 것) | O | X | X | O (강제 삭제) |
| 댓글 작성 | O | O | X | - |
| 댓글 삭제 (본인 것) | O | O | X | - |
| 댓글 삭제 (타인 것) | O | X | X | - |
| 할 일 생성/토글 | O | O | X | - |
| 할 일 삭제 (본인 것) | O | O | X | - |
| 할 일 삭제 (타인 것) | O | X | X | - |
| 사용자 권한 변경 | X | X | X | O |
| DB 테이블/로그 열람 | X | X | X | O |
