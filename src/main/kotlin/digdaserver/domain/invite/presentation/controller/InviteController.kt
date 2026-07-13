package digdaserver.domain.invite.presentation.controller

import digdaserver.domain.invite.application.service.InviteService
import digdaserver.domain.invite.presentation.dto.req.InviteCodeRequest
import digdaserver.domain.invite.presentation.dto.res.InviteCodeResponse
import digdaserver.domain.invite.presentation.dto.res.InviteJoinResponse
import digdaserver.domain.invite.presentation.dto.res.InviteValidateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Invite", description = "초대 코드 API")
class InviteController(
    private val inviteService: InviteService
) {

    @Operation(
        summary = "초대 코드 조회/발급",
        description = "유효한(만료 전) 초대 코드가 있으면 그대로 반환하고, 없거나 만료된 경우에만 새 코드를 발급합니다. 방장만 가능합니다."
    )
    @PostMapping("/group-rooms/{groupRoomId}/invites")
    fun regenerateInviteCode(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<InviteCodeResponse> {
        val response = inviteService.regenerateInviteCode(UUID.fromString(userId), groupRoomId)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "초대 코드 검증", description = "초대 코드를 입력하면 그룹방 미리보기 정보를 반환합니다.")
    @PostMapping("/invites/validate")
    fun validateInviteCode(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: InviteCodeRequest
    ): ResponseEntity<InviteValidateResponse> {
        val response = inviteService.validateInviteCode(UUID.fromString(userId), request.code)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "초대 코드로 참여", description = "초대 코드를 통해 그룹방에 참여합니다.")
    @PostMapping("/invites/join")
    fun joinByInviteCode(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: InviteCodeRequest
    ): ResponseEntity<InviteJoinResponse> {
        val response = inviteService.joinByInviteCode(UUID.fromString(userId), request.code)
        return ResponseEntity.ok(response)
    }
}
