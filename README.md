<div align="center">

# 🍙 Digda-server

**디그팟(DigPot · 디지털 그룹 포켓)** 백엔드 API 서버

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io)
[![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)](https://redis.io)
[![Deploy](https://img.shields.io/badge/Deploy-Docker%20%E2%86%92%20NCP-2496ED?logo=docker&logoColor=white)](#-배포-아키텍처)

운영사 **태리팟(Taeripot)** · 앱 [Digda-app](https://github.com/DateDiary/Digda-app) · 관리자 [digda-admin](https://github.com/DateDiary/digda-admin)

</div>

> 본 문서는 **내부 개발자·운영 담당자용** 안내입니다. 서버 구조와 데이터 흐름을 빠르게 파악하는 데 초점을 둡니다.

---

## 📖 개요

모바일 앱(디그팟)과 관리자 대시보드(digda-admin)에 REST API 를 제공하는 단일 백엔드입니다.
소셜 로그인 기반 인증, 그룹방 단위의 일기·일정·투두·캐릭터 도메인, FCM 푸시 알림,
이미지 업로드(S3), 정기 리마인더 배치를 담당합니다.

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|---|---|
| 언어·프레임워크 | Kotlin · Spring Boot 3 (Web, WebFlux) |
| 데이터 | Spring Data JPA · MySQL 8 · Redis(Refresh 토큰·캐시) |
| 인증·보안 | Spring Security · OAuth2 Client · JWT(jjwt) |
| 외부 연동 | OpenFeign(소셜 검증) · Firebase Admin(FCM) · AWS S3 |
| 배치·스케줄 | Spring Batch · `@Scheduled`(리마인더·정리) |
| 문서화 | springdoc-openapi (Swagger UI) |
| 빌드·품질 | Gradle(Kotlin DSL) · ktlint · spotless |
| 배포 | Docker · GitHub Actions → NCP |

---

## 🏗️ 시스템 구성도

```mermaid
flowchart LR
    subgraph CLIENT["클라이언트"]
        APP["📱 Digda-app<br/>Flutter"]
        ADMIN["🖥️ digda-admin<br/>Vue 3"]
    end

    subgraph SERVER["🍙 Digda-server · Spring Boot"]
        direction TB
        SEC["Security · JWT 필터"]
        CTRL["Controller (REST)"]
        SVC["Service (도메인 로직)"]
        REPO["Repository (JPA)"]
        SCHED["Scheduler · Batch"]
        SEC --> CTRL --> SVC --> REPO
        SCHED --> SVC
    end

    subgraph DATA["데이터"]
        DB[("MySQL")]
        REDIS[("Redis")]
    end
    subgraph EXT["외부 서비스"]
        OAUTH["카카오·네이버·Apple"]
        FCM[("Firebase FCM")]
        S3[("AWS S3")]
    end

    APP & ADMIN -->|HTTPS REST| SEC
    REPO --> DB
    SVC -->|토큰·캐시| REDIS
    SVC -->|소셜 검증| OAUTH
    SVC -->|이미지| S3
    SVC -->|푸시| FCM
    FCM -.알림.-> APP

    classDef client fill:#FFE2E2,stroke:#FF6B6B,stroke-width:1.5px,color:#7A1F1F;
    classDef server fill:#E6F4EA,stroke:#34A853,stroke-width:1.5px,color:#114B22;
    classDef data fill:#E8F0FE,stroke:#4285F4,stroke-width:1.5px,color:#0B3D91;
    classDef ext fill:#FFF4E5,stroke:#F9A825,stroke-width:1.5px,color:#7A4F00;
    class APP,ADMIN client; class SEC,CTRL,SVC,REPO,SCHED server;
    class DB,REDIS data; class OAUTH,FCM,S3 ext;
```

## 🔐 인증 흐름 (소셜 로그인 → JWT)

```mermaid
sequenceDiagram
    autonumber
    participant App as 📱 App
    participant API as 🍙 Server
    participant OAuth as 소셜 제공자
    participant Redis
    participant DB as MySQL
    App->>OAuth: 소셜 로그인(SDK)
    OAuth-->>App: 소셜 액세스 토큰
    App->>API: POST /auth (소셜 토큰)
    API->>OAuth: 토큰 검증 (Feign)
    OAuth-->>API: 사용자 정보
    API->>DB: 회원 조회/가입 (+ 알림설정 생성)
    API->>Redis: Refresh 토큰 저장
    API-->>App: Access · Refresh JWT
    Note over App,API: 이후 요청은 Bearer Access 로 JWT 필터 검증
```

## 🔔 알림(FCM) 발송 흐름

```mermaid
flowchart TB
    EVT["이벤트 발생<br/>일기·댓글·일정 리마인더 등"] --> NS["NotificationService"]
    NS --> SAVE[("알림 DB 저장<br/>인앱 알림 센터")]
    NS --> DISP["PushDispatcher"]
    DISP --> CHK{"수신 대상?<br/>설정 ON · 토큰 보유"}
    CHK -->|예| FCMS["FcmService 멀티캐스트"] --> FCM[("Firebase FCM")]
    FCMS --> CLEAN["무효 토큰 정리"]
    CHK -->|아니오| SKIP["스킵"]

    classDef a fill:#E6F4EA,stroke:#34A853,color:#114B22;
    classDef b fill:#E8F0FE,stroke:#4285F4,color:#0B3D91;
    classDef c fill:#FFF4E5,stroke:#F9A825,color:#7A4F00;
    class EVT,NS,DISP,FCMS,CLEAN a; class SAVE b; class FCM c;
```

---

## 🧩 도메인 모듈

`src/main/kotlin/digdaserver` = 도메인 패키지 + `global`(공통). 각 도메인은
`presentation(controller·dto) · application(service) · domain(entity·repository)` 레이어로 나뉩니다.

| 도메인 | 책임 |
|---|---|
| `oauth2` | 소셜 로그인, JWT 발급·재발급, 계정 |
| `user` | 프로필, 알림/개인정보 설정 |
| `group_room` | 그룹방 CRUD·홈 집계, 삭제 예약·복구, 방장 양도 |
| `membership` · `invite` | 구성원 관리(강퇴/탈퇴) · 초대 코드 |
| `diary` · `schedule` · `comment` | 그림일기(캘린더 집계) · 일정(리마인더 스케줄러) · 댓글 |
| `todo` · `character` | 그룹 투두 · 모찌(경험치·진화·퀴즈·상점) |
| `notification` · `device` | 알림 생성·푸시 디스패치 · FCM 토큰 |
| `upload` · `announcement` · `log` | S3 업로드 · 공지 · 활동 로그 |
| `global` | `config`(보안/CORS/Swagger) · `infra`(fcm/s3/feign) · `jwt` · `common` |

---

## 🔌 API & 문서

| 그룹 | 대표 엔드포인트 |
|---|---|
| 인증 | `POST /auth` · `POST /auth/reissue` · `POST /logout` |
| 그룹방 | `GET /group-rooms` · `GET /group-rooms/{id}` · `GET /group-rooms/{id}/home` |
| 일기·일정 | `GET /diaries/calendar` · `POST /diaries` · `GET /schedules` · `POST /schedules` |
| 캐릭터 | `GET /characters` · `GET /character-quizzes/random` |
| 알림·기기 | `GET /notifications` · `POST /devices` |
| 관리자 | `/admin/**` |

> 전체 스펙은 **Swagger UI** (`/swagger-ui/index.html`).

---

## ⚙️ 배포 아키텍처

`dev` 머지 시 GitHub Actions 가 CI(ktlint·build) → Docker 이미지 빌드/푸시 → **NCP 인스턴스** 배포를 수행합니다.
운영 DB 는 `ddl-auto=none` — 스키마 변경은 `SchemaAutoMigration` 에 ALTER 정의를 추가해 반영합니다.

```mermaid
flowchart LR
    PR["PR → dev 머지"] --> CI["GitHub Actions<br/>ktlint · build · test"]
    CI --> IMG["Docker 이미지<br/>빌드 · 푸시"]
    IMG --> NCP["NCP 인스턴스<br/>docker compose up"]
    NCP --> RUN[("app + redis 기동")]
    classDef s fill:#E6F4EA,stroke:#34A853,color:#114B22;
    classDef d fill:#E8F0FE,stroke:#4285F4,color:#0B3D91;
    class PR,CI,IMG,NCP s; class RUN d;
```

---

## 🤝 협업 / 컨벤션

- 통합 브랜치 **`dev`** · PR base 는 `dev` (`main` 직접 승격 금지)
- 이슈/PR 템플릿: `.github/` · 라벨(Type·Priority·Status·Domain)·기본 담당자(`@chltmdgh522`)
- **Kotlin 수정 후 push 전 `./gradlew ktlintFormat` 필수** (CI ktlint 실패 방지)
- 커밋(AngularJS): `feat` · `fix` · `docs` · `style` · `refactor` · `test` · `chore`

<div align="center"><sub>© 2026 태리팟 · 디그팟 — Digital Group Pocket</sub></div>
