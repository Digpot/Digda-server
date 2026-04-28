package digdaserver.admin.announcement.presentation.dto.res

import digdaserver.domain.announcement.domain.entity.Announcement
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "관리자 공지 항목")
data class AdminAnnouncementResponse(

    @Schema(description = "공지 ID")
    val announcementId: Long,

    @Schema(description = "공지 제목")
    val title: String,

    @Schema(description = "공지 본문")
    val body: String,

    @Schema(description = "발송 대상 (ALL / USER_IDS)")
    val targetType: String,

    @Schema(description = "실제 발송된 수신자 수")
    val recipientCount: Int,

    @Schema(description = "발송 시각")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(entity: Announcement): AdminAnnouncementResponse = AdminAnnouncementResponse(
            announcementId = entity.id,
            title = entity.title,
            body = entity.body,
            targetType = entity.targetType.name,
            recipientCount = entity.recipientCount,
            createdAt = entity.createdAt
        )
    }
}
