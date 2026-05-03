# 📘 API 명세서 — DigDa (그룹 다이어리)

---

## 📌 개요

**프로젝트**: DigDa — 그룹 기반 공유 다이어리 앱
**플랫폼**: Flutter (iOS/Android)
**인증 방식**: 소셜 로그인 (카카오, 네이버, 애플) + 관리자 계정 + JWT
**API 스타일**: RESTful JSON API
**Base URL**: `https://api.digda.app/v1`

### 도메인 네이밍 규칙

| 앱 용어 | API 도메인명 | 리소스 경로 | 설명 |
|---------|-------------|------------|------|
| 그룹 (방) | **GroupRoom** | `/group-rooms` | 다이어리 방 단위 |
| 일기 (글) | **Diary** | `/group-rooms/:groupRoomId/diaries` | 개별 일기 글 |
| 구성원 | **Membership** | `/group-rooms/:groupRoomId/memberships` | 그룹 내 구성원 관리 |
| 사용자 | **User** | `/users` | 로그인한 사용자 본인 |

> **왜 Member가 아니라 Membership인가?**
> `User`는 "나 자신"의 프로필/설정, `Membership`은 "특정 그룹 안에서의 소속 관계"를 의미합니다.
> Member라고 하면 User와 혼동되므로, 관계(소속)를 나타내는 Membership으로 명명합니다.

### API 설계 기준

| 항목 | 기준 |
|------|------|
| 인증 | Bearer JWT (Access Token 1시간 + Refresh Token 30일) |
| 날짜 형식 | ISO 8601 (`2026-03-19T09:00:00Z`) |
| 페이지네이션 | Offset 기반 (`?limit=20&offset=0`) |
| 에러 응답 | `{ "error": { "code": "...", "message": "..." } }` |
| 이미지 업로드 | Multipart form-data |
| 삭제 전략 | 그룹: Soft Delete (7일 복구), 나머지: Hard Delete |
| 권한 체크 | 그룹 구성원 여부 → 리소스 소유자/방장 여부 순서 |
| HTTP 메서드 | GET(조회), POST(생성), PUT(전체수정), PATCH(부분수정), DELETE(삭제) |

---

## 📂 도메인 목록

| # | 도메인 | 브랜치 | 엔드포인트 수 | 설명 |
|---|--------|--------|:---:|------|
| 1 | Auth | `feature/auth-api` | 6 | 소셜 로그인, 토큰 갱신, 약관 동의, 로그아웃, 회원 탈퇴 |
| 2 | User | `feature/user-api` | 4 | 프로필 조회/수정, 알림 설정 |
| 3 | GroupRoom | `feature/group-room-api` | 6 | 그룹(방) 생성, 조회, 수정, 삭제, 복구 |
| 4 | Invite | `feature/invite-api` | 3 | 초대 코드 생성, 검증, 참여 |
| 5 | Membership | `feature/membership-api` | 4 | 구성원 목록, 내보내기, 역할 변경, 탈퇴 |
| 6 | Schedule | `feature/schedule-api` | 5 | 일정 CRUD, 참여자 관리 |
| 7 | Diary | `feature/diary-api` | 6 | 일기 CRUD, 이미지 첨부, 캘린더 조회 |
| 8 | Comment | `feature/comment-api` | 4 | 일정·일기 댓글 작성/삭제 |
| 9 | Todo | `feature/todo-api` | 4 | 할 일 CRUD, 완료 토글 |
| 10 | Notification | `feature/notification-api` | 4 | 알림 목록, 읽음 처리, 삭제 |
| 11 | Device | `feature/device-api` | 2 | 푸시 알림용 디바이스 토큰 등록/해제 |
| 12 | Upload | `feature/upload-api` | 1 | 이미지 업로드 (단일) |
| | | **합계** | **48** | |

---

## 📦 도메인별 상세

---

### 🔐 1. Auth (인증)

- **브랜치**: `feature/auth-api`
- **설명**: 소셜 로그인, 온보딩(약관 동의), JWT 토큰 관리, 로그아웃, 회원 탈퇴
- **인증 필요**: 로그인·약관조회 제외 전부

---

#### 1-1. 소셜 로그인

**POST** `/auth/login`

