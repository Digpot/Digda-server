# DigDa ERD (Entity Relationship Diagram)

> 총 **14개 MySQL 테이블** + **2개 Redis 엔티티**
> `users`를 중심으로 그룹 → 일정/일기/할일 구조

---

## Mermaid ERD

```mermaid
erDiagram
    users["users (사용자)"] {
        binary_16 user_id PK "사용자 UUID"
        varchar social_id "소셜 고유 ID (nullable — 관리자는 null)"
        enum social_provider "제공자 (KAKAO/NAVER/APPLE/ADMIN)"
        varchar email "이메일 (nullable)"
        varchar name "닉네임 (2~20자)"
        varchar profile_image "프로필 이미지 URL"
        varchar status_message "상태 메시지 (최대 100자)"
        enum role "역할 (USER/ADMIN)"
        datetime createdAt "가입일"
        datetime updatedAt "수정일"
    }

    user_terms["user_terms (약관 동의)"] {
        bigint user_terms_id PK "약관 동의 ID"
        binary_16 user_id FK "사용자 UUID"
        boolean terms_of_service "이용약관 동의 (필수)"
        boolean privacy_policy "개인정보처리방침 (필수)"
        boolean age_confirmation "14세 이상 확인 (필수)"
        boolean marketing_consent "마케팅 수신 동의 (선택)"
        boolean push_consent "푸시 알림 동의 (선택)"
        datetime agreed_at "동의 일시"
    }

    user_notification_settings["user_notification_settings (알림 설정)"] {
        bigint id PK "알림 설정 ID"
        binary_16 user_id FK "사용자 UUID"
        boolean push_enabled "푸시 알림 전체 ON/OFF"
        boolean schedule_notification "일정 알림"
        boolean diary_notification "일기 알림"
        boolean comment_notification "댓글 알림"
        boolean marketing_consent "마케팅 수신 동의"
    }

    user_privacy_settings["user_privacy_settings (개인정보 설정)"] {
        bigint id PK "개인정보 설정 ID"
        binary_16 user_id FK "사용자 UUID"
        boolean profile_public "프로필 공개 여부"
        boolean activity_visible "활동 표시 여부"
    }

    groups["groups (그룹/다이어리 방)"] {
        bigint group_id PK "그룹 ID"
        varchar name "그룹명 (2~20자)"
        varchar thumbnail_image "썸네일 이미지 URL"
        int max_members "최대 인원"
        binary_16 owner_id FK "방장 사용자 UUID"
        datetime last_activity_at "마지막 활동 일시"
        datetime delete_scheduled_at "삭제 예약 일시"
        datetime deleted_at "삭제 일시 (Soft Delete)"
        datetime createdAt "생성일"
        datetime updatedAt "수정일"
    }

    memberships["memberships (그룹 소속)"] {
        bigint membership_id PK "소속 ID"
        binary_16 user_id FK "사용자 UUID"
        bigint group_id FK "그룹 ID"
        enum role "그룹 역할 (OWNER/MEMBER)"
        varchar color "사용자 색상 (#FFFFFF)"
        datetime joined_at "가입 일시"
    }

    invite_codes["invite_codes (초대 코드)"] {
        bigint invite_code_id PK "초대 코드 ID"
        bigint group_id FK "그룹 ID"
        varchar code UK "초대 코드 (6자리)"
        datetime expires_at "만료 일시"
        datetime created_at "생성 일시"
    }

    schedules["schedules (일정)"] {
        bigint schedule_id PK "일정 ID"
        bigint group_id FK "그룹 ID"
        varchar title "일정 제목 (최대 50자)"
        varchar color "일정 색상 (#FFFFFF)"
        date start_date "시작일"
        date end_date "종료일"
        time start_time "시작 시간 (종일이면 null)"
        time end_time "종료 시간 (종일이면 null)"
        boolean all_day "종일 여부"
        binary_16 created_by FK "작성자 UUID"
        datetime createdAt "생성일"
        datetime updatedAt "수정일"
    }

    schedule_participants["schedule_participants (일정 참여자)"] {
        bigint id PK "참여자 ID"
        bigint schedule_id FK "일정 ID"
        binary_16 user_id FK "사용자 UUID"
    }

    diaries["diaries (일기)"] {
        bigint diary_id PK "일기 ID"
        bigint group_id FK "그룹 ID"
        varchar title "일기 제목 (최대 20자)"
        varchar content "일기 내용 (최대 300자)"
        date date "일기 날짜"
        int weather "날씨 (0~3)"
        int mood "기분 (0~3)"
        binary_16 created_by FK "작성자 UUID"
        datetime createdAt "생성일"
        datetime updatedAt "수정일"
    }

    diary_images["diary_images (일기 이미지)"] {
        bigint diary_image_id PK "이미지 ID"
        bigint diary_id FK "일기 ID"
        varchar image_url "이미지 URL"
        int sort_order "정렬 순서"
    }

    comments["comments (댓글)"] {
        bigint comment_id PK "댓글 ID"
        enum target_type "대상 유형 (SCHEDULE/DIARY)"
        bigint target_id "대상 ID"
        varchar text "댓글 내용 (최대 200자)"
        binary_16 created_by FK "작성자 UUID"
        datetime created_at "작성 일시"
    }

    todos["todos (할 일)"] {
        bigint todo_id PK "할 일 ID"
        bigint group_id FK "그룹 ID"
        varchar text "할 일 내용 (최대 100자)"
        boolean completed "완료 여부"
        datetime completed_at "완료 일시"
        binary_16 completed_by FK "완료한 사용자 UUID"
        binary_16 created_by FK "작성자 UUID"
        datetime created_at "작성 일시"
    }

    notifications["notifications (알림)"] {
        bigint notification_id PK "알림 ID"
        binary_16 user_id FK "수신자 UUID"
        enum type "알림 유형"
        varchar title "알림 제목"
        varchar message "알림 메시지"
        bigint group_id "관련 그룹 ID"
        varchar group_name "관련 그룹명"
        bigint related_id "관련 리소스 ID"
        varchar related_type "관련 리소스 유형"
        boolean is_read "읽음 여부"
        datetime created_at "생성 일시"
    }

    devices["devices (디바이스)"] {
        bigint device_id PK "디바이스 ID"
        binary_16 user_id FK "사용자 UUID"
        varchar token UK "FCM 토큰 (고유)"
        enum platform "플랫폼 (IOS/ANDROID)"
        datetime createdAt "등록일"
        datetime updatedAt "수정일"
    }

    uploaded_images["uploaded_images (업로드 이미지)"] {
        bigint uploaded_image_id PK "이미지 ID"
        binary_16 user_id FK "업로더 UUID"
        varchar url "이미지 URL"
        int width "가로 크기 (px)"
        int height "세로 크기 (px)"
        enum purpose "용도 (PROFILE/GROUP_THUMBNAIL/DIARY)"
        datetime created_at "업로드 일시"
    }

    %% ── 관계 (Relationships) ──

    users ||--o| user_terms : "약관 동의"
    users ||--o| user_notification_settings : "알림 설정"
    users ||--o| user_privacy_settings : "개인정보 설정"

    users ||--o{ memberships : "그룹 소속"
    users ||--o{ devices : "디바이스 등록"
    users ||--o{ notifications : "알림 수신"
    users ||--o{ uploaded_images : "이미지 업로드"
    users ||--o{ comments : "댓글 작성"

    groups ||--o{ memberships : "구성원"
    groups ||--o{ invite_codes : "초대 코드"
    groups ||--o{ schedules : "일정"
    groups ||--o{ diaries : "일기"
    groups ||--o{ todos : "할 일"
    groups }o--|| users : "방장"

    schedules }o--|| users : "작성자"
    schedules ||--o{ schedule_participants : "참여자"
    schedule_participants }o--|| users : "참여 사용자"

    diaries }o--|| users : "작성자"
    diaries ||--o{ diary_images : "첨부 이미지"

    todos }o--|| users : "작성자"
    todos }o--o| users : "완료자"
```

