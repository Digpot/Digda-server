package digdaserver.domain.group_room.presentation.dto.res

import java.time.LocalDateTime

data class CreateGroupRoomResponse(
    val groupRoom: GroupRoomResponse,
    val inviteCode: String,
    val inviteCodeExpiresAt: LocalDateTime
)
