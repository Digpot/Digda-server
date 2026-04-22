package digdaserver.admin.schedule.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.schedule.application.service.AdminScheduleService
import digdaserver.admin.schedule.presentation.dto.res.AdminScheduleResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/schedules")
@Tag(name = "Admin - Schedule", description = "관리자 일정 관리 API")
class AdminScheduleController(
    private val adminScheduleService: AdminScheduleService
) {

    @Operation(summary = "일정 목록 조회", description = "페이징, 키워드(제목)")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminScheduleResponse>> {
        return ResponseEntity.ok(adminScheduleService.search(keyword, page, size))
    }

    @Operation(summary = "일정 상세 조회")
    @GetMapping("/{scheduleId}")
    fun getDetail(@PathVariable scheduleId: Long): ResponseEntity<AdminScheduleResponse> {
        return ResponseEntity.ok(adminScheduleService.getDetail(scheduleId))
    }
}
