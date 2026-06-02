package digdaserver.domain.group_room.presentation.controller

import digdaserver.domain.group_room.application.service.GroupRoomService
import digdaserver.domain.group_room.presentation.dto.req.CreateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.req.UpdateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.res.CreateGroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupHomeResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDeleteResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDetailResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomListResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/group-rooms")
@Tag(name = "GroupRoom", description = "그룹방 API")
class GroupRoomController(
    private val groupRoomService: GroupRoomService
) {

    @Operation(summary = "그룹방 생성", description = "그룹방을 생성합니다. 생성자가 방장이 되며 초대 코드가 자동 발급됩니다.")
    @PostMapping
    fun createGroupRoom(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: CreateGroupRoomRequest
    ): ResponseEntity<CreateGroupRoomResponse> {
        val response = groupRoomService.createGroupRoom(UUID.fromString(userId), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @Operation(summary = "내 그룹방 목록", description = "내가 속한 모든 그룹방을 최근 활동순으로 조회합니다.")
    @GetMapping
    fun getMyGroupRooms(
        @AuthenticationPrincipal userId: String
    ): ResponseEntity<GroupRoomListResponse> {
        val response = groupRoomService.getMyGroupRooms(UUID.fromString(userId))
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "그룹방 상세 조회", description = "그룹방의 상세 정보를 조회합니다. 구성원만 접근 가능합니다.")
    @GetMapping("/{groupRoomId}")
    fun getGroupRoomDetail(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<GroupRoomDetailResponse> {
        val response = groupRoomService.getGroupRoomDetail(UUID.fromString(userId), groupRoomId)
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "그룹 홈 대시보드",
        description = "그룹 홈 화면용 집계 — 오늘 요약(오늘 일정/새 일기/안읽음) + 활성 그룹(멤버·다가오는 일정)을 1회로 조회합니다. 구성원만 접근 가능합니다."
    )
    @GetMapping("/{groupRoomId}/home")
    fun getGroupHome(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<GroupHomeResponse> {
        val response = groupRoomService.getGroupHome(UUID.fromString(userId), groupRoomId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "그룹방 수정", description = "그룹방 정보를 수정합니다. 방장만 가능합니다.")
    @PutMapping("/{groupRoomId}")
    fun updateGroupRoom(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long,
        @RequestBody request: UpdateGroupRoomRequest
    ): ResponseEntity<GroupRoomResponse> {
        val response = groupRoomService.updateGroupRoom(UUID.fromString(userId), groupRoomId, request)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "그룹방 삭제", description = "그룹방을 삭제 예약합니다. 24시간 후 영구 삭제됩니다. 방장만 가능합니다.")
    @DeleteMapping("/{groupRoomId}")
    fun deleteGroupRoom(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<GroupRoomDeleteResponse> {
        val response = groupRoomService.deleteGroupRoom(UUID.fromString(userId), groupRoomId)
        return ResponseEntity.ok(response)
    }

    @Operation(summary = "그룹방 복구", description = "삭제 예약된 그룹방을 복구합니다. 삭제 예약 기간(24시간) 내에만 가능하며 방장만 가능합니다.")
    @PostMapping("/{groupRoomId}/recover")
    fun recoverGroupRoom(
        @AuthenticationPrincipal userId: String,
        @PathVariable groupRoomId: Long
    ): ResponseEntity<GroupRoomResponse> {
        val response = groupRoomService.recoverGroupRoom(UUID.fromString(userId), groupRoomId)
        return ResponseEntity.ok(response)
    }
}
