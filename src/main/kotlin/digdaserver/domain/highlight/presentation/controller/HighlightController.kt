package digdaserver.domain.highlight.presentation.controller

import digdaserver.domain.highlight.application.service.MonthlyHighlightService
import digdaserver.domain.highlight.presentation.dto.res.HighlightDetailResponse
import digdaserver.domain.highlight.presentation.dto.res.HighlightListResponse
import digdaserver.global.infra.logging.LogUserContext.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.util.UUID

@RestController
@Tag(name = "Highlight", description = "월간 하이라이트 API")
class HighlightController(
    private val highlightService: MonthlyHighlightService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "월간 하이라이트 목록", description = "그룹의 생성된 월간 하이라이트를 최신 월부터 반환합니다. 이전 달 다시보기용.")
    @GetMapping("/group-rooms/{groupRoomId}/highlights")
    fun list(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<HighlightListResponse> {
        log.info("api=GET /group-rooms/{}/highlights, userId={}", groupRoomId, currentUserId())
        return ResponseEntity.ok(highlightService.list(UUID.fromString(userId), groupRoomId))
    }

    @Operation(summary = "월간 하이라이트 상세", description = "month 는 yyyy-MM. 해당 월 하이라이트가 없으면 404(HIGHLIGHT_NOT_FOUND).")
    @GetMapping("/group-rooms/{groupRoomId}/highlights/{month}")
    fun get(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable month: YearMonth
    ): ResponseEntity<HighlightDetailResponse> {
        log.info("api=GET /group-rooms/{}/highlights/{}, userId={}", groupRoomId, month, currentUserId())
        return ResponseEntity.ok(highlightService.get(UUID.fromString(userId), groupRoomId, month))
    }
}
