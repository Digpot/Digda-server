package digdaserver.domain.omok.presentation.controller

import digdaserver.domain.omok.application.OmokService
import digdaserver.domain.omok.presentation.dto.CreateOmokGameRequest
import digdaserver.domain.omok.presentation.dto.OmokGameResponse
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
 * 오목 초대/대국 REST API. 착수·기권 등 실시간 진행은 STOMP(`/app/omok/{gameId}/...`,
 * 구독 `/topic/omok/{gameId}`) 로 처리한다 — [OmokWsController] 참조.
 */
@RestController
@RequestMapping("/omok")
@Tag(name = "Omok", description = "그룹 멤버 간 실시간 오목 대전 — 초대/수락/대국")
class OmokController(
    private val omokService: OmokService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "오목 초대", description = "같은 그룹 멤버에게 오목 대국을 신청합니다. 상대에게 알림이 발송됩니다.")
    @PostMapping("/games")
    fun createGame(
        @AuthenticationPrincipal userId: String,
        @RequestParam groupRoomId: Long,
        @RequestBody request: CreateOmokGameRequest
    ): ResponseEntity<OmokGameResponse> {
        log.info(
            "api=POST /omok/games, userId={}, groupRoomId={}, inviteeUserId={}",
            userId,
            groupRoomId,
            request.inviteeUserId
        )
        return ResponseEntity.ok(
            omokService.createGame(UUID.fromString(userId), groupRoomId, request.inviteeUserId)
        )
    }

    @Operation(summary = "대국 조회", description = "대국 스냅샷(상태/보드/턴). 재접속 동기화용.")
    @GetMapping("/games/{gameId}")
    fun getGame(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<OmokGameResponse> {
        log.info("api=GET /omok/games/{}, userId={}", gameId, userId)
        return ResponseEntity.ok(omokService.getGame(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 수락", description = "대국이 시작되고 양쪽에 ACCEPTED 이벤트가 브로드캐스트됩니다.")
    @PostMapping("/games/{gameId}/accept")
    fun accept(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<OmokGameResponse> {
        log.info("api=POST /omok/games/{}/accept, userId={}", gameId, userId)
        return ResponseEntity.ok(omokService.accept(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 거절")
    @PostMapping("/games/{gameId}/decline")
    fun decline(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<OmokGameResponse> {
        log.info("api=POST /omok/games/{}/decline, userId={}", gameId, userId)
        return ResponseEntity.ok(omokService.decline(UUID.fromString(userId), gameId))
    }

    @Operation(summary = "초대 취소", description = "초대자가 상대 수락 전에 초대를 거둡니다.")
    @PostMapping("/games/{gameId}/cancel")
    fun cancel(
        @AuthenticationPrincipal userId: String,
        @PathVariable gameId: Long
    ): ResponseEntity<OmokGameResponse> {
        log.info("api=POST /omok/games/{}/cancel, userId={}", gameId, userId)
        return ResponseEntity.ok(omokService.cancel(UUID.fromString(userId), gameId))
    }
}
