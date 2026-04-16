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
    val imageUrl: String?,
    val createdBy: DiaryUserSummary,
    val commentCount: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(diary: Diary, commentCount: Int): DiarySummaryResponse = DiarySummaryResponse(
            id = diary.id,
            title = diary.title,
            date = diary.date,
            weather = diary.weather,
            mood = diary.mood,
            imageUrl = diary.imageUrl,
            createdBy = DiaryUserSummary.from(diary.createdBy),
            commentCount = commentCount,
            createdAt = diary.createdAt
        )
    }
}
