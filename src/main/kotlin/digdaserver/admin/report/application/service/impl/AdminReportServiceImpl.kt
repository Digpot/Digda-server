package digdaserver.admin.report.application.service.impl

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.report.application.service.AdminReportService
import digdaserver.admin.report.presentation.dto.res.AdminReportResponse
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType
import digdaserver.domain.report.domain.repository.ReportRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminReportServiceImpl(
    private val reportRepository: ReportRepository
) : AdminReportService {

    override fun search(
        status: ReportStatus?,
        targetType: ReportTargetType?,
        page: Int,
        size: Int
    ): AdminPageResponse<AdminReportResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = reportRepository.searchForAdmin(status, targetType, pageable)
        return AdminPageResponse.of(result, AdminReportResponse::from)
    }

    @Transactional
    override fun updateStatus(reportId: Long, status: ReportStatus): AdminReportResponse {
        val report = reportRepository.findById(reportId)
            .orElseThrow { DigdaException(ErrorCode.RESOURCE_NOT_FOUND) }
        report.markReviewed(status)
        return AdminReportResponse.from(report)
    }
}