소셜 프로바이더 토큰으로 로그인. 최초 로그인 시 계정 자동 생성.
유저 식별은 **소셜 고유 ID + provider 조합**으로 수행 (email 기반 아님).

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| provider | string | ✅ | `kakao` · `naver` · `apple` |
| accessToken | string | ✅ | 소셜 프로바이더 액세스 토큰 |
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
| termsOfService | boolean | ✅ | 이용약관 동의 (필수) |
| privacyPolicy | boolean | ✅ | 개인정보처리방침 동의 (필수) |
| ageConfirmation | boolean | ✅ | 만 14세 이상 확인 (필수) |
| marketingConsent | boolean | ❌ | 마케팅 수신 동의 (선택) |
| pushConsent | boolean | ❌ | 푸시 알림 수신 동의 (선택) |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| user | User | 약관 동의 완료된 사용자 정보 |

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
| type | `terms-of-service` · `privacy-policy` · `marketing` |

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
| refreshToken | string | ✅ | 기존 리프레시 토큰 |

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

서버 측 리프레시 토큰 무효화 + 디바이스 토큰 해제.

**Response** `204 No Content`

---

#### 1-6. 회원 탈퇴

**DELETE** `/auth/account`

계정 영구 삭제. 소유 중인 그룹이 있으면 먼저 양도 필요.

**Response** `204 No Content`

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `OWNS_ACTIVE_GROUP` | 409 | 소유 중인 그룹이 있어 탈퇴 불가. 방장 양도 후 재시도. |

---

### 👤 2. User (사용자)

- **브랜치**: `feature/user-api`
- **설명**: 내 프로필 조회·수정, 알림 설정
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
| provider | string | `kakao` · `naver` · `apple` · `admin` |
| createdAt | string | 가입일 |

---

#### 2-2. 프로필 수정

**PUT** `/users/me`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| name | string | ❌ | 닉네임 (2~20자) |
| statusMessage | string? | ❌ | 상태 메시지 (빈 문자열 = 삭제, 최대 100자) |
| profileImageId | string? | ❌ | Upload API로 받은 이미지 ID (null = 기본 아바타로 초기화) |

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

### 🏠 3. GroupRoom (그룹)

- **브랜치**: `feature/group-room-api`
- **설명**: 다이어리 그룹(방) 생성, 목록 조회, 상세 조회, 수정, 삭제(Soft Delete 7일), 복구
- **인증 필요**: 전부
- **권한**: 수정/삭제/복구는 방장(owner)만

---

#### 3-1. 그룹 생성

**POST** `/group-rooms`

생성 시 초대 코드 자동 발급. 생성자가 방장(owner)이 됨.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| name | string | ✅ | 그룹명 (2~20자) |
| maxMembers | int | ✅ | 최대 인원 (2~99) |
| thumbnailImageId | string | ❌ | Upload API로 받은 이미지 ID |

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| group | Group | 생성된 그룹 |
| inviteCode | string | 자동 발급된 6자리 초대 코드 |
| inviteCodeExpiresAt | string | 초대 코드 만료 시각 (24시간 후) |

---

#### 3-2. 내 그룹 목록

**GET** `/group-rooms`

내가 속한 모든 그룹. 최근 활동순 정렬.

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| groups | GroupListItem[] | 그룹 배열 |

GroupListItem 객체:

| 필드 | 타입 | 설명 |
|------|------|------|
| id | string | 그룹 ID |
| name | string | 그룹명 |
| thumbnailImage | string? | 썸네일 이미지 URL |
| memberCount | int | 현재 구성원 수 |
| maxMembers | int | 최대 인원 |
| myRole | string | 내 역할 (`owner` · `member`) |
| lastActivityAt | string | 마지막 활동 시각 |
| isDeleteScheduled | boolean | 삭제 예약 여부 |

---

#### 3-3. 그룹 상세 조회

**GET** `/group-rooms/:groupRoomId`

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| group | Group | 그룹 정보 |
| memberships | MembershipSummary[] | 구성원 목록 (간략) |
| myRole | string | 내 역할 |
| inviteCode | string? | 현재 유효한 초대 코드 (방장에게만 노출) |

---

#### 3-4. 그룹 수정

**PUT** `/group-rooms/:groupRoomId`

방장만 가능.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| name | string | ❌ | 그룹명 (2~20자) |
| maxMembers | int | ❌ | 최대 인원 (현재 구성원 수 이상이어야 함) |
| thumbnailImageId | string? | ❌ | 새 썸네일 이미지 ID (null = 삭제) |

