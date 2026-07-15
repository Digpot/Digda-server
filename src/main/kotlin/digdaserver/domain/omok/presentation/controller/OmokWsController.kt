package digdaserver.domain.omok.presentation.controller

import digdaserver.domain.omok.application.OmokService
import digdaserver.domain.omok.presentation.dto.OmokMoveRequest
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.UUID

/**
 * 오목 실시간 진행 STOMP 핸들러. Principal 은 WebSocketConfig 의 CONNECT 인증에서
 * 설정된 userId (name). 결과는 서비스가 `/topic/omok/{gameId}` 로 브로드캐스트한다.
 *
 * 착수 검증 실패(DigdaException)는 해당 클라이언트 요청만 무시된다 — 보드 상태는
 * 브로드캐스트 스냅샷이 진실 출처라 클라이언트는 다음 이벤트로 재동기화된다.
 */
@Controller
class OmokWsController(
    private val omokService: OmokService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/omok/{gameId}/move")
    fun move(
        @DestinationVariable gameId: Long,
        principal: Principal,
        @Payload request: OmokMoveRequest
    ) {
        try {
            omokService.move(UUID.fromString(principal.name), gameId, request.x, request.y)
        } catch (e: Exception) {
            log.warn(
                "action=omok_move_rejected, gameId={}, userId={}, x={}, y={}, reason={}",
                gameId,
                principal.name,
                request.x,
                request.y,
                e.message
            )
        }
    }

    @MessageMapping("/omok/{gameId}/forfeit")
    fun forfeit(
        @DestinationVariable gameId: Long,
        principal: Principal
    ) {
        try {
            omokService.forfeit(UUID.fromString(principal.name), gameId)
        } catch (e: Exception) {
            log.warn(
                "action=omok_forfeit_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }
}
