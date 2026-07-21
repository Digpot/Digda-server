package digdaserver.domain.alkkagi.presentation.controller

import digdaserver.domain.alkkagi.application.AlkkagiService
import digdaserver.domain.alkkagi.presentation.dto.AlkkagiMoveRequest
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.UUID

/**
 * 알까기 실시간 진행 STOMP 핸들러. Principal 은 WebSocketConfig 의 CONNECT 인증에서
 * 설정된 userId (name). 결과는 서비스가 `/topic/alkkagi/{gameId}` 로 브로드캐스트한다.
 *
 * 수 검증 실패(DigdaException)는 해당 클라이언트 요청만 무시된다 — 돌 상태는
 * 브로드캐스트 스냅샷이 진실 출처라 클라이언트는 다음 이벤트로 재동기화된다.
 */
@Controller
class AlkkagiWsController(
    private val alkkagiService: AlkkagiService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/alkkagi/{gameId}/move")
    fun move(
        @DestinationVariable gameId: Long,
        principal: Principal,
        @Payload request: AlkkagiMoveRequest
    ) {
        try {
            alkkagiService.move(UUID.fromString(principal.name), gameId, request)
        } catch (e: Exception) {
            log.warn(
                "action=alkkagi_move_rejected, gameId={}, userId={}, stoneId={}, reason={}",
                gameId,
                principal.name,
                request.stoneId,
                e.message
            )
        }
    }

    @MessageMapping("/alkkagi/{gameId}/forfeit")
    fun forfeit(
        @DestinationVariable gameId: Long,
        principal: Principal
    ) {
        try {
            alkkagiService.forfeit(UUID.fromString(principal.name), gameId)
        } catch (e: Exception) {
            log.warn(
                "action=alkkagi_forfeit_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }
}
