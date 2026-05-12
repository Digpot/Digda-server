package digdaserver.domain.group_room.presentation.dto.res

import java.time.LocalDateTime

data class GroupRoomDeleteResponse(
    val deleteScheduledAt: LocalDateTime
)
