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
    val location: String?,
    val regionKey: String?,
    val regionSido: String?,
    val regionSigungu: String?,
    val imageUrls: List<String>,
    val createdBy: DiaryUserSummary,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val likeCount: Long,
    val likedByMe: Boolean,
    val reactions: List<DiaryReactionSummary>
) {
    companion object {
        fun from(
            diary: Diary,
            likeCount: Long,
            likedByMe: Boolean,
            reactions: List<DiaryReactionSummary>
        ): DiaryResponse = DiaryResponse(
            id = diary.id,
            title = diary.title,
            content = diary.content,
            date = diary.date,
            weather = diary.weather,
            mood = diary.mood,
            location = diary.location,
            regionKey = diary.regionKey,
            regionSido = diary.regionSido,
            regionSigungu = diary.regionSigungu,
            imageUrls = diary.images.sortedBy { it.sortOrder }.map { it.url },
            createdBy = DiaryUserSummary.from(diary.createdBy),
            createdAt = diary.createdAt,
            updatedAt = diary.updatedAt,
            likeCount = likeCount,
            likedByMe = likedByMe,
            reactions = reactions
        )
    }
}
