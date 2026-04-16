package digdaserver.domain.diary.presentation.dto.res

data class DiaryDetailResponse(
    val diary: DiaryResponse,
    val comments: List<DiaryCommentResponse>
)
