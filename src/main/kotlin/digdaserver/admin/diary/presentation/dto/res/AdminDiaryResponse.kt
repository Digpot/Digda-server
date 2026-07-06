package digdaserver.admin.diary.presentation.dto.res

import digdaserver.domain.diary.domain.entity.Diary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "어드민용 일기 정보")
data class AdminDiaryResponse(

    @Schema(description = "일기 ID")
    val diaryId: Long,

    @Schema(description = "그룹방 ID")
    val groupRoomId: Long,

    @Schema(description = "그룹방 이름")
    val groupRoomName: String,

    @Schema(description = "작성자 ID(UUID)")
    val createdBy: String,

    @Schema(description = "작성자 이름")
    val authorName: String,

    @Schema(description = "제목")
    val title: String,

    @Schema(description = "내용")
    val content: String,

    @Schema(description = "일기 날짜")
    val date: LocalDate,

    @Schema(description = "날씨(0~3)")
    val weather: Int,

    @Schema(description = "기분(0~4)")
    val mood: Int,

    @Schema(description = "장소")
    val location: String?,

    @Schema(description = "이미지 URL 목록 (정렬 순)")
    val imageUrls: List<String>,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "수정 시각")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(diary: Diary): AdminDiaryResponse = AdminDiaryResponse(
            diaryId = diary.id,
            groupRoomId = diary.groupRoom.id,
            groupRoomName = diary.groupRoom.name,
            createdBy = diary.createdBy.id.toString(),
            authorName = diary.createdBy.name,
            title = diary.title,
            content = diary.content,
            date = diary.date,
            weather = diary.weather,
            mood = diary.mood,
            location = diary.location,
            imageUrls = diary.images.sortedBy { it.sortOrder }.map { it.url },
            createdAt = diary.createdAt,
            updatedAt = diary.updatedAt
        )
    }
}
