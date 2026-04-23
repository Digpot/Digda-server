package digdaserver.admin.log.presentation.dto.res

import digdaserver.domain.log.domain.entity.UserActionLog
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "유저 행동 로그")
data class AdminUserLogResponse(

    @Schema(description = "로그 ID")
    val logId: Long,

    @Schema(description = "행위자 ID(UUID). 비로그인/시스템 기록은 null 가능")
    val actorId: String?,

    @Schema(description = "액션 타입")
    val action: String,

    @Schema(description = "대상 타입 (USER/GROUP_ROOM/DIARY/SCHEDULE/COMMENT/TODO 등)")
    val targetType: String?,

    @Schema(description = "대상 ID")
    val targetId: String?,

    @Schema(description = "상세 메시지")
    val detail: String?,

    @Schema(description = "생성 시각")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(log: UserActionLog): AdminUserLogResponse = AdminUserLogResponse(
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
