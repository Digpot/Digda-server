package digdaserver.domain.group_room.presentation.dto.req

import java.util.Optional

data class UpdateGroupRoomRequest(
    val name: String? = null,
    val maxMembers: Int? = null,
    val thumbnailImageId: Optional<String>? = null
)
