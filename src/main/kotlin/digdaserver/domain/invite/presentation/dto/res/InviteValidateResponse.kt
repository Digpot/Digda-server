package digdaserver.domain.invite.presentation.dto.res

import java.time.LocalDateTime

data class InviteValidateResponse(
    val groupRoomName: String,
    val thumbnailImage: String?,
    val memberCount: Int,
    val maxMembers: Int,
    val expiresAt: LocalDateTime
)
