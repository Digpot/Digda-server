package digdaserver.domain.diary.presentation.dto.res

data class DiaryLikeResponse(
    val likedByMe: Boolean,
    val likeCount: Long
)
