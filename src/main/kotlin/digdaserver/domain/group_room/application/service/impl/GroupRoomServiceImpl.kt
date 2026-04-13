package digdaserver.domain.group_room.application.service.impl

import digdaserver.domain.group_room.application.service.GroupRoomService
import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.group_room.domain.entity.GroupRoomRole
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.group_room.presentation.dto.req.CreateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.res.CreateGroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomResponse
import digdaserver.domain.invite.domain.entity.InviteCode
import digdaserver.domain.invite.domain.repository.InviteCodeRepository
import digdaserver.domain.membership.domain.entity.Membership
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class GroupRoomServiceImpl(
    private val groupRoomRepository: GroupRoomRepository,
    private val userRepository: UserRepository,
    private val membershipRepository: MembershipRepository,
    private val inviteCodeRepository: InviteCodeRepository
) : GroupRoomService {

    @Transactional
    override fun createGroupRoom(userId: UUID, request: CreateGroupRoomRequest): CreateGroupRoomResponse {
        validateGroupRoomName(request.name)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val groupRoom = groupRoomRepository.save(
            GroupRoom(
                name = request.name,
                maxMembers = request.maxMembers,
                owner = user,
                thumbnailImage = request.thumbnailImageId
            )
        )

        membershipRepository.save(
            Membership(
                user = user,
                groupRoom = groupRoom,
                role = GroupRoomRole.OWNER,
                color = generateRandomColor()
            )
        )

        val inviteCode = inviteCodeRepository.save(
            InviteCode(
                groupRoom = groupRoom,
                code = generateInviteCode(),
                expiresAt = LocalDateTime.now().plusHours(24)
            )
        )

        return CreateGroupRoomResponse(
            groupRoom = GroupRoomResponse.from(groupRoom, 1),
            inviteCode = inviteCode.code,
            inviteCodeExpiresAt = inviteCode.expiresAt
        )
    }

    private fun validateGroupRoomName(name: String) {
        if (name.length < 2) throw DigdaException(ErrorCode.GROUP_ROOM_NAME_TOO_SHORT)
        if (name.length > 20) throw DigdaException(ErrorCode.GROUP_ROOM_NAME_TOO_LONG)
    }

    private fun generateInviteCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun generateRandomColor(): String {
        val colors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8")
        return colors.random()
    }
}
