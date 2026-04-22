package digdaserver.admin.log.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.log.application.service.AdminActionLogService
import digdaserver.admin.log.domain.entity.AdminAction
import digdaserver.admin.log.presentation.dto.res.AdminActionLogResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "Admin - Log", description = "관리자 행위/시스템 로그 조회 API")
class AdminActionLogController(
    private val adminActionLogService: AdminActionLogService
) {

    @Operation(
        summary = "로그 조회",
        description = "최신순 정렬. actorId/action/기간/키워드(detail·targetId) 필터 지원."
    )
    @GetMapping
    fun search(
        @RequestParam(required = false) actorId: UUID?,
        @RequestParam(required = false) action: AdminAction?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminActionLogResponse>> {
        return ResponseEntity.ok(
            adminActionLogService.search(actorId, action, from, to, keyword, page, size)
        )
    }
}
