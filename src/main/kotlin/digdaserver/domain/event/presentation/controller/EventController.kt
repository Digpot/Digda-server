package digdaserver.domain.event.presentation.controller

import digdaserver.domain.event.application.service.ExpEventService
import digdaserver.domain.event.presentation.dto.res.ExpEventResponse
import digdaserver.global.infra.logging.LogUserContext.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 앱이 진행 중인 시즌 이벤트(경험치 배수)를 조회한다 — 배너/배지 노출용. */
@RestController
@RequestMapping("/events")
@Tag(name = "Event", description = "시즌 이벤트 조회")
class EventController(
    private val expEventService: ExpEventService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "경험치 배수 이벤트 조회",
        description = "active=true 이면 지금 경험치에 multiplier 가 곱해지는 중. 배너 문구는 title."
    )
    @GetMapping("/exp")
    fun getExpEvent(): ResponseEntity<ExpEventResponse> {
        log.info("api=GET /events/exp, userId={}", currentUserId())
        return ResponseEntity.ok(expEventService.get())
    }
}
