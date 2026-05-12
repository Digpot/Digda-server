@file:Suppress("ktlint:standard:package-name")

package digdaserver.domain.group_room.presentation.dto.res

data class GroupRoomListResponse(
    val groupRooms: List<GroupRoomListItem>
)
