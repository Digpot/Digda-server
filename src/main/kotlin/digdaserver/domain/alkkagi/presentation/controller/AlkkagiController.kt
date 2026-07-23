package digdaserver.domain.alkkagi.presentation.controller

import digdaserver.domain.alkkagi.application.AlkkagiService
import digdaserver.domain.alkkagi.domain.AlkkagiGame
import digdaserver.domain.alkkagi.presentation.dto.AcceptAlkkagiGameRequest
import digdaserver.domain.alkkagi.presentation.dto.AlkkagiGameResponse
import digdaserver.domain.alkkagi.presentation.dto.CreateAlkkagiGameRequest
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
 * 알까기 초대/대국 REST API. 플릭·기권 등 실시간 진행은 STOMP
 * (`/app/alkkagi/{gameId}/...`, 구독 `/topic/alkkagi/{gameId}`) 로 처리한다
 * — [AlkkagiWsController] 참조.
 */
@RestController
@RequestMapping("/alkkagi")
@Tag(name = "Alkkagi", description = "그룹 멤버 간 실시간 알까기 대전 — 초대/수락/대국")
class AlkkagiController(
    private val alkkagiService: AlkkagiService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "알까기 초대",
        description = "같은 그룹 멤버에게 알까기 대결을 신청합니다. 돌 개수(1~10)는 초대자가 정합니다."
    )
    @PostMapping("/games")
    fun createGame(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: CreateAlkkagiGameRequest
    ): ResponseEntity<AlkkagiGameResponse> {
        log.info(
            "api=POST /alkkagi/games, userId={}, groupRoomId={}, inviteeUserId={}, stoneCount={}",
            userId,
            groupRoomId,
            request.inviteeUserId,
            request.stoneCount
        )
        return ResponseEntity.ok(
            alkkagiService.createGame(
                inviterId = UUID.fromString(userId),
                groupRoomId = groupRoomId,
                inviteeId = request.inviteeUserId,
                stoneCount = request.stoneCount,
                formation = AlkkagiGame.Formation.of(request.formation)
            )
        )
    }

    @Operation(summary = "대국 조회", description = "대국 스냅샷(상태/돌/턴). 재접속 동기화용.")
    @GetMapping("/games/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<AlkkagiGameResponse> {
        log.info("api=GET /alkkagi/games/{}, userId={}", gameId, userId)
        return ResponseEntity.ok(alkkagiService.getGame(UUID.fromString(userId), gameId))
    }

    @Operation(
        summary = "초대 수락",
        description = "수락자 대형을 함께 보내면 그 대형으로 배치됩니다. 대국이 시작되고 양쪽에 ACCEPTED 이벤트가 브로드캐스트됩니다."
    )
    @PostMapping("/games/{gameId}/accept")
    fun accept(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long,
        @RequestBody(required = false) request: AcceptAlkkagiGameRequest?
    ): ResponseEntity<AlkkagiGameResponse> {
        log.info(
            "api=POST /alkkagi/games/{}/accept, userId={}, formation={}",
            gameId,
            userId,
            request?.formation
        )
        return ResponseEntity.ok(
            alkkagiService.accept(
                userId = UUID.fromString(userId),
                gameId = gameId,
                formation = AlkkagiGame.Formation.of(request?.formation)
            )
        )
    }

    @Operation(summary = "초대 거절")
    @PostMapping("/games/{gameId}/decline")
    fun decline(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<AlkkagiGameResponse> {
        log.info("api=POST /alkkagi/games/{}/decline, userId={}", gameId, userId)
        return ResponseEntity.ok(alkkagiService.decline(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 취소", description = "초대자가 상대 수락 전에 초대를 거둡니다.")
    @PostMapping("/games/{gameId}/cancel")
    fun cancel(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<AlkkagiGameResponse> {
        log.info("api=POST /alkkagi/games/{}/cancel, userId={}", gameId, userId)
        return ResponseEntity.ok(alkkagiService.cancel(UUID.fromString(userId), gameId))
    }
}
