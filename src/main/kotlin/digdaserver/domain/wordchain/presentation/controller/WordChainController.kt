package digdaserver.domain.wordchain.presentation.controller

import digdaserver.domain.wordchain.application.WordChainService
import digdaserver.domain.wordchain.presentation.dto.CreateWordChainRequest
import digdaserver.domain.wordchain.presentation.dto.WordChainResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * 끝말잇기 방 REST API. 단어 제출·포기는 STOMP(`/app/wordchain/{gameId}/...`,
 * 구독 `/topic/wordchain/{gameId}`) — [WordChainWsController].
 */
@RestController
@RequestMapping("/wordchain")
@Tag(name = "WordChain", description = "그룹 멤버 다인 실시간 끝말잇기")
class WordChainController(
    private val wordChainService: WordChainService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "방 생성 + 초대", description = "그룹 멤버 여럿을 초대해 방을 만듭니다. 턴 제한시간(10~60초) 설정 가능.")
    @PostMapping("/games")
    fun createGame(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: CreateWordChainRequest
    ): ResponseEntity<WordChainResponse> {
        log.info(
            "api=POST /wordchain/games, userId={}, groupRoomId={}, invitees={}, turnSeconds={}",
            userId,
            groupRoomId,
            request.inviteeUserIds.size,
            request.turnSeconds
        )
        return ResponseEntity.ok(
            wordChainService.createGame(
                hostId = UUID.fromString(userId),
                groupRoomId = groupRoomId,
                inviteeIds = request.inviteeUserIds,
                turnSeconds = request.turnSeconds
            )
        )
    }

    @Operation(summary = "게임 조회", description = "스냅샷(참가자/단어 기록/턴). 재접속 동기화용.")
    @GetMapping("/games/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<WordChainResponse> {
        log.info("api=GET /wordchain/games/{}, userId={}", gameId, userId)
        return ResponseEntity.ok(wordChainService.getGame(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "참가")
    @PostMapping("/games/{gameId}/join")
    fun join(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<WordChainResponse> {
        log.info("api=POST /wordchain/games/{}/join, userId={}", gameId, userId)
        return ResponseEntity.ok(wordChainService.join(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 거절")
    @PostMapping("/games/{gameId}/decline")
    fun decline(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<WordChainResponse> {
        log.info("api=POST /wordchain/games/{}/decline, userId={}", gameId, userId)
        return ResponseEntity.ok(wordChainService.decline(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "방 취소", description = "방장이 시작 전에 방을 없앱니다.")
    @PostMapping("/games/{gameId}/cancel")
    fun cancel(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<WordChainResponse> {
        log.info("api=POST /wordchain/games/{}/cancel, userId={}", gameId, userId)
        return ResponseEntity.ok(wordChainService.cancel(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "게임 시작", description = "방장 전용 — 참가 2명 이상. 서버가 시작 단어를 제시합니다.")
    @PostMapping("/games/{gameId}/start")
    fun start(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<WordChainResponse> {
        log.info("api=POST /wordchain/games/{}/start, userId={}", gameId, userId)
        return ResponseEntity.ok(wordChainService.start(UUID.fromString(userId), gameId))
    }
}
