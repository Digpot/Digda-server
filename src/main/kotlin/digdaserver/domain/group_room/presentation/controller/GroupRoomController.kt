package digdaserver.domain.group_room.presentation.controller

import digdaserver.domain.group_room.application.service.GroupRoomService
import digdaserver.domain.group_room.presentation.dto.req.CreateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.res.CreateGroupRoomResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
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
}
