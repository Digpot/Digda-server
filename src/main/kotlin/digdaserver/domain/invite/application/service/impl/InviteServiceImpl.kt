package digdaserver.domain.invite.application.service.impl

import digdaserver.domain.group_room.application.service.impl.GroupRoomServiceImpl.Companion.MAX_GROUP_ROOMS_PER_USER
import digdaserver.domain.group_room.domain.entity.GroupRoomRole
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.MembershipSummary
import digdaserver.domain.invite.application.service.InviteService
import digdaserver.domain.invite.domain.entity.InviteCode
import digdaserver.domain.invite.domain.repository.InviteCodeRepository
import digdaserver.domain.invite.presentation.dto.res.InviteCodeResponse
import digdaserver.domain.invite.presentation.dto.res.InviteJoinResponse
import digdaserver.domain.invite.presentation.dto.res.InviteValidateResponse
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.membership.domain.entity.Membership
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class InviteServiceImpl(
    private val inviteCodeRepository: InviteCodeRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val membershipRepository: MembershipRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
    private val userActionLogService: UserActionLogService
) : InviteService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun regenerateInviteCode(userId: UUID, groupRoomId: Long): InviteCodeResponse {
        log.info("regenerateInviteCode: userId={}, groupRoomId={}", userId, groupRoomId)

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        // 유효한(만료 전) 코드가 있으면 그대로 재사용한다. 초대 시트를 열 때마다
        // 코드가 바뀌면 먼저 코드를 받은 사람이 입장하지 못하므로,
        // 새 코드는 기존 코드가 24시간 만료된 뒤에만 발급한다.
        val existing = inviteCodeRepository.findFirstByGroupRoomIdOrderByCreatedAtDesc(groupRoomId)
        if (existing.isPresent && !existing.get().isExpired) {
            val current = existing.get()
            log.info(
                "regenerateInviteCode: reuse valid code, groupRoomId={}, expiresAt={}",
                groupRoomId,
                current.expiresAt
            )
            return InviteCodeResponse(
                code = current.code,
                expiresAt = current.expiresAt
            )
        }

        inviteCodeRepository.deleteAllByGroupRoomId(groupRoomId)
        inviteCodeRepository.flush()

        val inviteCode = inviteCodeRepository.save(
            InviteCode(
                groupRoom = groupRoom,
                code = generateInviteCode(),
                expiresAt = LocalDateTime.now().plusHours(24)
            )
        )

        log.info(
            "regenerateInviteCode: issued new code, groupRoomId={}, expiresAt={}",
            groupRoomId,
            inviteCode.expiresAt
        )

        return InviteCodeResponse(
            code = inviteCode.code,
            expiresAt = inviteCode.expiresAt
        )
    }

    override fun validateInviteCode(userId: UUID, code: String): InviteValidateResponse {
        val inviteCode = inviteCodeRepository.findByCode(code.uppercase())
            .orElseThrow { DigdaException(ErrorCode.INVITE_CODE_INVALID) }

        if (inviteCode.isExpired) throw DigdaException(ErrorCode.INVITE_CODE_EXPIRED)

        val groupRoom = inviteCode.groupRoom

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.INVITE_CODE_INVALID)

        val memberCount = membershipRepository.countByGroupRoomId(groupRoom.id)

        if (memberCount >= groupRoom.maxMembers) throw DigdaException(ErrorCode.GROUP_ROOM_FULL)

        if (membershipRepository.existsByGroupRoomIdAndUserId(groupRoom.id, userId)) {
            throw DigdaException(ErrorCode.ALREADY_JOINED)
        }

        // 1인당 그룹방 개수 제한 — 가입 화면에서 미리 막아 안내한다.
        if (membershipRepository.countActiveByUserId(userId) >= MAX_GROUP_ROOMS_PER_USER) {
            throw DigdaException(ErrorCode.GROUP_ROOM_LIMIT_EXCEEDED)
        }

        return InviteValidateResponse(
            groupRoomName = groupRoom.name,
            thumbnailImage = groupRoom.thumbnailImage,
            memberCount = memberCount,
            maxMembers = groupRoom.maxMembers,
            expiresAt = inviteCode.expiresAt
        )
    }

    @Transactional
    override fun joinByInviteCode(userId: UUID, code: String): InviteJoinResponse {
        val inviteCode = inviteCodeRepository.findByCode(code.uppercase())
            .orElseThrow { DigdaException(ErrorCode.INVITE_CODE_INVALID) }

        if (inviteCode.isExpired) throw DigdaException(ErrorCode.INVITE_CODE_EXPIRED)

        val groupRoom = inviteCode.groupRoom

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.INVITE_CODE_INVALID)

        val memberCount = membershipRepository.countByGroupRoomId(groupRoom.id)

        if (memberCount >= groupRoom.maxMembers) throw DigdaException(ErrorCode.GROUP_ROOM_FULL)

        if (membershipRepository.existsByGroupRoomIdAndUserId(groupRoom.id, userId)) {
            throw DigdaException(ErrorCode.ALREADY_JOINED)
        }

        // 1인당 그룹방 개수 제한(생성+참여 합산). 실제 가입 시점에서도 재확인한다.
        if (membershipRepository.countActiveByUserId(userId) >= MAX_GROUP_ROOMS_PER_USER) {
            throw DigdaException(ErrorCode.GROUP_ROOM_LIMIT_EXCEEDED)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        if (user.restricted) throw DigdaException(ErrorCode.USER_RESTRICTED)

        membershipRepository.save(
            Membership(
                user = user,
                groupRoom = groupRoom,
                role = GroupRoomRole.MEMBER,
                color = generateRandomColor()
            )
        )

        groupRoom.updateLastActivity()

        notificationService.notifyMemberJoined(groupRoom.id, userId)

        userActionLogService.record(
            actorId = userId,
            action = UserAction.JOIN_GROUP_ROOM,
            targetType = "GROUP_ROOM",
            targetId = groupRoom.id.toString(),
            detail = "name=${groupRoom.name}, viaInviteCode=${inviteCode.code}"
        )

        val memberships = membershipRepository.findAllByGroupRoomId(groupRoom.id)

        return InviteJoinResponse(
            groupRoom = GroupRoomResponse.from(groupRoom, memberships.size),
            memberships = memberships.map { MembershipSummary.from(it) }
        )
    }

    private fun generateInviteCode(): String {
        val chars = ('0'..'9').toList()
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun generateRandomColor(): String {
        val colors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8")
        return colors.random()
    }
}
