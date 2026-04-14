package digdaserver.domain.group_room.application.service.impl

import digdaserver.domain.group_room.application.service.GroupRoomService
import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.group_room.domain.entity.GroupRoomRole
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.group_room.presentation.dto.req.CreateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.req.UpdateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.res.CreateGroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDeleteResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDetailResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomListItem
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomListResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.MembershipSummary
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

    override fun getMyGroupRooms(userId: UUID): GroupRoomListResponse {
        val memberships = membershipRepository.findAllByUserIdWithGroupRoom(userId)

        val groupRoomItems = memberships.map { membership ->
            val memberCount = membershipRepository.countByGroupRoomId(membership.groupRoom.id)
            val inviteCode = if (membership.isOwner) {
                inviteCodeRepository.findFirstByGroupRoomIdOrderByCreatedAtDesc(membership.groupRoom.id)
                    .filter { !it.isExpired }
                    .map { it.code }
                    .orElse(null)
            } else {
                null
            }
            GroupRoomListItem.from(membership.groupRoom, memberCount, membership.role, inviteCode)
        }

        return GroupRoomListResponse(groupRooms = groupRoomItems)
    }

    override fun getGroupRoomDetail(userId: UUID, groupRoomId: Long): GroupRoomDetailResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val memberships = membershipRepository.findAllByGroupRoomId(groupRoomId)
        val memberCount = memberships.size

        return GroupRoomDetailResponse(
            groupRoom = GroupRoomResponse.from(groupRoom, memberCount),
            memberships = memberships.map { MembershipSummary.from(it) },
            myRole = membership.role.name.lowercase()
        )
    }

    @Transactional
    override fun updateGroupRoom(userId: UUID, groupRoomId: Long, request: UpdateGroupRoomRequest): GroupRoomResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        request.name?.let { validateGroupRoomName(it) }

        request.maxMembers?.let { newMax ->
            val currentCount = membershipRepository.countByGroupRoomId(groupRoomId)
            if (newMax < currentCount) throw DigdaException(ErrorCode.MAX_MEMBERS_BELOW_CURRENT)
        }

        groupRoom.update(
            name = request.name,
            maxMembers = request.maxMembers,
            thumbnailImage = null
        )

        // thumbnailImageId: null(미전송)=변경없음, Optional.empty=삭제, Optional(값)=변경
        request.thumbnailImageId?.let { optional ->
            if (optional.isPresent) {
                groupRoom.thumbnailImage = optional.get()
            } else {
                groupRoom.removeThumbnail()
            }
        }

        val memberCount = membershipRepository.countByGroupRoomId(groupRoomId)
        return GroupRoomResponse.from(groupRoom, memberCount)
    }

    @Transactional
    override fun deleteGroupRoom(userId: UUID, groupRoomId: Long): GroupRoomDeleteResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        groupRoom.scheduleDelete()

        return GroupRoomDeleteResponse(
            deleteScheduledAt = groupRoom.deleteScheduledAt!!
        )
    }

    @Transactional
    override fun recoverGroupRoom(userId: UUID, groupRoomId: Long): GroupRoomResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        if (!groupRoom.isDeleteScheduled) throw DigdaException(ErrorCode.GROUP_ROOM_NOT_SCHEDULED_FOR_DELETION)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        groupRoom.recover()

        val memberCount = membershipRepository.countByGroupRoomId(groupRoomId)
        return GroupRoomResponse.from(groupRoom, memberCount)
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
