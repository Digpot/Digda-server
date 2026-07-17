package digdaserver.domain.molebattle.presentation.controller

import digdaserver.domain.molebattle.application.MoleBattleService
import digdaserver.domain.molebattle.presentation.dto.CreateMoleBattleRequest
import digdaserver.domain.molebattle.presentation.dto.MoleBattleResponse
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
 * 두더지 잡기 초대/대결 REST API. 점수 보고·기권은 STOMP(`/app/molebattle/{gameId}/...`,
 * 구독 `/topic/molebattle/{gameId}`) — [MoleBattleWsController].
 */
@RestController
@RequestMapping("/molebattle")
@Tag(name = "MoleBattle", description = "그룹 멤버 간 실시간 두더지 잡기 대결")
class MoleBattleController(
    private val moleBattleService: MoleBattleService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "두더지 잡기 초대", description = "같은 그룹 멤버에게 대결을 신청합니다. 상대에게 알림이 발송됩니다.")
    @PostMapping("/games")
    fun createGame(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: CreateMoleBattleRequest
    ): ResponseEntity<MoleBattleResponse> {
        log.info(
            "api=POST /molebattle/games, userId={}, groupRoomId={}, inviteeUserId={}",
            userId,
            groupRoomId,
            request.inviteeUserId
        )
        return ResponseEntity.ok(
            moleBattleService.createGame(UUID.fromString(userId), groupRoomId, request.inviteeUserId)
        )
    }

    @Operation(summary = "대결 조회", description = "스냅샷(상태/점수/시드/타이머). 재접속 동기화용.")
    @GetMapping("/games/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<MoleBattleResponse> {
        log.info("api=GET /molebattle/games/{}, userId={}", gameId, userId)
        return ResponseEntity.ok(moleBattleService.getGame(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 수락", description = "공유 스폰 시드가 발급되고 3초 카운트다운 후 30초 대결 시작.")
    @PostMapping("/games/{gameId}/accept")
    fun accept(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<MoleBattleResponse> {
        log.info("api=POST /molebattle/games/{}/accept, userId={}", gameId, userId)
        return ResponseEntity.ok(moleBattleService.accept(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 거절")
    @PostMapping("/games/{gameId}/decline")
    fun decline(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<MoleBattleResponse> {
        log.info("api=POST /molebattle/games/{}/decline, userId={}", gameId, userId)
        return ResponseEntity.ok(moleBattleService.decline(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 취소", description = "초대자가 상대 수락 전에 초대를 거둡니다.")
    @PostMapping("/games/{gameId}/cancel")
    fun cancel(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<MoleBattleResponse> {
        log.info("api=POST /molebattle/games/{}/cancel, userId={}", gameId, userId)
        return ResponseEntity.ok(moleBattleService.cancel(UUID.fromString(userId), gameId))
    }
}
