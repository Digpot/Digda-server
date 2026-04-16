package digdaserver.domain.diary.presentation.dto.res

import digdaserver.domain.diary.domain.entity.Diary
import java.time.LocalDate
import java.time.LocalDateTime

data class DiaryResponse(
    val id: Long,
    val title: String,
    val content: String,
    val date: LocalDate,
    val weather: Int,
    val mood: Int,
    val imageUrl: String?,
    val createdBy: DiaryUserSummary,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(diary: Diary): DiaryResponse = DiaryResponse(
            id = diary.id,
            title = diary.title,
            content = diary.content,
            date = diary.date,
            weather = diary.weather,
            mood = diary.mood,
            imageUrl = diary.imageUrl,
            createdBy = DiaryUserSummary.from(diary.createdBy),
            createdAt = diary.createdAt,
            updatedAt = diary.updatedAt
        )
    }
}
