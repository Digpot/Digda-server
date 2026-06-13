package digdaserver.admin.report.application.service

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.report.presentation.dto.res.AdminReportResponse
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType

interface AdminReportService {

    fun search(
        status: ReportStatus?,
        targetType: ReportTargetType?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminReportResponse>

    /** 신고 처리 상태 전이(RESOLVED/DISMISSED). reviewedAt 을 기록한다. */
    fun updateStatus(reportId: Long, status: ReportStatus): AdminReportResponse
}
