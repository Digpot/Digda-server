package digdaserver.domain.membership.presentation.dto.res

import digdaserver.domain.membership.domain.entity.Membership
import java.time.LocalDateTime
import java.util.UUID

data class MembershipResponse(
    val userId: UUID,
    val name: String,
    val profileImage: String?,
    val color: String,
    val role: String,
    val joinedAt: LocalDateTime
) {
    companion object {
        fun from(membership: Membership): MembershipResponse = MembershipResponse(
            userId = membership.user.id,
            name = membership.user.name,
            profileImage = membership.user.profileImage,
            color = membership.color,
            role = membership.role.name.lowercase(),
            joinedAt = membership.joinedAt
        )
    }
}
