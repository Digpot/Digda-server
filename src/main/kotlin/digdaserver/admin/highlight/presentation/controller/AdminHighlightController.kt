package digdaserver.admin.highlight.presentation.controller

import digdaserver.admin.highlight.presentation.dto.req.AdminGenerateHighlightRequest
import digdaserver.domain.highlight.application.scheduler.HighlightRunResult
import digdaserver.domain.highlight.application.scheduler.MonthlyHighlightScheduler
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.ZoneId

/** 어드민 월간 하이라이트 수동 생성. `/api/admin` 하위라 ROLE_ADMIN 으로 보호된다. */
@RestController
@RequestMapping("/api/admin/highlights")
@Tag(name = "Admin - Highlight", description = "관리자 월간 하이라이트 수동 생성")
class AdminHighlightController(
    private val scheduler: MonthlyHighlightScheduler
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "월간 하이라이트 수동 생성",
        description = "스케줄러와 같은 로직을 즉시 실행한다. 이미 있는 그룹은 건너뛰고, regenerate=true 면 지우고 다시 만든다. " +
            "미래 달은 만들 수 없다."
    )
    @PostMapping("/generate")
    fun generate(
        @Valid
        @RequestBody
        request: AdminGenerateHighlightRequest
    ): ResponseEntity<HighlightRunResult> {
        val thisMonth = YearMonth.now(ZoneId.of("Asia/Seoul"))
        val month = request.month?.let { YearMonth.parse(it) } ?: scheduler.lastMonth()
        if (!month.isBefore(thisMonth)) throw DigdaException(ErrorCode.HIGHLIGHT_FUTURE_MONTH)
        log.info(
            "api=POST /api/admin/highlights/generate, month={}, groupRoomId={}, regenerate={}",
            month,
            request.groupRoomId,
            request.regenerate
        )
        val result = scheduler.execute(
            month = month,
            groupId = request.groupRoomId,
            regenerate = request.regenerate,
            notify = request.notify,
            trigger = "admin"
        )
        return ResponseEntity.ok(result)
    }
}
