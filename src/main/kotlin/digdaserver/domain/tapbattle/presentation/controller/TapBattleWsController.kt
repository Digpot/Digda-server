package digdaserver.domain.tapbattle.presentation.controller

import digdaserver.domain.tapbattle.application.TapBattleService
import digdaserver.domain.tapbattle.presentation.dto.TapReportRequest
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.UUID

/** 탭배틀 실시간 STOMP 핸들러 — 검증 실패는 해당 요청만 무시(오목과 동일 정책). */
@Controller
class TapBattleWsController(
    private val tapBattleService: TapBattleService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/tapbattle/{gameId}/taps")
    fun taps(
        @DestinationVariable gameId: Long,
        principal: Principal,
        @Payload request: TapReportRequest
    ) {
        try {
            tapBattleService.reportTaps(UUID.fromString(principal.name), gameId, request.taps)
        } catch (e: Exception) {
            log.warn(
                "action=tapbattle_taps_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }

    @MessageMapping("/tapbattle/{gameId}/forfeit")
    fun forfeit(@DestinationVariable gameId: Long, principal: Principal) {
        try {
            tapBattleService.forfeit(UUID.fromString(principal.name), gameId)
        } catch (e: Exception) {
            log.warn(
                "action=tapbattle_forfeit_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }
}
