package digdaserver.domain.group_room.presentation.dto.res

import digdaserver.domain.membership.domain.entity.Membership

data class MembershipSummary(
    val userId: String,
    val name: String,
    val profileImage: String?,
    val role: String,
    val color: String
) {
    companion object {
        fun from(membership: Membership): MembershipSummary = MembershipSummary(
            userId = membership.user.id.toString(),
            name = membership.user.name,
            profileImage = membership.user.profileImage,
            role = membership.role.name.lowercase(),
            color = membership.color
        )
    }
}