**Response** `200 OK` — 수정된 Group 객체

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `MAX_MEMBERS_BELOW_CURRENT` | 400 | 현재 구성원 수보다 적은 값으로 설정 시도 |

---

#### 3-5. 그룹 삭제 (Soft Delete)

**DELETE** `/group-rooms/:groupRoomId`

방장만 가능. 즉시 삭제가 아닌 7일 후 영구 삭제 예약.

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| deleteScheduledAt | string | 영구 삭제 예정 시각 (7일 후) |

---

#### 3-6. 그룹 복구

**POST** `/group-rooms/:groupRoomId/recover`

방장만 가능. 삭제 예약 기간(7일) 내에만 복구 가능.

**Response** `200 OK` — 복구된 Group 객체

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `GROUP_NOT_SCHEDULED_FOR_DELETION` | 400 | 삭제 예약되지 않은 그룹 |
| `GROUP_ALREADY_DELETED` | 410 | 이미 영구 삭제됨 |

---

### 🔗 4. Invite (초대)

- **브랜치**: `feature/invite-api`
- **설명**: 초대 코드 생성·검증·참여
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

코드 입력 시 그룹 미리보기. 참여 전 확인용.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| code | string | ✅ | 6자리 초대 코드 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRoomName | string | 그룹룸명 |
| thumbnailImage | string? | 썸네일 이미지 URL |
| memberCount | int | 현재 구성원 수 |
| maxMembers | int | 최대 인원 |
| expiresAt | string | 코드 만료 시각 |

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `INVITE_CODE_INVALID` | 404 | 존재하지 않는 코드 |
| `INVITE_CODE_EXPIRED` | 410 | 만료된 코드 |
| `GROUP_FULL` | 409 | 인원 초과 |
| `ALREADY_JOINED` | 409 | 이미 참여 중인 그룹 |

---

#### 4-3. 초대 코드로 참여

**POST** `/invites/join`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| code | string | ✅ | 6자리 초대 코드 |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| groupRoom | GroupRoom | 참여한 그룹룸 정보 |
| memberships | MembershipSummary[] | 전체 구성원 목록 |

> 참여 성공 시 기존 구성원에게 `member_joined` 푸시 알림 발송

---

### 👥 5. Membership (구성원 관리)

- **브랜치**: `feature/membership-api`
- **설명**: 그룹 내 구성원 목록 조회, 내보내기(추방), 역할 변경(방장 양도), 자발적 탈퇴
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
| role | string | `owner` · `member` |
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
| `USER_NOT_IN_GROUP` | 404 | 해당 그룹 구성원이 아님 |

---

#### 5-3. 역할 변경 (방장 양도)

**PUT** `/group-rooms/:groupRoomId/memberships/:userId/role`

현재 방장만 가능. 지정한 구성원에게 방장을 양도하면 기존 방장은 일반 구성원이 됨.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| role | string | ✅ | `owner` (양도 대상) |

**Response** `200 OK`

| 필드 | 타입 | 설명 |
|------|------|------|
| memberships | Membership[] | 변경된 전체 구성원 목록 |

---

#### 5-4. 그룹 탈퇴

**POST** `/group-rooms/:groupRoomId/leave`

자발적 탈퇴. 방장은 양도 후에만 탈퇴 가능.

**Response** `204 No Content`

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `OWNER_CANNOT_LEAVE` | 400 | 방장은 양도 후 탈퇴 가능 |

---

### 📅 6. Schedule (일정)

- **브랜치**: `feature/schedule-api`
- **설명**: 그룹 일정 CRUD, 참여자 지정, 월별 조회
- **인증 필요**: 전부
- **권한**: 수정/삭제는 작성자 또는 방장

---

#### 6-1. 일정 목록 (기간 조회)

**GET** `/group-rooms/:groupRoomId/schedules`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:---:|------|
| startDate | string | ✅ | 조회 시작일 (`2026-03-01`) |
| endDate | string | ✅ | 조회 종료일 (`2026-03-31`) |

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

> **색상 옵션**: `#FF6B6B`(빨강), `#A78BFA`(보라), `#60A5FA`(파랑), `#34D399`(초록), `#FB923C`(주황)

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
| title | string | ✅ | 제목 (1~50자) |
| color | string | ✅ | 색상 hex |
| startDate | string | ✅ | 시작일 (`YYYY-MM-DD`) |
| endDate | string | ✅ | 종료일 (`YYYY-MM-DD`, 시작일 이상) |
| startTime | string | ❌ | 시작 시간 (`HH:mm`, allDay=false일 때) |
| endTime | string | ❌ | 종료 시간 (`HH:mm`) |
| allDay | boolean | ✅ | 종일 여부 |
| participantIds | string[] | ❌ | 참여자 사용자 UUID 배열 |

