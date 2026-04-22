package digdaserver.admin.grouproom.presentation.controller

import digdaserver.admin.common.dto.res.AdminPageResponse
import digdaserver.admin.grouproom.application.service.AdminGroupRoomService
import digdaserver.admin.grouproom.presentation.dto.req.AdminGroupRoomStatusRequest
import digdaserver.admin.grouproom.presentation.dto.res.AdminGroupRoomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/group-rooms")
@Tag(name = "Admin - GroupRoom", description = "관리자 그룹방 관리 API")
class AdminGroupRoomController(
    private val adminGroupRoomService: AdminGroupRoomService
) {

    @Operation(summary = "그룹방 목록 조회", description = "페이징, 키워드(이름), 삭제 포함 여부 필터")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "true") includeDeleted: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<AdminPageResponse<AdminGroupRoomResponse>> {
        return ResponseEntity.ok(adminGroupRoomService.search(keyword, includeDeleted, page, size))
    }

    @Operation(summary = "그룹방 상세 조회")
    @GetMapping("/{groupRoomId}")
    fun getDetail(@PathVariable groupRoomId: Long): ResponseEntity<AdminGroupRoomResponse> {
        return ResponseEntity.ok(adminGroupRoomService.getDetail(groupRoomId))
    }

    @Operation(summary = "그룹방 상태 변경", description = "RECOVER/SCHEDULE_DELETE/HARD_DELETE")
    @PatchMapping("/{groupRoomId}/status")
    fun changeStatus(
        @PathVariable groupRoomId: Long,
        @Valid @RequestBody request: AdminGroupRoomStatusRequest
    ): ResponseEntity<AdminGroupRoomResponse> {
        return ResponseEntity.ok(adminGroupRoomService.changeStatus(groupRoomId, request.action))
    }
}
