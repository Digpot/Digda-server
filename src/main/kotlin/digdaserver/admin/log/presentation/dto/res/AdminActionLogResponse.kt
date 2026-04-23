package digdaserver.admin.log.presentation.dto.res

import digdaserver.admin.log.domain.entity.AdminActionLog
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "관리자 행위 로그")
data class AdminActionLogResponse(

    @Schema(description = "로그 ID")
    val logId: Long,

    @Schema(description = "행위자 ID(UUID). 시스템 로그인 경우 null 가능")
    val actorId: String?,

    @Schema(description = "액션 타입")
    val action: String,

    @Schema(description = "대상 타입 (USER/GROUP_ROOM/DIARY/TABLE 등)")
    val targetType: String?,

    @Schema(description = "대상 ID")
    val targetId: String?,

    @Schema(description = "상세 메시지")
    val detail: String?,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(log: AdminActionLog): AdminActionLogResponse = AdminActionLogResponse(
            logId = log.id,
            actorId = log.actorId?.toString(),
            action = log.action.name,
            targetType = log.targetType,
            targetId = log.targetId,
            detail = log.detail,
            createdAt = log.createdAt
        )
    }
}
