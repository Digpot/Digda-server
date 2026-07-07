package digdaserver.domain.group_room.application.service.impl

import digdaserver.domain.diary.domain.repository.DiaryRepository
import digdaserver.domain.group_room.application.service.GroupRoomService
import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.group_room.domain.entity.GroupRoomRole
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.group_room.presentation.dto.req.CreateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.req.UpdateGroupRoomRequest
import digdaserver.domain.group_room.presentation.dto.res.ActiveGroupResponse
import digdaserver.domain.group_room.presentation.dto.res.CreateGroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupHomeResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDeleteResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomDetailResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomListItem
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomListResponse
import digdaserver.domain.group_room.presentation.dto.res.GroupRoomResponse
import digdaserver.domain.group_room.presentation.dto.res.MembershipSummary
import digdaserver.domain.group_room.presentation.dto.res.NextEventResponse
import digdaserver.domain.group_room.presentation.dto.res.TodaySummaryResponse
import digdaserver.domain.invite.domain.entity.InviteCode
import digdaserver.domain.invite.domain.repository.InviteCodeRepository
import digdaserver.domain.log.application.service.UserActionLogService
import digdaserver.domain.log.domain.entity.UserAction
import digdaserver.domain.membership.domain.entity.Membership
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.schedule.domain.repository.ScheduleRepository
import digdaserver.domain.upload.domain.repository.UploadedImageRepository
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

