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
    val regionKey: String?,
    val regionSido: String?,
    val regionSigungu: String?,
    val thumbnailUrl: String?,
    val imageCount: Int,
    val createdBy: DiaryUserSummary,
    val commentCount: Int,
    val likeCount: Long,
    val likedByMe: Boolean,
    val createdAt: LocalDateTime,
    /** 이 뷰어에게 숨겨진 일기(차단/신고)인지. true 면 본문 필드는 비워진 상태로 내려간다. */
    val hidden: Boolean = false,
    /** 숨김 사유 코드(BLOCKED_USER / REPORTED / HIDDEN). 보일 때는 null. */
    val hiddenReason: String? = null
) {
    /** 차단/신고로 숨겨야 할 때 — 본문을 비우고 플래그만 남긴 사본. 날짜·기분·작성자는 슬롯/표시용으로 유지. */
    fun asHidden(reason: String): DiarySummaryResponse = copy(
        title = "",
        location = null,
        thumbnailUrl = null,
        imageCount = 0,
        hidden = true,
        hiddenReason = reason
    )

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
                regionKey = diary.regionKey,
                regionSido = diary.regionSido,
                regionSigungu = diary.regionSigungu,
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
