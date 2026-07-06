package digdaserver.domain.diary.presentation.dto.res

data class DiaryListResponse(
    val diaries: List<DiarySummaryResponse>,
    val total: Long
)
