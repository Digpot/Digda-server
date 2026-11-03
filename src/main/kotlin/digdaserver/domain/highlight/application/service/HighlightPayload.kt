package digdaserver.domain.highlight.application.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** DB(`monthly_highlight.payload`)에 JSON 으로 저장되는 하이라이트 콘텐츠. 항목 추가는 하위 호환(ignoreUnknown). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HighlightPayload(
    val totalDiaryCount: Int,
    /** 일기가 하나라도 있었던 날 수. */
    val activeDays: Int,
    val daysInMonth: Int,
    val members: List<HighlightMemberStat>,
    /** 최다 작성자(동률이면 모두). 비어 있지 않다. */
    val topWriters: List<HighlightMemberStat>,
    val coverImageUrl: String?,
    val photos: List<String>,
    val featuredDiaries: List<HighlightDiary>,
    val topLocation: String?,
    /** 일기가 가장 많았던 요일, 1=월 … 7=일. 동률이면 이른 요일. */
    val busiestWeekday: Int?,
    val summary: String,
    val closingMessage: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HighlightMemberStat(
    val userId: String,
    val name: String,
    val profileImage: String?,
    val diaryCount: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HighlightDiary(
    val diaryId: Long,
    val title: String,
    val excerpt: String,
    /** yyyy-MM-dd */
    val date: String,
    val authorName: String,
    val imageUrl: String?,
    val likeCount: Int,
    val reactionCount: Int,
    val commentCount: Int
)