**Response** `201 Created` — Schedule 객체

> 그룹 구성원에게 `schedule_created` 푸시 알림 발송

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `END_DATE_BEFORE_START` | 400 | 종료일이 시작일보다 이전 |
| `END_TIME_BEFORE_START` | 400 | 같은 날인데 종료 시간이 시작 시간보다 이전 |
| `INVALID_PARTICIPANT` | 400 | 참여자가 그룹 구성원이 아님 |

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

### 📓 7. Diary (일기)

- **브랜치**: `feature/diary-api`
- **설명**: 개별 일기 글 CRUD, 이미지 첨부, 날씨·기분 선택, 캘린더용 날짜 조회
- **인증 필요**: 전부
- **권한**: 수정/삭제는 작성자 또는 방장

---

#### 7-1. 일기 목록 (최신순)

**GET** `/group-rooms/:groupRoomId/diaries`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:---:|------|
| month | string | ❌ | 월 필터 (`2026-03`) |
| limit | int | ❌ | 페이지 크기 (기본 20, 최대 100) |
| offset | int | ❌ | 오프셋 (기본 0) |

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
| thumbnailImage | string? | 첫 번째 이미지 URL (썸네일용) |
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
| month | string | ✅ | 조회 월 (`2026-03`) |

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
| images | string[] | 이미지 URL 배열 |
| createdBy | UserSummary | 작성자 |
| createdAt | string | 작성일시 |
| updatedAt | string | 수정일시 |

---

#### 7-4. 일기 작성

**POST** `/group-rooms/:groupRoomId/diaries`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| title | string | ✅ | 제목 (1~20자) |
| content | string | ✅ | 본문 (1~300자) |
| date | string | ✅ | 날짜 (`YYYY-MM-DD`, 오늘 이전만 가능) |
| weather | int | ✅ | 0~3 |
| mood | int | ✅ | 0~3 |
| imageIds | string[] | ❌ | Upload API로 받은 이미지 ID 배열 (최대 5장) |

**Response** `201 Created` — Diary 객체

> 그룹 구성원에게 `diary_written` 푸시 알림 발송

**에러 케이스**

| 코드 | HTTP | 상황 |
|------|:---:|------|
| `FUTURE_DATE_NOT_ALLOWED` | 400 | 미래 날짜에 일기 작성 시도 |
| `INVALID_WEATHER_VALUE` | 400 | weather가 0~3 범위 밖 |
| `INVALID_MOOD_VALUE` | 400 | mood가 0~3 범위 밖 |
| `TOO_MANY_IMAGES` | 400 | 이미지 5장 초과 |

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

### 💬 8. Comment (댓글)

- **브랜치**: `feature/comment-api`
- **설명**: 일정·일기에 댓글 작성/삭제
- **인증 필요**: 전부
- **권한**: 삭제는 댓글 작성자 또는 방장

---

#### 8-1. 일정 댓글 작성

**POST** `/group-rooms/:groupRoomId/schedules/:scheduleId/comments`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| text | string | ✅ | 댓글 내용 (1~200자) |

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

### ✅ 9. Todo (할 일)

- **브랜치**: `feature/todo-api`
- **설명**: 그룹 공유 할 일 목록 — 생성, 완료 토글, 삭제
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
| text | string | ✅ | 내용 (1~100자) |

**Response** `201 Created` — Todo 객체

---

#### 9-3. 할 일 완료 토글

**PATCH** `/group-rooms/:groupRoomId/todos/:todoId`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| completed | boolean | ✅ | 완료 여부 |

**Response** `200 OK` — 수정된 Todo 객체

---

#### 9-4. 할 일 삭제

**DELETE** `/group-rooms/:groupRoomId/todos/:todoId`

**Response** `204 No Content`

---

### 🔔 10. Notification (알림)

- **브랜치**: `feature/notification-api`
- **설명**: 인앱 알림 목록 조회, 읽음 처리, 전체 읽음, 삭제
- **인증 필요**: 전부

---

#### 10-1. 알림 목록

