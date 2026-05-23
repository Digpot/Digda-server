package digdaserver.domain.notification.application.service.impl

import digdaserver.domain.group_room.domain.entity.GroupRoom
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.dto.NotificationPayload
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.notification.domain.entity.Notification
import digdaserver.domain.notification.domain.entity.NotificationType
import digdaserver.domain.notification.domain.repository.NotificationRepository
import digdaserver.domain.notification.presentation.dto.res.NotificationListResponse
import digdaserver.domain.notification.presentation.dto.res.NotificationResponse
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.common.page.OffsetBasedPageRequest
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.fcm.presentation.application.NotificationPushDispatcher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val membershipRepository: MembershipRepository,
    private val groupRoomRepository: GroupRoomRepository,
    private val userRepository: UserRepository,
    private val pushDispatcher: NotificationPushDispatcher
) : NotificationService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getNotifications(userId: UUID, limit: Int, offset: Int): NotificationListResponse {
        log.info("userId={}, action=알림 목록 조회, limit={}, offset={}", userId, limit, offset)
        val safeLimit = limit.coerceIn(1, 100)
        val safeOffset = offset.coerceAtLeast(0)
        val pageable = OffsetBasedPageRequest.of(
            safeOffset,
            safeLimit,
            Sort.by(Sort.Direction.DESC, "createdAt")
        )

        val page = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
        val unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId)

        return NotificationListResponse(
            notifications = page.content.map { NotificationResponse.from(it) },
            total = page.totalElements,
            unreadCount = unreadCount,
            limit = safeLimit,
            offset = safeOffset,
            hasMore = (safeOffset + page.content.size) < page.totalElements
        )
    }

    @Transactional
    override fun markAsRead(userId: UUID, notificationId: Long, isRead: Boolean) {
        log.info(
            "userId={}, action=알림 읽음 처리, notificationId={}, isRead={}",
            userId,
            notificationId,
            isRead
        )
        val notification = requireOwnedNotification(userId, notificationId)
        if (isRead) notification.markAsRead()
    }

    @Transactional
    override fun markAllAsRead(userId: UUID) {
        log.info("userId={}, action=전체 알림 읽음 처리", userId)
        notificationRepository.markAllAsReadByUserId(userId)
    }

    @Transactional
    override fun deleteNotification(userId: UUID, notificationId: Long) {
        log.info("userId={}, action=알림 삭제, notificationId={}", userId, notificationId)
        val notification = requireOwnedNotification(userId, notificationId)
        notificationRepository.delete(notification)
    }

    @Transactional
    override fun notifyGroupRoomDeleteScheduled(groupRoomId: Long, actorUserId: UUID) {
        val groupRoom = findGroupRoom(groupRoomId)
        val recipients = memberRecipientsExcept(groupRoomId, actorUserId)

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.GROUP_DELETE_SCHEDULED,
                title = "그룹방 삭제 예약",
                message = "'${groupRoom.name}' 그룹방이 삭제 예약되었습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = groupRoomId,
                relatedType = "GROUP_ROOM"
            )
        )
    }

    @Transactional
    override fun notifyMemberJoined(groupRoomId: Long, joinedUserId: UUID) {
        val groupRoom = findGroupRoom(groupRoomId)
        val joinedUser = findUser(joinedUserId)
        val recipients = memberRecipientsExcept(groupRoomId, joinedUserId)

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.MEMBER_JOINED,
                title = "새 구성원 참여",
                message = "${joinedUser.name}님이 '${groupRoom.name}' 그룹방에 참여했습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name
            )
        )
    }

    @Transactional
    override fun notifyDiaryWritten(groupRoomId: Long, diaryId: Long, authorUserId: UUID, diaryTitle: String) {
        val groupRoom = findGroupRoom(groupRoomId)
        val author = findUser(authorUserId)
        val recipients = memberRecipientsExcept(groupRoomId, authorUserId)

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.DIARY_WRITTEN,
                title = "새 일기",
                message = "${author.name}님이 '$diaryTitle' 일기를 작성했습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = diaryId,
                relatedType = "DIARY"
            )
        )
    }

    @Transactional
    override fun notifyScheduleCreated(
        groupRoomId: Long,
        scheduleId: Long,
        creatorUserId: UUID,
        scheduleTitle: String,
        participantUserIds: List<UUID>
    ) {
        val recipientIds = participantUserIds.filter { it != creatorUserId }
        if (recipientIds.isEmpty()) return

        val groupRoom = findGroupRoom(groupRoomId)
        val creator = findUser(creatorUserId)
        val recipients = userRepository.findAllById(recipientIds).toList()

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.SCHEDULE_CREATED,
                title = "새 일정",
                message = "${creator.name}님이 '$scheduleTitle' 일정에 회원님을 참가자로 추가했습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = scheduleId,
                relatedType = "SCHEDULE"
            )
        )
    }

    @Transactional
    override fun notifyMemberLeft(groupRoomId: Long, leaverUserId: UUID) {
        val groupRoom = findGroupRoom(groupRoomId)
        val leaver = findUser(leaverUserId)
        val recipients = memberRecipientsExcept(groupRoomId, leaverUserId)

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.MEMBER_LEFT,
                title = "구성원 탈퇴",
                message = "${leaver.name}님이 '${groupRoom.name}' 그룹방에서 나갔습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name
            )
        )
    }

    @Transactional
    override fun notifyOwnershipTransferred(groupRoomId: Long, actorUserId: UUID, newOwnerUserId: UUID) {
        val groupRoom = findGroupRoom(groupRoomId)
        val newOwner = findUser(newOwnerUserId)
        val recipients = memberRecipientsExcept(groupRoomId, actorUserId)

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.OWNERSHIP_TRANSFERRED,
                title = "방장 권한 이양",
                message = "'${groupRoom.name}' 그룹방의 방장이 ${newOwner.name}님으로 변경되었습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = groupRoomId,
                relatedType = "GROUP_ROOM"
            )
        )
    }

    @Transactional
    override fun notifyScheduleParticipantsAdded(
        groupRoomId: Long,
        scheduleId: Long,
        actorUserId: UUID,
        scheduleTitle: String,
        addedParticipantUserIds: List<UUID>
    ) {
        val recipientIds = addedParticipantUserIds.filter { it != actorUserId }
        if (recipientIds.isEmpty()) return

        val groupRoom = findGroupRoom(groupRoomId)
        val actor = findUser(actorUserId)
        val recipients = userRepository.findAllById(recipientIds).toList()

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.SCHEDULE_UPDATED,
                title = "일정 참가자 추가",
                message = "${actor.name}님이 '$scheduleTitle' 일정에 회원님을 참가자로 추가했습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = scheduleId,
                relatedType = "SCHEDULE"
            )
        )
    }

    @Transactional
    override fun notifyScheduleReminderDayBefore(
        groupRoomId: Long,
        scheduleId: Long,
        scheduleTitle: String,
        recipientUserIds: List<UUID>
    ) {
        notifyScheduleReminder(
            groupRoomId = groupRoomId,
            scheduleId = scheduleId,
            recipientUserIds = recipientUserIds,
            type = NotificationType.SCHEDULE_DAY_BEFORE,
            title = "내일 일정",
            message = "내일 '$scheduleTitle' 일정이 예정되어 있습니다."
        )
    }

    @Transactional
    override fun notifyScheduleReminderToday(
        groupRoomId: Long,
        scheduleId: Long,
        scheduleTitle: String,
        recipientUserIds: List<UUID>
    ) {
        notifyScheduleReminder(
            groupRoomId = groupRoomId,
            scheduleId = scheduleId,
            recipientUserIds = recipientUserIds,
            type = NotificationType.SCHEDULE_TODAY,
            title = "오늘 일정",
            message = "오늘 '$scheduleTitle' 일정이 예정되어 있습니다."
        )
    }

    private fun notifyScheduleReminder(
        groupRoomId: Long,
        scheduleId: Long,
        recipientUserIds: List<UUID>,
        type: NotificationType,
        title: String,
        message: String
    ) {
        val uniqueRecipientIds = recipientUserIds.distinct()
        if (uniqueRecipientIds.isEmpty()) return

        // 동일 일정에 같은 종류의 리마인더가 이미 발송된 경우 중복 발송하지 않는다.
        if (notificationRepository.existsByTypeAndRelatedId(type, scheduleId)) return

        val groupRoom = findGroupRoom(groupRoomId)
        val recipients = userRepository.findAllById(uniqueRecipientIds).toList()
        if (recipients.isEmpty()) return

        notify(
            recipients,
            NotificationPayload(
                type = type,
                title = title,
                message = message,
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = scheduleId,
                relatedType = "SCHEDULE"
            )
        )
    }

    @Transactional
    override fun notifyCommentOnSchedule(
        groupRoomId: Long,
        scheduleId: Long,
        commenterUserId: UUID,
        scheduleTitle: String
    ) {
        val groupRoom = findGroupRoom(groupRoomId)
        val commenter = findUser(commenterUserId)
        // 일정 댓글은 그룹 멤버 전원에게 노출(작성자 본인 제외). 어떤 댓글이 달렸는지
        // 모든 멤버가 알 수 있게 하는 디그팟 그룹 다이어리 특성에 맞춤.
        val recipients = memberRecipientsExcept(groupRoomId, commenterUserId)

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.COMMENT_ON_SCHEDULE,
                title = "새 댓글",
                message = "${commenter.name}님이 '$scheduleTitle' 일정에 댓글을 남겼습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = scheduleId,
                relatedType = "SCHEDULE"
            )
        )
    }

    @Transactional
    override fun notifyCommentOnDiary(
        groupRoomId: Long,
        diaryId: Long,
        commenterUserId: UUID,
        diaryTitle: String
    ) {
        val groupRoom = findGroupRoom(groupRoomId)
        val commenter = findUser(commenterUserId)
        val recipients = memberRecipientsExcept(groupRoomId, commenterUserId)

        notify(
            recipients,
            NotificationPayload(
                type = NotificationType.COMMENT_ON_DIARY,
                title = "새 댓글",
                message = "${commenter.name}님이 '$diaryTitle' 일기에 댓글을 남겼습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name,
                relatedId = diaryId,
                relatedType = "DIARY"
            )
        )
    }

    @Transactional
    override fun notifyMemberRemoved(groupRoomId: Long, actorUserId: UUID, removedUserId: UUID) {
        val groupRoom = findGroupRoom(groupRoomId)
        val removedUser = findUser(removedUserId)

        // recipients 를 먼저 조회해 auto-flush 로 pending DELETE 를 선행 실행한 뒤
        // notify() 의 saveAll 이 뒤따르도록 순서를 고정한다.
        // (notify() 이후에 findAllByGroupRoomId 를 호출하면 auto-flush 시
        //  notification INSERT 와 membership DELETE 가 혼재해 제약 위반이 발생할 수 있음)
        val others = membershipRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.user }
            .filter { it.id != actorUserId && it.id != removedUserId }

        // 강퇴 당사자에게는 본인이 내보내졌다는 알림을 별도 메시지로 발송.
        notify(
            listOf(removedUser),
            NotificationPayload(
                type = NotificationType.MEMBER_REMOVED,
                title = "그룹방에서 내보내짐",
                message = "'${groupRoom.name}' 그룹방에서 내보내졌습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name
            )
        )

        // 나머지 멤버에게는 "OOO 님이 내보내졌다" 안내. 강퇴 액터(방장) + 당사자 제외.
        notify(
            others,
            NotificationPayload(
                type = NotificationType.MEMBER_REMOVED,
                title = "구성원 내보냄",
                message = "${removedUser.name}님이 '${groupRoom.name}' 그룹방에서 내보내졌습니다.",
                groupRoomId = groupRoomId,
                groupRoomName = groupRoom.name
            )
        )
    }

    @Transactional
    override fun sendAnnouncement(
        targetUserIds: List<UUID>?,
        title: String,
        body: String
    ): Int {
        val payload = NotificationPayload(
            type = NotificationType.ANNOUNCEMENT,
            title = title,
            message = body
        )

        if (!targetUserIds.isNullOrEmpty()) {
            val recipients = userRepository.findAllById(targetUserIds).toList()
            if (recipients.isEmpty()) return 0
            notify(recipients, payload)
            return recipients.size
        }

        // Broadcast to all users — process in batches to avoid loading the entire user table
        val allIds = userRepository.findAllIds()
        if (allIds.isEmpty()) return 0

        var notifiedCount = 0
        allIds.chunked(ANNOUNCEMENT_BATCH_SIZE) { batchIds ->
            val batch = userRepository.findAllById(batchIds).toList()
            if (batch.isNotEmpty()) {
                notify(batch, payload)
                notifiedCount += batch.size
            }
        }
        return notifiedCount
    }

    private fun notify(recipients: List<User>, payload: NotificationPayload) {
        if (recipients.isEmpty()) return

        val notifications = recipients.map { user -> payload.toEntity(user) }
        notificationRepository.saveAll(notifications)

        pushDispatcher.dispatch(recipients, payload)
    }

    private fun NotificationPayload.toEntity(recipient: User): Notification =
        Notification(
            user = recipient,
            type = type,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoomName,
            relatedId = relatedId,
            relatedType = relatedType
        )

    private fun requireOwnedNotification(userId: UUID, notificationId: Long): Notification {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { DigdaException(ErrorCode.NOTIFICATION_NOT_FOUND) }
        if (notification.user.id != userId) throw DigdaException(ErrorCode.FORBIDDEN)
        return notification
    }

    private fun findGroupRoom(groupRoomId: Long): GroupRoom =
        groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

    private fun findUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

    private fun memberRecipientsExcept(groupRoomId: Long, excludedUserId: UUID): List<User> =
        membershipRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.user }
            .filter { it.id != excludedUserId }

    companion object {
        private const val ANNOUNCEMENT_BATCH_SIZE = 500
    }
}
