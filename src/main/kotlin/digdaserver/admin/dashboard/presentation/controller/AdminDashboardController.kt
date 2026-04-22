package digdaserver.admin.dashboard.presentation.controller

import digdaserver.admin.dashboard.application.service.AdminDashboardService
import digdaserver.admin.dashboard.presentation.dto.res.DashboardSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin - Dashboard", description = "관리자 대시보드 API")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService
) {

    @Operation(summary = "대시보드 요약 통계", description = "총 사용자/그룹방/일기/일정/댓글/할일 등 핵심 지표를 반환합니다.")
    @GetMapping("/summary")
    fun getSummary(): ResponseEntity<DashboardSummaryResponse> {
        return ResponseEntity.ok(adminDashboardService.getSummary())
    }
}
