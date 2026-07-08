package digdaserver.domain.diary.presentation.dto.res

import java.time.LocalDate

/**
 * 일기 캘린더 응답.
 *
 * - [dates] : 일기가 존재하는 날짜 목록(하위호환 — 기존 dot marker 용).
 * - [entries] : 날짜별 대표 일기 요약(사진 그리드용 썸네일/기분/편수).
 * - [stats] : 이번 달 통계 스트립(편수·연속 기록·최다 기분).
 * - [myDates] : 조회자 본인이 일기를 쓴 날짜 목록 — "인당 하루 1편" 규칙의 작성 가능 여부 판정용.
 */
data class DiaryCalendarResponse(
    val dates: List<LocalDate>,
    val entries: List<DiaryCalendarEntry> = emptyList(),
    val stats: DiaryCalendarStats = DiaryCalendarStats(),
    val myDates: List<LocalDate> = emptyList()
)

/**
 * 캘린더 한 칸(날짜)에 대응하는 대표 일기. 하루 여러 편이면 [count] 로 표시하고 대표 1건을 노출.
 *
 * 차단/신고로 숨겨진 일기여도 [count] 는 유지된다 — "하루 1편" 슬롯이 빈 날로 보이지 않게 하기 위함.
 * 이때 [hidden] 만 true 가 되고 [thumbnailUrl] 은 비워진다.
 */
data class DiaryCalendarEntry(
    val date: LocalDate,
    val diaryId: Long,
    val thumbnailUrl: String?,
    val mood: Int,
    val count: Int,
    val hidden: Boolean = false,
    val hiddenReason: String? = null
) {
    fun asHidden(reason: String): DiaryCalendarEntry = copy(
        thumbnailUrl = null,
        hidden = true,
        hiddenReason = reason
    )
}

/**
 * 통계 스트립 값.
 * - [count] : 해당 월 작성된 일기 편수.
 * - [streak] : 오늘(KST) 기준 연속으로 일기를 쓴 일수. 오늘 미작성이면 어제까지의 연속 일수.
 * - [topMood] : 해당 월 가장 많이 기록된 기분(0~4). 일기가 없으면 null.
 */
data class DiaryCalendarStats(
    val count: Int = 0,
    val streak: Int = 0,
    val topMood: Int? = null
)
