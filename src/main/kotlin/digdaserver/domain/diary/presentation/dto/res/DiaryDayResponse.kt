package digdaserver.domain.diary.presentation.dto.res

import java.time.LocalDate

/**
 * 특정 날짜의 그림일기 목록 응답 — 캘린더에서 날짜를 탭했을 때의 목록 화면용.
 *
 * - [diaries] : 그날 작성된 모든 일기(먼저 쓴 순). 차단/신고 숨김은 본문만 비워 자리 유지.
 * - [representativeDiaryId] : 그날의 대표 썸네일 일기 id. 지정된 대표가 없으면
 *   "가장 먼저 작성된 일기"로 폴백해 항상 하나를 가리킨다(일기가 없으면 null).
 * - [myDiaryId] : 조회자 본인이 그날 쓴 일기 id(없으면 null) — 작성 버튼 노출 판정용.
 */
data class DiaryDayResponse(
    val date: LocalDate,
    val diaries: List<DiarySummaryResponse>,
    val representativeDiaryId: Long?,
    val myDiaryId: Long?
)