---

## Redis Entities (비관계형)

> JWT 토큰과 소셜 토큰을 Redis에 저장하여 빠른 조회/만료 처리

```mermaid
erDiagram
    JsonWebToken_Redis["JsonWebToken (JWT 토큰 — TTL 14일)"] {
        string refreshToken PK "리프레시 토큰 (키)"
        string providerId "사용자 UUID (users.user_id 참조)"
        string email "사용자 이메일 (nullable)"
        enum role "역할 (USER/ADMIN)"
    }

    SocialToken_Redis["SocialToken (소셜 토큰)"] {
        string id PK "키 (userId:provider 형식)"
        string userId "사용자 UUID (users.user_id 참조)"
        enum provider "소셜 제공자 (KAKAO/NAVER/APPLE)"
        string accessToken "소셜 액세스 토큰"
        string refreshToken "소셜 리프레시 토큰"
        datetime expiresIn "만료 일시"
    }
```

---

## 테이블 요약

| # | 테이블 | 설명 | 주요 관계 |
|---|--------|------|-----------|
| 1 | `users` | 사용자 (중심 엔티티, PK=UUID, UK=social_id+social_provider) | — |
| 2 | `user_terms` | 약관 동의 | users 1:1 |
| 3 | `user_notification_settings` | 알림 설정 | users 1:1 |
| 4 | `user_privacy_settings` | 개인정보 설정 | users 1:1 |
| 5 | `groups` | 그룹 (다이어리 방) | users N:1 (방장) |
| 6 | `memberships` | 그룹 소속 관계 | users N:1, groups N:1 |
| 7 | `invite_codes` | 초대 코드 (6자리, 만료) | groups N:1 |
| 8 | `schedules` | 일정 | groups N:1, users N:1 |
| 9 | `schedule_participants` | 일정 참여자 | schedules N:1, users N:1 |
| 10 | `diaries` | 일기 | groups N:1, users N:1 |
| 11 | `diary_images` | 일기 첨부 이미지 (최대 5장) | diaries N:1 |
| 12 | `comments` | 댓글 (일정/일기 공용) | users N:1 |
| 13 | `todos` | 할 일 | groups N:1, users N:1 |
| 14 | `notifications` | 알림 | users N:1 |
| 15 | `devices` | 디바이스 (FCM 푸시용) | users N:1 |
| 16 | `uploaded_images` | 업로드 이미지 (S3) | users N:1 |

### Enum 목록

| Enum | 값 | 사용처 |
|------|----|--------|
| `SocialProvider` | KAKAO, NAVER, APPLE, ADMIN | users.social_provider |
| `Role` | USER, ADMIN | users.role |
| `GroupRole` | OWNER, MEMBER | memberships.role |
| `Platform` | IOS, ANDROID | devices.platform |
| `CommentTargetType` | SCHEDULE, DIARY | comments.target_type |
| `ImagePurpose` | PROFILE, GROUP_THUMBNAIL, DIARY | uploaded_images.purpose |
| `NotificationType` | SCHEDULE_CREATED, SCHEDULE_UPDATED, DIARY_WRITTEN, COMMENT_ON_SCHEDULE, COMMENT_ON_DIARY, MEMBER_JOINED, MEMBER_REMOVED, GROUP_DELETE_SCHEDULED | notifications.type |