**GET** `/notifications`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:---:|------|
| limit | int | ❌ | 페이지 크기 (기본 20, 최대 100) |
| offset | int | ❌ | 오프셋 (기본 0) |

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
| groupRoomId | string? | 관련 그룹룸 ID |
| groupRoomName | string? | 그룹룸명 |
| relatedId | string? | 관련 리소스 ID (일정/일기) |
| relatedType | string? | 관련 리소스 타입 (`schedule` · `diary` · `comment`) |
| isRead | boolean | 읽음 여부 |
| createdAt | string | 생성 시각 |

**알림 유형 (type) 전체 목록**

| type | 트리거 시점 | 수신 대상 | 푸시 발송 |
|------|-----------|----------|:---:|
| `schedule_created` | 일정 생성 | 그룹 전체 구성원 (작성자 제외) | ✅ |
| `schedule_updated` | 일정 수정 | 해당 일정 참여자 (수정자 제외) | ✅ |
| `diary_written` | 일기 작성 | 그룹 전체 구성원 (작성자 제외) | ✅ |
| `comment_on_schedule` | 일정에 댓글 | 일정 작성자 + 기존 댓글 작성자 (본인 제외) | ✅ |
| `comment_on_diary` | 일기에 댓글 | 일기 작성자 + 기존 댓글 작성자 (본인 제외) | ✅ |
| `member_joined` | 새 구성원 참여 | 기존 구성원 전체 | ✅ |
| `member_left` | 구성원 자발적 탈퇴 | 남은 구성원 전체 | ✅ |
| `member_removed` | 구성원 내보내기 | 내보내진 사용자 | ✅ |
| `ownership_transferred` | 방장 양도 | 그룹 전체 구성원 | ✅ |
| `group_delete_scheduled` | 그룹 삭제 예약 | 전체 구성원 | ✅ |
| `announcement` | 공지 | 전체 구성원 | ✅ |

---

#### 10-2. 알림 읽음 처리

**PATCH** `/notifications/:notificationId`

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| isRead | boolean | ✅ | `true` |

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

### 📱 11. Device (디바이스)

- **브랜치**: `feature/device-api`
- **설명**: 푸시 알림 발송을 위한 디바이스 토큰(FCM) 등록·해제
- **인증 필요**: 전부

---

#### 11-1. 디바이스 토큰 등록

**POST** `/devices`

앱 시작 시 또는 토큰 갱신 시 호출. 동일 토큰이면 upsert.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| token | string | ✅ | FCM 디바이스 토큰 |
| platform | string | ✅ | `ios` · `android` |

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

### 📤 12. Upload (업로드)

- **브랜치**: `feature/upload-api`
- **설명**: 이미지 업로드 (프로필, 그룹 썸네일, 일기 사진)
- **인증 필요**: 전부

---

#### 12-1. 이미지 업로드 (단일)

**POST** `/uploads/images`

Multipart form-data 방식.

**Request**

| 필드 | 타입 | 필수 | 설명 |
|------|------|:---:|------|
| file | binary | ✅ | 이미지 파일 (PNG/JPEG, 최대 5MB) |
| purpose | string | ✅ | `profile` · `group_thumbnail` · `diary` |

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

#### 12-2. 이미지 업로드 (다중)

**POST** `/uploads/images/batch`

일기 사진 여러 장 동시 업로드. Multipart form-data.

**Request** — 최대 5개 파일 (`files[]`)

**Response** `201 Created`

| 필드 | 타입 | 설명 |
|------|------|------|
| images | UploadResult[] | 업로드된 이미지 배열 (각각 id, url, width, height) |

---

## 🔒 공통 사항

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

## 🌿 브랜치 전략

### 브랜치 구조

```
main
 └── develop
      ├── feature/auth-api          ← 1순위
      ├── feature/user-api          ← 2순위
      ├── feature/upload-api        ← 3순위
      ├── feature/group-api         ← 4순위
      ├── feature/invite-api        ← 5순위
      ├── feature/membership-api    ← 6순위
      ├── feature/device-api        ← 7순위 (독립)
      ├── feature/schedule-api      ← 8순위
      ├── feature/diary-api         ← 9순위
      ├── feature/comment-api       ← 10순위
      ├── feature/todo-api          ← 11순위 (독립)
      └── feature/notification-api  ← 12순위
```

### 개발 순서 (의존성 기반)

