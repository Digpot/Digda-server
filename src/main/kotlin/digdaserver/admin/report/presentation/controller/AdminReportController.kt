package digdaserver.admin.report.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.report.application.service.AdminReportService
import digdaserver.admin.report.presentation.dto.req.AdminUpdateReportStatusRequest
import digdaserver.admin.report.presentation.dto.res.AdminReportResponse
import digdaserver.domain.report.domain.entity.ReportStatus
import digdaserver.domain.report.domain.entity.ReportTargetType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin - Report", description = "관리자 신고 관리 API")
class AdminReportController(
    private val adminReportService: AdminReportService
) {

    @Operation(summary = "신고 목록 조회", description = "상태/대상종류 필터 + 페이징. 최신순.")
    @GetMapping
    fun search(
        @RequestParam(required = false) status: ReportStatus?,
        @RequestParam(required = false) targetType: ReportTargetType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminReportResponse>> {
        return ResponseEntity.ok(adminReportService.search(status, targetType, page, size))
    }

    @Operation(summary = "신고 처리", description = "신고를 RESOLVED(조치 완료) 또는 DISMISSED(반려)로 전이합니다.")
    @PatchMapping("/{reportId}/status")
    fun updateStatus(
        @PathVariable reportId: Long,
        @RequestBody request: AdminUpdateReportStatusRequest
    ): ResponseEntity<AdminReportResponse> {
        return ResponseEntity.ok(adminReportService.updateStatus(reportId, request.status))
    }
}
