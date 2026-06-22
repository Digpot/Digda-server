# 개발 워크플로우 가이드

DigDa 서버 프로젝트의 도메인 API 개발 프로세스입니다.

---

## 전체 흐름

```
1. 이슈 생성 → 2. 브랜치 생성 (이슈 연결) → 3. 구현 → 4. 빌드 확인 → 5. 커밋 → 6. 푸시 → 7. PR 생성 → 8. 머지
```

---

## 1. 이슈 생성

GitHub Issues에서 아래 양식으로 생성합니다.

**제목**: `[✨ Feature] {도메인명} API 구현 ({기능 요약})`

**라벨**: `✨ Feature` + `🟠 P1: High` + 도메인 라벨 (예: `📅 Schedule`)

**본문 양식**:

```markdown
### 🔔 개요

{도메인} 도메인 API {N}개를 구현합니다. {기능 설명}

### ✅ 해야 할 일

- [ ] {API 1} — `{메서드} {엔드포인트}`
- [ ] {API 2} — `{메서드} {엔드포인트}`
- [ ] ...
- [ ] Swagger 문서 연동 확인
- [ ] 에러 케이스 처리 및 테스트

## API 상세

| # | 메서드 | 엔드포인트 | 설명 |
|---|--------|-----------|------|
| 1 | GET | `/...` | ... |

### 🙏🏻 요구사항

{각 API별 상세 요구사항}

### 🙋🏻 덧붙일 말
브랜치: `feature/{도메인}-api`
선행 의존: {선행 이슈} (완료)
```

---

## 2. 브랜치 생성 (이슈 연결) — 필수: `gh issue develop` 사용

**반드시 `gh issue develop` 명령어로 브랜치를 생성합니다.** `git checkout -b`로 수동 생성하면 이슈와 브랜치가 연결되지 않습니다.

```bash
# 반드시 이 명령어로 브랜치 생성 (이슈-브랜치 자동 연결)
gh issue develop {이슈번호} --name feature/{도메인}-api --base dev

# 생성된 브랜치로 전환
git fetch origin
git checkout feature/{도메인}-api
git merge origin/dev --no-edit
```

> **주의**: `git checkout -b` 사용 금지. `gh issue develop`으로 생성해야 PR 머지 시 이슈가 자동으로 닫힙니다.

---

## 3. 구현

### 패키지 구조

```
src/main/kotlin/digdaserver/domain/{도메인}/
├── application/
│   └── service/
│       ├── {Domain}Service.kt              # 인터페이스
│       └── impl/
│           └── {Domain}ServiceImpl.kt      # 구현체
├── domain/
│   ├── entity/
│   │   └── {Entity}.kt                     # (이미 존재)
│   └── repository/
│       └── {Entity}Repository.kt           # (이미 존재)
└── presentation/
    ├── controller/
    │   └── {Domain}Controller.kt           # REST 컨트롤러
    └── dto/
        ├── req/
        │   └── {Action}Request.kt          # 요청 DTO
        └── res/
            └── {Action}Response.kt         # 응답 DTO
```

### 구현 규칙

- Service 인터페이스 / Impl 분리 구조
- `@Transactional(readOnly = true)` 클래스 레벨, 쓰기 메서드만 `@Transactional`
- 권한 체크 순서: 그룹방 존재 → 삭제 여부 → 구성원 여부 → 방장/작성자 여부
- Swagger 어노테이션 (`@Tag`, `@Operation`)
- `@AuthenticationPrincipal userId: String` → `UUID.fromString(userId)`
- 에러 코드는 `ErrorCode.kt`에 정의된 것 활용

---

## 4. 빌드 확인

```bash
./gradlew compileKotlin
```

> BUILD SUCCESSFUL 확인 후 다음 단계로 진행합니다.

---

## 5. 커밋

```bash
git add {변경 파일들}
git commit -m "[Feature] {도메인} API 구현 ({기능 요약})"
```

**커밋 메시지 컨벤션**:

| 태그 | 용도 |
|------|------|
| `[Feature]` | 새로운 기능 추가 |
| `[Fix]` | 버그 수정 |
| `[Refactor]` | 코드 리팩토링 |
| `[Chore]` | 빌드, 설정, 의존성 등 |
| `[Docs]` | 문서 추가/수정 |

---

## 6. 푸시

```bash
git push origin feature/{도메인}-api
```

---

## 7. PR 생성

**제목**: `[Feature] {도메인} API 구현 ({기능 요약})`

**라벨**: 이슈와 동일

**본문 양식**:

```markdown
## #️⃣연관된 이슈

> #{이슈번호}

## 📝작업 내용

> {도메인} 도메인 API {N}개를 구현했습니다.

### {도메인} API ({N}개)
| # | 메서드 | 엔드포인트 | 설명 |
|---|--------|-----------|------|
| 1 | GET | `/...` | ... |

### 주요 구현 사항
- {구현 사항 1}
- {구현 사항 2}
```

---

## 8. 머지 — PR 생성 후 반드시 머지까지 완료

PR 생성에서 끝이 아닙니다. **반드시 머지까지 수행합니다.**

```bash
# PR 머지 (PR 생성 직후 실행)
gh pr merge {PR번호} --merge
```

> 머지 후 다음 도메인 작업 시 dev를 pull 받고 새 이슈부터 다시 시작합니다.

---

## 진행 현황

| # | 도메인 | 브랜치 | 이슈 | PR | 상태 |
|---|--------|--------|:---:|:---:|:---:|
| 1 | Auth | `feature/auth-api` | #7 | #8 | ✅ 완료 |
| 2 | User | `feature/user-api` | #9 | #10 | ✅ 완료 |
| 3 | GroupRoom | `feature/group-room-api` | #11 | #12 | ✅ 완료 |
| 4 | Invite | `feature/invite-api` | #13 | #14 | ✅ 완료 |
| 5 | Membership | `feature/membership-api` | #15 | #16 | ✅ 완료 |
| 6 | Schedule | `feature/schedule-api` | #17 | #18 | ✅ 완료 |
| 7 | Diary | `feature/diary-api` | #19 | #20 | ✅ 완료 |
| 8 | Comment | `feature/comment-api` | #21 | #22 | ✅ 완료 |
| 9 | Todo | `feature/todo-api` | #23 | #25 | ✅ 완료 |
| 10 | Notification | `feature/notification-api` | - | - | 🔲 대기 |
| 11 | Upload | `feature/upload-api` | - | - | 🔲 대기 |
| 12 | Device | `feature/device-api` | - | - | 🔲 대기 |