@Service
@Transactional(readOnly = true)
class GroupRoomServiceImpl(
    private val groupRoomRepository: GroupRoomRepository,
    private val userRepository: UserRepository,
    private val membershipRepository: MembershipRepository,
    private val inviteCodeRepository: InviteCodeRepository,
    private val notificationService: NotificationService,
    private val userActionLogService: UserActionLogService,
    private val uploadedImageRepository: UploadedImageRepository,
    private val scheduleRepository: ScheduleRepository,
    private val diaryRepository: DiaryRepository
) : GroupRoomService {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 클라이언트가 보내는 thumbnailImageId(=UploadedImage 의 PK 문자열) 를 실제 S3 URL 로 변환.
     * 입력이 null/blank/숫자가 아니면 null 을 반환해 호출 측에서 변경 없음으로 처리한다.
     */
    private fun resolveImageUrl(imageId: String?): String? {
        if (imageId.isNullOrBlank()) return null
        val id = imageId.toLongOrNull() ?: run {
            log.warn("imageId 가 Long 이 아님: imageId={}", imageId)
            return null
        }
        val image = uploadedImageRepository.findById(id).orElse(null)
        if (image == null) {
            log.warn("imageId 에 해당하는 업로드 레코드 없음: imageId={}", id)
            return null
        }
        return image.url
    }

    @Transactional
    override fun createGroupRoom(userId: UUID, request: CreateGroupRoomRequest): CreateGroupRoomResponse {
        log.info("userId={}, action=그룹 생성 요청, name={}", userId, request.name)
        validateGroupRoomName(request.name)

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        if (user.restricted) throw DigdaException(ErrorCode.USER_RESTRICTED)

        // 1인당 그룹방은 최대 MAX_GROUP_ROOMS_PER_USER 개 (생성+참여 합산).
        if (membershipRepository.countActiveByUserId(userId) >= MAX_GROUP_ROOMS_PER_USER) {
            throw DigdaException(ErrorCode.GROUP_ROOM_LIMIT_EXCEEDED)
        }

        val thumbnailUrl = resolveImageUrl(request.thumbnailImageId)

        val groupRoom = groupRoomRepository.save(
            GroupRoom(
                name = request.name,
                maxMembers = request.maxMembers,
                owner = user,
                thumbnailImage = thumbnailUrl
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

        userActionLogService.record(
            actorId = userId,
            action = UserAction.CREATE_GROUP_ROOM,
            targetType = "GROUP_ROOM",
            targetId = groupRoom.id.toString(),
            detail = "name=${groupRoom.name}"
        )

        log.info(
            "userId={}, action=그룹 생성 완료, groupRoomId={}, thumbnail={}",
            userId,
            groupRoom.id,
            groupRoom.thumbnailImage
        )

        return CreateGroupRoomResponse(
            groupRoom = GroupRoomResponse.from(groupRoom, 1),
            inviteCode = inviteCode.code,
            inviteCodeExpiresAt = inviteCode.expiresAt
        )
    }

    override fun getMyGroupRooms(userId: UUID): GroupRoomListResponse {
        log.info("userId={}, action=내 그룹방 목록 조회", userId)
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
        log.info("userId={}, action=그룹 상세 조회, groupRoomId={}", userId, groupRoomId)
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

    override fun getGroupHome(userId: UUID, groupRoomId: Long): GroupHomeResponse {
        log.info("userId={}, action=그룹 홈 조회, groupRoomId={}", userId, groupRoomId)

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        // 구성원만 접근 가능 (상세 조회와 동일 정책).
        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        val user = userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val memberships = membershipRepository.findAllByGroupRoomId(groupRoomId)
        val today = LocalDate.now(KST)

        // 오늘 요약: 오늘에 걸치는 일정 수 / 오늘 작성된 일기 수 / 안읽음 알림 수(집계값).
        val scheduleCount =
            scheduleRepository.findAllByGroupRoomIdAndDateRange(groupRoomId, today, today).size
        val newDiaryCount = diaryRepository
            .findAllByGroupRoomIdAndDateBetween(groupRoomId, today, today, PageRequest.of(0, 1))
            .totalElements.toInt()
        val unreadCount = notificationService.getNotifications(userId, 1, 0).unreadCount

        // 다가오는 일정: 오늘 이후 1년 범위에서 시작일·시작시각이 가장 이른 1건.
        val nextEvent = scheduleRepository
            .findAllByGroupRoomIdAndDateRange(groupRoomId, today, today.plusYears(1))
            .sortedWith(compareBy({ it.startDate }, { it.startTime ?: LocalTime.MIN }))
            .firstOrNull()

        return GroupHomeResponse(
            userName = user.displayedName(),
            today = TodaySummaryResponse(
                scheduleCount = scheduleCount,
                newDiaryCount = newDiaryCount,
                unreadCount = unreadCount
            ),
            activeGroup = ActiveGroupResponse(
                id = groupRoom.id,
                name = groupRoom.name,
                thumbnailImage = groupRoom.thumbnailImage,
                memberCount = memberships.size,
                myRole = membership.role.name.lowercase(),
                members = memberships.map { MembershipSummary.from(it) },
                nextEvent = nextEvent?.let { NextEventResponse.from(it) }
            )
        )
    }

    @Transactional
    override fun updateGroupRoom(userId: UUID, groupRoomId: Long, request: UpdateGroupRoomRequest): GroupRoomResponse {
        log.info(
            "userId={}, action=그룹 수정 요청, groupRoomId={}, fields=[name={}, maxMembers={}, thumbnailImageId={}]",
            userId,
            groupRoomId,
            request.name,
            request.maxMembers,
            request.thumbnailImageId
        )

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

        // thumbnailImageId: null(미전송)=변경없음, Optional.empty=삭제, Optional(값)=변경.
        // 값이 전달되면 UploadedImage 의 PK 문자열이므로 실제 S3 URL 로 변환해 저장한다.
        // (기존엔 PK 문자열을 그대로 저장하여 클라이언트 리스트 썸네일이 깨졌음)
        request.thumbnailImageId?.let { optional ->
            if (optional.isPresent) {
                val resolvedUrl = resolveImageUrl(optional.get())
                if (resolvedUrl != null) {
                    groupRoom.thumbnailImage = resolvedUrl
                } else {
                    log.warn(
                        "userId={}, action=그룹 썸네일 변경 무시(업로드 lookup 실패), groupRoomId={}, imageId={}",
                        userId,
                        groupRoomId,
                        optional.get()
                    )
                }
            } else {
                groupRoom.removeThumbnail()
            }
        }

        log.info(
            "userId={}, action=그룹 수정 완료, groupRoomId={}, thumbnail={}",
            userId,
            groupRoomId,
            groupRoom.thumbnailImage
        )

        val memberCount = membershipRepository.countByGroupRoomId(groupRoomId)
        return GroupRoomResponse.from(groupRoom, memberCount)
    }

    @Transactional
    override fun deleteGroupRoom(userId: UUID, groupRoomId: Long): GroupRoomDeleteResponse {
        log.info("userId={}, action=그룹 삭제 요청, groupRoomId={}", userId, groupRoomId)

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        groupRoom.scheduleDelete()

        notificationService.notifyGroupRoomDeleteScheduled(groupRoomId, userId)

        log.info(
            "userId={}, action=그룹 삭제 예약 완료, groupRoomId={}, deleteScheduledAt={}",
            userId,
            groupRoomId,
            groupRoom.deleteScheduledAt
        )

        return GroupRoomDeleteResponse(
            deleteScheduledAt = groupRoom.deleteScheduledAt!!
        )
    }

    @Transactional
    override fun recoverGroupRoom(userId: UUID, groupRoomId: Long): GroupRoomResponse {
        log.info("userId={}, action=그룹 복구 요청, groupRoomId={}", userId, groupRoomId)

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        if (groupRoom.deletedAt != null) throw DigdaException(ErrorCode.GROUP_ROOM_ALREADY_DELETED)

        if (!groupRoom.isDeleteScheduled) throw DigdaException(ErrorCode.GROUP_ROOM_NOT_SCHEDULED_FOR_DELETION)

        val membership = membershipRepository.findByGroupRoomIdAndUserId(groupRoomId, userId)
            .orElseThrow { DigdaException(ErrorCode.NOT_GROUP_ROOM_MEMBER) }

        if (!membership.isOwner) throw DigdaException(ErrorCode.NOT_GROUP_ROOM_OWNER)

        groupRoom.recover()

        log.info("userId={}, action=그룹 복구 완료, groupRoomId={}", userId, groupRoomId)

        val memberCount = membershipRepository.countByGroupRoomId(groupRoomId)
        return GroupRoomResponse.from(groupRoom, memberCount)
    }

    private fun validateGroupRoomName(name: String) {
        if (name.length < 2) throw DigdaException(ErrorCode.GROUP_ROOM_NAME_TOO_SHORT)
        if (name.length > 20) throw DigdaException(ErrorCode.GROUP_ROOM_NAME_TOO_LONG)
    }

    private fun generateInviteCode(): String {
        val chars = ('0'..'9').toList()
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun generateRandomColor(): String {
        val colors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8")
        return colors.random()
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")

        /** 1인당 참여 가능한 그룹방 최대 개수(생성+참여 합산). */
        const val MAX_GROUP_ROOMS_PER_USER = 6L
    }
}
