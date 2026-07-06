package digdaserver.domain.block.presentation.controller

import digdaserver.domain.block.application.service.BlockService
import digdaserver.domain.block.domain.entity.HideReason
import digdaserver.domain.block.presentation.dto.req.HideContentRequest
import digdaserver.domain.block.presentation.dto.res.BlockedUserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Block", description = "차단/숨김 API")
class BlockController(
    private val blockService: BlockService
) {

    @Operation(summary = "사용자 차단", description = "해당 사용자의 모든 일기·댓글·일정을 내 화면에서 숨깁니다(전역·단방향).")
    @PostMapping("/blocks/users/{userId}")
    fun blockUser(
        @AuthenticationPrincipal myId: String,
        @PathVariable userId: String
    ): ResponseEntity<Void> {
        blockService.blockUser(UUID.fromString(myId), UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "사용자 차단 해제")
    @DeleteMapping("/blocks/users/{userId}")
    fun unblockUser(
        @AuthenticationPrincipal myId: String,
        @PathVariable userId: String
    ): ResponseEntity<Void> {
        blockService.unblockUser(UUID.fromString(myId), UUID.fromString(userId))
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "차단 목록 조회", description = "마이페이지 — 내가 차단한 사용자 목록(최신순).")
    @GetMapping("/blocks/users")
    fun listBlockedUsers(
        @AuthenticationPrincipal myId: String
    ): ResponseEntity<List<BlockedUserResponse>> {
        return ResponseEntity.ok(blockService.listBlockedUsers(UUID.fromString(myId)))
    }

    @Operation(summary = "게시물 숨기기", description = "신고 없이 개별 일기/댓글/일정을 내 화면에서만 숨깁니다.")
    @PostMapping("/blocks/content")
    fun hideContent(
        @AuthenticationPrincipal myId: String,
        @RequestBody request: HideContentRequest
    ): ResponseEntity<Void> {
        blockService.hideContent(UUID.fromString(myId), request.targetType, request.targetId, HideReason.HIDDEN)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "게시물 숨김 해제")
    @DeleteMapping("/blocks/content")
    fun unhideContent(
        @AuthenticationPrincipal myId: String,
        @RequestBody request: HideContentRequest
    ): ResponseEntity<Void> {
        blockService.unhideContent(UUID.fromString(myId), request.targetType, request.targetId)
        return ResponseEntity.noContent().build()
    }
}
