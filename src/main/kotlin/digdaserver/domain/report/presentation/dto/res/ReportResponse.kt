package digdaserver.domain.report.presentation.dto.res

import digdaserver.domain.report.domain.entity.Report
import digdaserver.domain.report.domain.entity.ReportReason
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType
import java.time.LocalDateTime

data class ReportResponse(
    val id: Long,
    val targetType: ReportTargetType,
    val targetId: String,
    val reason: ReportReason,
    val status: ReportStatus,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(report: Report): ReportResponse = ReportResponse(
            id = report.id,
            targetType = report.targetType,
            targetId = report.targetId,
            reason = report.reason,
            status = report.status,
            createdAt = report.createdAt
        )
    }
}
