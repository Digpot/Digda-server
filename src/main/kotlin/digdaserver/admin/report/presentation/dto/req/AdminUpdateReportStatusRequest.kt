package digdaserver.admin.report.presentation.dto.req

import digdaserver.domain.report.domain.entity.ReportStatus

/** 신고 처리 상태 전이 요청(RESOLVED/DISMISSED). */
data class AdminUpdateReportStatusRequest(
    val status: ReportStatus
)
