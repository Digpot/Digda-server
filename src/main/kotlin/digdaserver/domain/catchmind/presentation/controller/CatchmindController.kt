package digdaserver.domain.catchmind.presentation.controller

import digdaserver.domain.catchmind.application.CatchmindService
import digdaserver.domain.catchmind.presentation.dto.CatchmindGameResponse
import digdaserver.domain.catchmind.presentation.dto.CreateCatchmindRequest
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
 * 캐치마인드 방 REST API. 드로잉/추리 등 실시간 진행은 STOMP
 * (`/app/catchmind/{gameId}/...`, 구독 `/topic/catchmind/{gameId}`) — [CatchmindWsController].
 */
@RestController
@RequestMapping("/catchmind")
@Tag(name = "Catchmind", description = "그룹 멤버 다인 실시간 그림 맞추기")
class CatchmindController(
    private val catchmindService: CatchmindService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "방 생성 + 초대", description = "같은 그룹 멤버 여럿을 초대해 방을 만듭니다. 초대 알림이 발송됩니다.")
    @PostMapping("/games")
    fun createGame(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: CreateCatchmindRequest
    ): ResponseEntity<CatchmindGameResponse> {
        log.info(
            "api=POST /catchmind/games, userId={}, groupRoomId={}, invitees={}",
            userId,
            groupRoomId,
            request.inviteeUserIds.size
        )
        return ResponseEntity.ok(
            catchmindService.createGame(UUID.fromString(userId), groupRoomId, request.inviteeUserIds)
        )
    }

    @Operation(summary = "게임 조회", description = "스냅샷(참가자/라운드/누적 획). 출제자에게만 word 가 채워집니다.")
    @GetMapping("/games/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<CatchmindGameResponse> {
        log.info("api=GET /catchmind/games/{}, userId={}", gameId, userId)
        return ResponseEntity.ok(catchmindService.getGame(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "참가", description = "초대받은 방에 참가합니다. PLAYER_JOINED 브로드캐스트.")
    @PostMapping("/games/{gameId}/join")
    fun join(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<CatchmindGameResponse> {
        log.info("api=POST /catchmind/games/{}/join, userId={}", gameId, userId)
        return ResponseEntity.ok(catchmindService.join(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 거절")
    @PostMapping("/games/{gameId}/decline")
    fun decline(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<CatchmindGameResponse> {
        log.info("api=POST /catchmind/games/{}/decline, userId={}", gameId, userId)
        return ResponseEntity.ok(catchmindService.decline(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "방 취소", description = "방장이 시작 전에 방을 없앱니다.")
    @PostMapping("/games/{gameId}/cancel")
    fun cancel(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<CatchmindGameResponse> {
        log.info("api=POST /catchmind/games/{}/cancel, userId={}", gameId, userId)
        return ResponseEntity.ok(catchmindService.cancel(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "게임 시작", description = "방장 전용 — 참가 2명 이상일 때 시작. STARTED + ROUND_START 브로드캐스트.")
    @PostMapping("/games/{gameId}/start")
    fun start(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<CatchmindGameResponse> {
        log.info("api=POST /catchmind/games/{}/start, userId={}", gameId, userId)
        return ResponseEntity.ok(catchmindService.start(UUID.fromString(userId), gameId))
    }
}
