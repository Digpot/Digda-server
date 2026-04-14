package digdaserver.domain.invite.presentation.dto.res

import digdaserver.domain.group_room.presentation.dto.res.GroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.MembershipSummary

data class InviteJoinResponse(
    val groupRoom: GroupRoomResponse,
    val memberships: List<MembershipSummary>
)
