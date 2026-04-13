package digdaserver.domain.group_room.application.service

import digdaserver.domain.group_room.presentation.dto.req.CreateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.res.CreateGroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomListResponse
import java.util.UUID

interface GroupRoomService {

    fun createGroupRoom(userId: UUID, request: CreateGroupRoomRequest): CreateGroupRoomResponse

    fun getMyGroupRooms(userId: UUID): GroupRoomListResponse
}
