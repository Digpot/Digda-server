@file:Suppress("ktlint:standard:package-name")

package digdaserver.domain.group_room.presentation.dto.res

data class GroupRoomDetailResponse(
    val groupRoom: GroupRoomResponse,
    val memberships: List<MembershipSummary>,
    val myRole: String
)
