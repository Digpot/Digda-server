package digdaserver.domain.invite.presentation.dto.res

import java.time.LocalDateTime

data class InviteCodeResponse(
    val code: String,
    val expiresAt: LocalDateTime
)
