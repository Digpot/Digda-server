package digdaserver.domain.report.presentation.controller

import digdaserver.domain.report.application.service.ReportService
import digdaserver.domain.report.presentation.dto.req.CreateReportRequest
import digdaserver.domain.report.presentation.dto.res.ReportResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Report", description = "신고 API")
class ReportController(
    private val reportService: ReportService
) {

    @Operation(
        summary = "신고하기",
        description = "일기/댓글/일정/사용자를 신고합니다. 콘텐츠 신고는 신고자 본인에게서 자동 숨김됩니다."
    )
    @PostMapping("/reports")
    fun createReport(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: CreateReportRequest
    ): ResponseEntity<ReportResponse> {
        val response = reportService.createReport(UUID.fromString(userId), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
