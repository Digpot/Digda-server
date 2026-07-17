package digdaserver.domain.catchmind.presentation.controller

import digdaserver.domain.catchmind.application.CatchmindService
import digdaserver.domain.catchmind.presentation.dto.CatchmindGuessRequest
import digdaserver.domain.catchmind.presentation.dto.CatchmindStrokeRequest
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.UUID

/**
 * 캐치마인드 실시간 STOMP 핸들러. 검증 실패는 해당 요청만 무시 — 클라이언트는
 * 브로드캐스트 스냅샷으로 재동기화한다 (오목과 동일한 정책).
 */
@Controller
class CatchmindWsController(
    private val catchmindService: CatchmindService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/catchmind/{gameId}/stroke")
    fun stroke(
        @DestinationVariable gameId: Long,
        principal: Principal,
        @Payload request: CatchmindStrokeRequest
    ) {
        try {
            catchmindService.stroke(UUID.fromString(principal.name), gameId, request)
        } catch (e: Exception) {
            log.warn(
                "action=catchmind_stroke_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }

    @MessageMapping("/catchmind/{gameId}/clear")
    fun clear(@DestinationVariable gameId: Long, principal: Principal) {
        try {
            catchmindService.clear(UUID.fromString(principal.name), gameId)
        } catch (e: Exception) {
            log.warn(
                "action=catchmind_clear_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }

    @MessageMapping("/catchmind/{gameId}/skip")
    fun skip(@DestinationVariable gameId: Long, principal: Principal) {
        try {
            catchmindService.skip(UUID.fromString(principal.name), gameId)
        } catch (e: Exception) {
            log.warn(
                "action=catchmind_skip_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }

    @MessageMapping("/catchmind/{gameId}/guess")
    fun guess(
        @DestinationVariable gameId: Long,
        principal: Principal,
        @Payload request: CatchmindGuessRequest
    ) {
        try {
            catchmindService.guess(UUID.fromString(principal.name), gameId, request.text)
        } catch (e: Exception) {
            log.warn(
                "action=catchmind_guess_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }
}
