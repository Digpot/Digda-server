package digdaserver.domain.highlight.presentation.dto.res

import digdaserver.domain.highlight.application.service.HighlightDiary
import digdaserver.domain.highlight.application.service.HighlightMemberStat
import java.time.LocalDateTime

/** 이전 달 목록의 한 칸. */
data class HighlightListItem(
    /** "yyyy-MM" */
    val month: String,
    val year: Int,
    val monthValue: Int,
    val title: String,
    val totalDiaryCount: Int,
    val coverImageUrl: String?,
    val generatedAt: LocalDateTime
)

data class HighlightListResponse(
    val highlights: List<HighlightListItem>
)

data class HighlightDetailResponse(
    val month: String,
    val year: Int,
    val monthValue: Int,
    /** 예: "2026년 10월, 우리의 기록" */
    val title: String,
    val groupRoomName: String,
    val generatedAt: LocalDateTime,
    val totalDiaryCount: Int,
    val activeDays: Int,
    val daysInMonth: Int,
    val members: List<HighlightMemberStat>,
    val topWriters: List<HighlightMemberStat>,
    val coverImageUrl: String?,
    val photos: List<String>,
    val featuredDiaries: List<HighlightDiary>,
    val topLocation: String?,
    /** 1=월 … 7=일 */
    val busiestWeekday: Int?,
    val summary: String,
    val closingMessage: String
)
