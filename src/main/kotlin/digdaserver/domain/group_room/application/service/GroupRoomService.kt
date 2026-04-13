package digdaserver.domain.group_room.application.service

import digdaserver.domain.group_room.presentation.dto.req.CreateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.req.UpdateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.res.CreateGroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDetailResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDeleteResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomListResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomResponse
import java.util.UUID

interface GroupRoomService {

    fun createGroupRoom(userId: UUID, request: CreateGroupRoomRequest): CreateGroupRoomResponse

    fun getMyGroupRooms(userId: UUID): GroupRoomListResponse

    fun getGroupRoomDetail(userId: UUID, groupRoomId: Long): GroupRoomDetailResponse

    fun updateGroupRoom(userId: UUID, groupRoomId: Long, request: UpdateGroupRoomRequest): GroupRoomResponse

    fun deleteGroupRoom(userId: UUID, groupRoomId: Long): GroupRoomDeleteResponse
}
