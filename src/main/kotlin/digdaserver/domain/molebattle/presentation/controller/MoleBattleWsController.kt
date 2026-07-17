package digdaserver.domain.molebattle.presentation.controller

import digdaserver.domain.molebattle.application.MoleBattleService
import digdaserver.domain.molebattle.presentation.dto.MoleScoreRequest
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.UUID

/** 두더지 잡기 실시간 STOMP 핸들러 — 검증 실패는 해당 요청만 무시. */
@Controller
class MoleBattleWsController(
    private val moleBattleService: MoleBattleService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/molebattle/{gameId}/score")
    fun score(
        @DestinationVariable gameId: Long,
        principal: Principal,
        @Payload request: MoleScoreRequest
    ) {
        try {
            moleBattleService.reportScore(UUID.fromString(principal.name), gameId, request.score)
        } catch (e: Exception) {
            log.warn(
                "action=molebattle_score_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }

    @MessageMapping("/molebattle/{gameId}/forfeit")
    fun forfeit(@DestinationVariable gameId: Long, principal: Principal) {
        try {
            moleBattleService.forfeit(UUID.fromString(principal.name), gameId)
        } catch (e: Exception) {
            log.warn(
                "action=molebattle_forfeit_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }
}
