package digdaserver.domain.report.presentation.dto.req

import digdaserver.domain.report.domain.entity.ReportReason
import digdaserver.domain.report.domain.entity.ReportTargetType

/**
 * 신고 요청.
 *
 * - [targetId] : DIARY/COMMENT/SCHEDULE 는 Long PK 문자열, USER 는 UUID 문자열.
 * - [groupRoomId] : 대상이 속한 그룹방(있으면). 어드민 추적용으로만 쓰여 선택값.
 */
data class CreateReportRequest(
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason,
    val detail: String? = null,
    val groupRoomId: Long? = null
)
