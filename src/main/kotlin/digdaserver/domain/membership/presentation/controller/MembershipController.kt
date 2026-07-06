package digdaserver.domain.membership.presentation.controller

import digdaserver.domain.membership.application.service.MembershipService
import digdaserver.domain.membership.presentation.dto.req.ChangeRoleRequest
import digdaserver.domain.membership.presentation.dto.res.MembershipListResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Membership", description = "구성원 관리 API")
class MembershipController(
    private val membershipService: MembershipService
) {

    @Operation(summary = "구성원 목록 조회", description = "그룹방의 전체 구성원 목록을 조회합니다.")
    @GetMapping("/group-rooms/{groupRoomId}/memberships")
    fun getMemberships(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<MembershipListResponse> {
        val response = membershipService.getMemberships(UUID.fromString(userId), groupRoomId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "구성원 내보내기 (추방)", description = "방장이 특정 구성원을 그룹방에서 내보냅니다.")
    @DeleteMapping("/group-rooms/{groupRoomId}/memberships/{targetUserId}")
    fun removeMember(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable targetUserId: UUID
    ): ResponseEntity<Void> {
        membershipService.removeMember(UUID.fromString(userId), groupRoomId, targetUserId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "역할 변경 (방장 양도)", description = "방장이 다른 구성원에게 방장 권한을 양도합니다.")
    @PutMapping("/group-rooms/{groupRoomId}/memberships/{targetUserId}/role")
    fun changeRole(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @PathVariable targetUserId: UUID,
        @RequestBody request: ChangeRoleRequest
    ): ResponseEntity<MembershipListResponse> {
        val response = membershipService.changeRole(UUID.fromString(userId), groupRoomId, targetUserId, request.role)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "그룹방 탈퇴", description = "현재 사용자가 그룹방에서 자발적으로 탈퇴합니다. 방장은 양도 후에만 가능합니다.")
    @PostMapping("/group-rooms/{groupRoomId}/leave")
    fun leaveGroupRoom(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<Void> {
        membershipService.leaveGroupRoom(UUID.fromString(userId), groupRoomId)
        return ResponseEntity.noContent().build()
    }
}
