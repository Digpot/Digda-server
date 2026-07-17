package digdaserver.domain.tapbattle.presentation.controller

import digdaserver.domain.tapbattle.application.TapBattleService
import digdaserver.domain.tapbattle.presentation.dto.CreateTapBattleRequest
import digdaserver.domain.tapbattle.presentation.dto.TapBattleResponse
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
 * 탭배틀 초대/대결 REST API. 탭 보고·기권은 STOMP(`/app/tapbattle/{gameId}/...`,
 * 구독 `/topic/tapbattle/{gameId}`) — [TapBattleWsController].
 */
@RestController
@RequestMapping("/tapbattle")
@Tag(name = "TapBattle", description = "그룹 멤버 간 실시간 연타 대결")
class TapBattleController(
    private val tapBattleService: TapBattleService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "탭배틀 초대", description = "같은 그룹 멤버에게 연타 대결을 신청합니다. 상대에게 알림이 발송됩니다.")
    @PostMapping("/games")
    fun createGame(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: CreateTapBattleRequest
    ): ResponseEntity<TapBattleResponse> {
        log.info(
            "api=POST /tapbattle/games, userId={}, groupRoomId={}, inviteeUserId={}",
            userId,
            groupRoomId,
            request.inviteeUserId
        )
        return ResponseEntity.ok(
            tapBattleService.createGame(UUID.fromString(userId), groupRoomId, request.inviteeUserId)
        )
    }

    @Operation(summary = "대결 조회", description = "스냅샷(상태/점수/타이머). 재접속 동기화용.")
    @GetMapping("/games/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<TapBattleResponse> {
        log.info("api=GET /tapbattle/games/{}, userId={}", gameId, userId)
        return ResponseEntity.ok(tapBattleService.getGame(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 수락", description = "3초 카운트다운 후 15초 연타 시작 — ACCEPTED 브로드캐스트.")
    @PostMapping("/games/{gameId}/accept")
    fun accept(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<TapBattleResponse> {
        log.info("api=POST /tapbattle/games/{}/accept, userId={}", gameId, userId)
        return ResponseEntity.ok(tapBattleService.accept(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 거절")
    @PostMapping("/games/{gameId}/decline")
    fun decline(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<TapBattleResponse> {
        log.info("api=POST /tapbattle/games/{}/decline, userId={}", gameId, userId)
        return ResponseEntity.ok(tapBattleService.decline(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 취소", description = "초대자가 상대 수락 전에 초대를 거둡니다.")
    @PostMapping("/games/{gameId}/cancel")
    fun cancel(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<TapBattleResponse> {
        log.info("api=POST /tapbattle/games/{}/cancel, userId={}", gameId, userId)
        return ResponseEntity.ok(tapBattleService.cancel(UUID.fromString(userId), gameId))
    }
}
