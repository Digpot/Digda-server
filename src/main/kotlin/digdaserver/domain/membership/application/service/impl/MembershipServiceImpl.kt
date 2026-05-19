package digdaserver.domain.membership.application.service.impl

import digdaserver.domain.group_room.domain.entity.GroupRoomRole
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.membership.application.service.MembershipService
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.membership.presentation.dto.res.MembershipListResponse
import digdaserver.domain.membership.presentation.dto.res.MembershipResponse
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class MembershipServiceImpl(
    private val membershipRepository: MembershipRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val notificationService: NotificationService,
    private val userActionLogService: UserActionLogService
) : MembershipService {

    override fun getMemberships(userId: UUID, groupRoomId: Long): MembershipListResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val memberships = membershipRepository.findAllByGroupRoomId(groupRoomId)

        return MembershipListResponse(
            memberships = memberships.map { MembershipResponse.from(it) }
        )
    }

    @Transactional
    override fun removeMember(userId: UUID, groupRoomId: Long, targetUserId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        val targetMembership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, targetUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_IN_GROUP_ROOM) }

        if (targetMembership.isOwner) throw DigdaException(ErrorCode.CANNOT_REMOVE_OWNER)

        membershipRepository.delete(targetMembership)

        notificationService.notifyMemberRemoved(groupRoomId, userId, targetUserId)

        userActionLogService.record(
            actorId = userId,
            action = UserAction.REMOVE_MEMBER,
            targetType = "GROUP_ROOM",
            targetId = groupRoomId.toString(),
            detail = "removedUserId=$targetUserId"
        )
    }

    @Transactional
    override fun changeRole(userId: UUID, groupRoomId: Long, targetUserId: UUID, role: String): MembershipListResponse {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        val targetMembership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, targetUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_IN_GROUP_ROOM) }

        val newRole = try {
            GroupRoomRole.valueOf(role.uppercase())
        } catch (e: IllegalArgumentException) {
            throw DigdaException(ErrorCode.INVALID_ROLE)
        }

        if (newRole != GroupRoomRole.OWNER) throw DigdaException(ErrorCode.INVALID_ROLE)

        membership.changeRole(GroupRoomRole.MEMBER)
        targetMembership.changeRole(GroupRoomRole.OWNER)

        notificationService.notifyOwnershipTransferred(groupRoomId, userId, targetUserId)

        userActionLogService.record(
            actorId = userId,
            action = UserAction.TRANSFER_OWNER,
            targetType = "GROUP_ROOM",
            targetId = groupRoomId.toString(),
            detail = "targetUserId=$targetUserId"
        )

        val memberships = membershipRepository.findAllByGroupRoomId(groupRoomId)

        return MembershipListResponse(
            memberships = memberships.map { MembershipResponse.from(it) }
        )
    }

    @Transactional
    override fun leaveGroupRoom(userId: UUID, groupRoomId: Long) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (membership.isOwner) throw DigdaException(ErrorCode.OWNER_CANNOT_LEAVE)

        membershipRepository.delete(membership)

        notificationService.notifyMemberLeft(groupRoomId, userId)

        userActionLogService.record(
            actorId = userId,
            action = UserAction.LEAVE_GROUP_ROOM,
            targetType = "GROUP_ROOM",
            targetId = groupRoomId.toString(),
            detail = "name=${groupRoom.name}"
        )
    }
}