| 순서 | 브랜치 | 의존 대상 | 이유 |
|:---:|--------|----------|------|
| 1 | `feature/auth-api` | — | 모든 API의 인증 기반, JWT 발급 |
| 2 | `feature/user-api` | Auth | 프로필·설정, 인증된 사용자 필요 |
| 3 | `feature/upload-api` | Auth | 이미지 업로드, 이후 도메인에서 참조 |
| 4 | `feature/group-api` | Auth, Upload | 그룹 CRUD, 썸네일 이미지 |
| 5 | `feature/invite-api` | Group | 초대 코드는 그룹에 종속 |
| 6 | `feature/membership-api` | Group | 구성원 관리는 그룹에 종속 |
| 7 | `feature/device-api` | Auth | FCM 토큰 등록, Auth만 의존 (병렬 가능) |
| 8 | `feature/schedule-api` | Group, Membership | 일정은 그룹+구성원 필요 |
| 9 | `feature/diary-api` | Group, Upload | 일기는 그룹+이미지 필요 |
| 10 | `feature/comment-api` | Schedule, Diary | 댓글은 일정/일기에 종속 |
| 11 | `feature/todo-api` | Group | 할 일은 그룹에만 종속 (병렬 가능) |
| 12 | `feature/notification-api` | 전체 | 모든 도메인 이벤트를 수신하여 알림+푸시 생성 |

> 7번(Device)과 11번(Todo)은 의존성이 적어 다른 도메인과 병렬 개발 가능

### 머지 규칙

- 각 feature 브랜치 → `develop`으로 PR 생성
- 코드 리뷰 필수 (최소 1인)
- CI 파이프라인 통과 후 머지 (lint + test + build)
- `develop` → `main`은 릴리스 시점에 머지
- 핫픽스: `main`에서 `hotfix/*` 브랜치 생성 → `main` + `develop` 양쪽 머지

---

## 📊 데이터베이스 ERD 요약

| 테이블 | 주요 관계 | 비고 |
|--------|-----------|------|
| users | — | 소셜 로그인 정보 포함 |
| user_terms | users 1:1 | 약관 동의 내역 |
| user_notification_settings | users 1:1 | 알림 설정 |
| user_privacy_settings | users 1:1 | 개인정보 설정 |
| groups | users N:1 (owner) | Soft Delete 지원 |
| group_memberships | users N:M groups | 조인 테이블, role·color 포함 |
| invite_codes | groups 1:N | 24시간 만료 |
| schedules | groups 1:N | 색상, 종일 여부 포함 |
| schedule_participants | users N:M schedules | 일정 참여자 |
| diaries | groups 1:N | 날씨, 기분 포함 |
| diary_images | diaries 1:N | 이미지 URL, 순서 |
| comments | schedules/diaries 다형성 1:N | parentType으로 구분 |
| todos | groups 1:N | 완료 여부, 완료자 |
| notifications | users 1:N | type, relatedId로 연결 |
| devices | users 1:N | FCM 토큰, 플랫폼 |
| uploaded_images | users 1:N | purpose별 분류 |

---

