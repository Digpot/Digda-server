package digdaserver.domain.wordchain.presentation.controller

import digdaserver.domain.wordchain.application.WordChainService
import digdaserver.domain.wordchain.presentation.dto.WordSubmitRequest
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import java.security.Principal
import java.util.UUID

/** 끝말잇기 실시간 STOMP 핸들러 — 검증 실패는 해당 요청만 무시. */
@Controller
class WordChainWsController(
    private val wordChainService: WordChainService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/wordchain/{gameId}/word")
    fun word(
        @DestinationVariable gameId: Long,
        principal: Principal,
        @Payload request: WordSubmitRequest
    ) {
        try {
            wordChainService.submitWord(UUID.fromString(principal.name), gameId, request.word)
        } catch (e: Exception) {
            log.warn(
                "action=wordchain_word_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }

    @MessageMapping("/wordchain/{gameId}/forfeit")
    fun forfeit(@DestinationVariable gameId: Long, principal: Principal) {
        try {
            wordChainService.forfeit(UUID.fromString(principal.name), gameId)
        } catch (e: Exception) {
            log.warn(
                "action=wordchain_forfeit_rejected, gameId={}, userId={}, reason={}",
                gameId,
                principal.name,
                e.message
            )
        }
    }
}
