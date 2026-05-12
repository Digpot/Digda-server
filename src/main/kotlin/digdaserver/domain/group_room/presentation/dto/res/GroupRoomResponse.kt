@file:Suppress("ktlint:standard:package-name")

package digdaserver.domain.group_room.presentation.dto.res

import digdaserver.domain.group_room.domain.entity.GroupRoom
import java.time.LocalDateTime

data class GroupRoomResponse(
    val id: Long,
    val name: String,
    val thumbnailImage: String?,
    val maxMembers: Int,
    val memberCount: Int,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(groupRoom: GroupRoom, memberCount: Int): GroupRoomResponse = GroupRoomResponse(
            id = groupRoom.id,
            name = groupRoom.name,
            thumbnailImage = groupRoom.thumbnailImage,
            maxMembers = groupRoom.maxMembers,
            memberCount = memberCount,
            createdAt = groupRoom.createdAt
        )
    }
}
