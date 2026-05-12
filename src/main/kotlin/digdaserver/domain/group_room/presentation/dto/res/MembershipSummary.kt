@file:Suppress("ktlint:standard:package-name")

package digdaserver.domain.group_room.presentation.dto.res

import digdaserver.domain.membership.domain.entity.Membership

data class MembershipSummary(
    val name: String,
    val profileImage: String?,
    val role: String,
    val color: String
) {
    companion object {
        fun from(membership: Membership): MembershipSummary = MembershipSummary(
            name = membership.user.name,
            profileImage = membership.user.profileImage,
            role = membership.role.name.lowercase(),
            color = membership.color
        )
    }
}
