package digdaserver.admin.report.presentation.dto.res

import digdaserver.domain.report.domain.entity.Report
import digdaserver.domain.report.domain.entity.ReportReason
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "어드민용 신고 정보")
data class AdminReportResponse(

    @Schema(description = "신고 ID")
    val reportId: Long,

    @Schema(description = "신고자 ID(UUID)")
    val reporterId: String,

    @Schema(description = "신고자 이름")
    val reporterName: String,

    @Schema(description = "대상 종류")
    val targetType: ReportTargetType,

    @Schema(description = "대상 ID(콘텐츠는 PK, USER 는 UUID)")
    val targetId: String,

    @Schema(description = "피신고자 ID(UUID). 대상 콘텐츠/사용자의 작성자. 해석 불가 시 null")
    val reportedUserId: String?,

    @Schema(description = "피신고자 이름. 해석 불가 시 null")
    val reportedUserName: String?,

    @Schema(description = "피신고자의 현재 이용 제한 상태. 해석 불가 시 null")
    val reportedUserRestricted: Boolean?,

    @Schema(description = "대상이 속한 그룹방 ID")
    val groupRoomId: Long?,

    @Schema(description = "신고 사유")
    val reason: ReportReason,

    @Schema(description = "상세 사유")
    val detail: String?,

    @Schema(description = "처리 상태")
    val status: ReportStatus,

    @Schema(description = "접수 시각")
    val createdAt: LocalDateTime,

    @Schema(description = "검토 시각")
    val reviewedAt: LocalDateTime?,

    @Schema(description = "신고된 콘텐츠 원본 스냅샷(검토용)")
    val targetContent: AdminReportTargetContentResponse
) {
    companion object {
        fun from(
            report: Report,
            reportedUserId: String? = null,
            reportedUserName: String? = null,
            reportedUserRestricted: Boolean? = null,
            targetContent: AdminReportTargetContentResponse =
                AdminReportTargetContentResponse.unavailable()
        ): AdminReportResponse = AdminReportResponse(
            reportId = report.id,
            reporterId = report.reporter.id.toString(),
            reporterName = report.reporter.displayedName(),
            targetType = report.targetType,
            targetId = report.targetId,
            reportedUserId = reportedUserId,
            reportedUserName = reportedUserName,
            reportedUserRestricted = reportedUserRestricted,
            groupRoomId = report.groupRoomId,
            reason = report.reason,
            detail = report.detail,
            status = report.status,
            createdAt = report.createdAt,
            reviewedAt = report.reviewedAt,
            targetContent = targetContent
        )
    }
}
