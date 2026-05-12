@file:Suppress("ktlint:standard:package-name")

package digdaserver.domain.group_room.presentation.dto.req

data class CreateGroupRoomRequest(
    val name: String,
    val maxMembers: Int,
    val thumbnailImageId: String? = null
)
