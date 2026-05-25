package digdaserver.domain.diary.presentation.dto.res

import digdaserver.domain.diary.domain.entity.Diary
import java.time.LocalDate
import java.time.LocalDateTime

data class DiarySummaryResponse(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val weather: Int,
    val mood: Int,
    val location: String?,
    val thumbnailUrl: String?,
    val imageCount: Int,
    val createdBy: DiaryUserSummary,
    val commentCount: Int,
    val likeCount: Long,
    val likedByMe: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(
            diary: Diary,
            commentCount: Int,
            likeCount: Long,
            likedByMe: Boolean
        ): DiarySummaryResponse {
            val sorted = diary.images.sortedBy { it.sortOrder }
            return DiarySummaryResponse(
                id = diary.id,
                title = diary.title,
                date = diary.date,
                weather = diary.weather,
                mood = diary.mood,
                location = diary.location,
                thumbnailUrl = sorted.firstOrNull()?.url,
                imageCount = sorted.size,
                createdBy = DiaryUserSummary.from(diary.createdBy),
                commentCount = commentCount,
                likeCount = likeCount,
                likedByMe = likedByMe,
                createdAt = diary.createdAt
            )
        }
    }
}
