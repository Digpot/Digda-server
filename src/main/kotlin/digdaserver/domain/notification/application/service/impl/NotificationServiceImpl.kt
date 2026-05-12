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

    override fun getNotifications(userId: UUID, limit: Int, offset: Int): NotificationListResponse {
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
        val notification = requireOwnedNotification(userId, notificationId)
        if (isRead) notification.markAsRead()
    }

    @Transactional
    override fun markAllAsRead(userId: UUID) {
        notificationRepository.markAllAsReadByUserId(userId)
    }

    @Transactional
    override fun deleteNotification(userId: UUID, notificationId: Long) {
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
