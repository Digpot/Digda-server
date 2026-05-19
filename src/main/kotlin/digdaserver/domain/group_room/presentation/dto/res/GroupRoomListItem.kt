package digdaserver.domain.group_room.presentation.dto.res

import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.group_room.domain.entity.GroupRoomRole
import java.time.LocalDateTime

data class GroupRoomListItem(
    val id: Long,
    val name: String,
    val thumbnailImage: String?,
    val memberCount: Int,
    val maxMembers: Int,
    val myRole: String,
    val lastActivityAt: LocalDateTime,
    val isDeleteScheduled: Boolean,
    val deleteScheduledAt: LocalDateTime?,
    val inviteCode: String? = null
) {
    companion object {
        fun from(groupRoom: GroupRoom, memberCount: Int, myRole: GroupRoomRole, inviteCode: String? = null): GroupRoomListItem = GroupRoomListItem(
            id = groupRoom.id,
            name = groupRoom.name,
            thumbnailImage = groupRoom.thumbnailImage,
            memberCount = memberCount,
            maxMembers = groupRoom.maxMembers,
            myRole = myRole.name.lowercase(),
            lastActivityAt = groupRoom.lastActivityAt,
            isDeleteScheduled = groupRoom.isDeleteScheduled,
            deleteScheduledAt = groupRoom.deleteScheduledAt,
            inviteCode = inviteCode
        )
    }
}