## 📋 엔드포인트 전체 목록 (49개)

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
| 11 | POST | `/group-rooms` | GroupRoom | 그룹 생성 |
| 12 | GET | `/group-rooms` | GroupRoom | 내 그룹 목록 |
| 13 | GET | `/group-rooms/:groupRoomId` | GroupRoom | 그룹 상세 |
| 14 | PUT | `/group-rooms/:groupRoomId` | GroupRoom | 그룹 수정 |
| 15 | DELETE | `/group-rooms/:groupRoomId` | GroupRoom | 그룹 삭제 |
| 16 | POST | `/group-rooms/:groupRoomId/recover` | GroupRoom | 그룹 복구 |
| 17 | POST | `/group-rooms/:groupRoomId/invites` | Invite | 초대 코드 생성 |
| 18 | POST | `/invites/validate` | Invite | 초대 코드 검증 |
| 19 | POST | `/invites/join` | Invite | 초대 코드로 참여 |
| 20 | GET | `/group-rooms/:groupRoomId/memberships` | Membership | 구성원 목록 |
| 21 | DELETE | `/group-rooms/:groupRoomId/memberships/:userId` | Membership | 구성원 내보내기 |
| 22 | PUT | `/group-rooms/:groupRoomId/memberships/:userId/role` | Membership | 역할 변경 |
| 23 | POST | `/group-rooms/:groupRoomId/leave` | Membership | 그룹 탈퇴 |
| 24 | GET | `/group-rooms/:groupRoomId/schedules` | Schedule | 일정 목록 |
| 25 | GET | `/group-rooms/:groupRoomId/schedules/:scheduleId` | Schedule | 일정 상세 |
| 26 | POST | `/group-rooms/:groupRoomId/schedules` | Schedule | 일정 생성 |
| 27 | PUT | `/group-rooms/:groupRoomId/schedules/:scheduleId` | Schedule | 일정 수정 |
| 28 | DELETE | `/group-rooms/:groupRoomId/schedules/:scheduleId` | Schedule | 일정 삭제 |
| 29 | GET | `/group-rooms/:groupRoomId/diaries` | Diary | 일기 목록 |
| 30 | GET | `/group-rooms/:groupRoomId/diaries/calendar` | Diary | 일기 캘린더 |
| 31 | GET | `/group-rooms/:groupRoomId/diaries/:diaryId` | Diary | 일기 상세 |
| 32 | POST | `/group-rooms/:groupRoomId/diaries` | Diary | 일기 작성 |
| 33 | PUT | `/group-rooms/:groupRoomId/diaries/:diaryId` | Diary | 일기 수정 |
| 34 | DELETE | `/group-rooms/:groupRoomId/diaries/:diaryId` | Diary | 일기 삭제 |
| 35 | POST | `/group-rooms/:groupRoomId/schedules/:scheduleId/comments` | Comment | 일정 댓글 작성 |
| 36 | DELETE | `/group-rooms/:groupRoomId/schedules/:scheduleId/comments/:commentId` | Comment | 일정 댓글 삭제 |
| 37 | POST | `/group-rooms/:groupRoomId/diaries/:diaryId/comments` | Comment | 일기 댓글 작성 |
| 38 | DELETE | `/group-rooms/:groupRoomId/diaries/:diaryId/comments/:commentId` | Comment | 일기 댓글 삭제 |
| 39 | GET | `/group-rooms/:groupRoomId/todos` | Todo | 할 일 목록 |
| 40 | POST | `/group-rooms/:groupRoomId/todos` | Todo | 할 일 생성 |
| 41 | PATCH | `/group-rooms/:groupRoomId/todos/:todoId` | Todo | 할 일 토글 |
| 42 | DELETE | `/group-rooms/:groupRoomId/todos/:todoId` | Todo | 할 일 삭제 |
| 43 | GET | `/notifications` | Notification | 알림 목록 |
| 44 | PATCH | `/notifications/:notificationId` | Notification | 알림 읽음 |
| 45 | POST | `/notifications/read-all` | Notification | 전체 읽음 |
| 46 | DELETE | `/notifications/:notificationId` | Notification | 알림 삭제 |
| 47 | POST | `/devices` | Device | 디바이스 토큰 등록 |
| 48 | DELETE | `/devices/:deviceId` | Device | 디바이스 토큰 해제 |
| 49 | POST | `/uploads/images` | Upload | 이미지 업로드 (단일) |

---

## 🔑 권한 매트릭스

| 행동 | 방장 (owner) | 구성원 (member) | 비구성원 |
|------|:---:|:---:|:---:|
| 그룹 조회 | ✅ | ✅ | ❌ |
| 그룹 수정/삭제/복구 | ✅ | ❌ | ❌ |
| 초대 코드 생성 | ✅ | ❌ | ❌ |
| 구성원 내보내기 | ✅ | ❌ | ❌ |
| 역할 변경 (방장 양도) | ✅ | ❌ | ❌ |
| 일정 생성 | ✅ | ✅ | ❌ |
| 일정 수정/삭제 (본인 것) | ✅ | ✅ | ❌ |
| 일정 수정/삭제 (타인 것) | ✅ | ❌ | ❌ |
| 일기 작성 | ✅ | ✅ | ❌ |
| 일기 수정/삭제 (본인 것) | ✅ | ✅ | ❌ |
| 일기 수정/삭제 (타인 것) | ✅ | ❌ | ❌ |
| 댓글 작성 | ✅ | ✅ | ❌ |
| 댓글 삭제 (본인 것) | ✅ | ✅ | ❌ |
| 댓글 삭제 (타인 것) | ✅ | ❌ | ❌ |
| 할 일 생성/토글 | ✅ | ✅ | ❌ |
| 할 일 삭제 (본인 것) | ✅ | ✅ | ❌ |
| 할 일 삭제 (타인 것) | ✅ | ❌ | ❌ |
