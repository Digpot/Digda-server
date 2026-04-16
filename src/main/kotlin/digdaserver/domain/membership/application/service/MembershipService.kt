package digdaserver.domain.membership.application.service

import digdaserver.domain.membership.presentation.dto.res.MembershipListResponse
import java.util.UUID

interface MembershipService {

    fun getMemberships(userId: UUID, groupRoomId: Long): MembershipListResponse

    fun removeMember(userId: UUID, groupRoomId: Long, targetUserId: UUID)

    fun changeRole(userId: UUID, groupRoomId: Long, targetUserId: UUID, role: String): MembershipListResponse

    fun leaveGroupRoom(userId: UUID, groupRoomId: Long)
}
