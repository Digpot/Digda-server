package digdaserver.domain.notification.application.service.impl

import digdaserver.domain.device.domain.repository.DeviceRepository
import digdaserver.domain.group_room.domain.repository.GroupRoomRepository
import digdaserver.domain.membership.domain.repository.MembershipRepository
import digdaserver.domain.notification.application.service.NotificationService
import digdaserver.domain.notification.domain.entity.Notification
import digdaserver.domain.notification.domain.entity.NotificationType
import digdaserver.domain.notification.domain.repository.NotificationRepository
import digdaserver.domain.notification.presentation.dto.res.NotificationListResponse
import digdaserver.domain.notification.presentation.dto.res.NotificationResponse
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.entity.UserNotificationSetting
import digdaserver.domain.user.domain.repository.UserRepository
import digdaserver.global.infra.exception.error.DigdaException
import digdaserver.global.infra.exception.error.ErrorCode
import digdaserver.global.infra.fcm.presentation.application.FcmService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
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
    private val deviceRepository: DeviceRepository,
    private val fcmService: FcmService
) : NotificationService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getNotifications(userId: UUID, limit: Int, offset: Int): NotificationListResponse {
        val safeLimit = limit.coerceIn(1, 100)
        val safeOffset = offset.coerceAtLeast(0)
        val pageable = PageRequest.of(safeOffset / safeLimit, safeLimit)

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
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { DigdaException(ErrorCode.NOTIFICATION_NOT_FOUND) }

        if (notification.user.id != userId) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        if (isRead) {
            notification.markAsRead()
        }
    }

    @Transactional
    override fun markAllAsRead(userId: UUID) {
        notificationRepository.markAllAsReadByUserId(userId)
    }

    @Transactional
    override fun deleteNotification(userId: UUID, notificationId: Long) {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { DigdaException(ErrorCode.NOTIFICATION_NOT_FOUND) }

        if (notification.user.id != userId) {
            throw DigdaException(ErrorCode.FORBIDDEN)
        }

        notificationRepository.delete(notification)
    }

    @Transactional
    override fun notifyGroupRoomDeleteScheduled(groupRoomId: Long, actorUserId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val recipients = membershipRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.user }
            .filter { it.id != actorUserId }

        val title = "그룹방 삭제 예약"
        val message = "'${groupRoom.name}' 그룹방이 삭제 예약되었습니다."

        saveNotifications(
            recipients = recipients,
            type = NotificationType.GROUP_DELETE_SCHEDULED,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoom.name,
            relatedId = groupRoomId,
            relatedType = "GROUP_ROOM"
        )
    }

    @Transactional
    override fun notifyMemberJoined(groupRoomId: Long, joinedUserId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val joinedUser = userRepository.findById(joinedUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val recipients = membershipRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.user }
            .filter { it.id != joinedUserId }

        val title = "새 구성원 참여"
        val message = "${joinedUser.name}님이 '${groupRoom.name}' 그룹방에 참여했습니다."

        saveNotifications(
            recipients = recipients,
            type = NotificationType.MEMBER_JOINED,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoom.name,
            relatedId = null,
            relatedType = null
        )
    }

    @Transactional
    override fun notifyDiaryWritten(groupRoomId: Long, diaryId: Long, authorUserId: UUID, diaryTitle: String) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val author = userRepository.findById(authorUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val recipients = membershipRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.user }
            .filter { it.id != authorUserId }

        val title = "새 일기"
        val message = "${author.name}님이 '${diaryTitle}' 일기를 작성했습니다."

        saveNotifications(
            recipients = recipients,
            type = NotificationType.DIARY_WRITTEN,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoom.name,
            relatedId = diaryId,
            relatedType = "DIARY"
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
        if (participantUserIds.isEmpty()) return

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val creator = userRepository.findById(creatorUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val recipientIds = participantUserIds.filter { it != creatorUserId }
        if (recipientIds.isEmpty()) return

        val recipients = userRepository.findAllById(recipientIds).toList()

        val title = "새 일정"
        val message = "${creator.name}님이 '${scheduleTitle}' 일정에 회원님을 참가자로 추가했습니다."

        saveNotifications(
            recipients = recipients,
            type = NotificationType.SCHEDULE_CREATED,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoom.name,
            relatedId = scheduleId,
            relatedType = "SCHEDULE"
        )
    }

    @Transactional
    override fun notifyMemberLeft(groupRoomId: Long, leaverUserId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val leaver = userRepository.findById(leaverUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val recipients = membershipRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.user }
            .filter { it.id != leaverUserId }

        val title = "구성원 탈퇴"
        val message = "${leaver.name}님이 '${groupRoom.name}' 그룹방에서 나갔습니다."

        saveNotifications(
            recipients = recipients,
            type = NotificationType.MEMBER_LEFT,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoom.name,
            relatedId = null,
            relatedType = null
        )
    }

    @Transactional
    override fun notifyOwnershipTransferred(groupRoomId: Long, actorUserId: UUID, newOwnerUserId: UUID) {
        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val newOwner = userRepository.findById(newOwnerUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val recipients = membershipRepository.findAllByGroupRoomId(groupRoomId)
            .map { it.user }
            .filter { it.id != actorUserId }

        val title = "방장 권한 이양"
        val message = "'${groupRoom.name}' 그룹방의 방장이 ${newOwner.name}님으로 변경되었습니다."

        saveNotifications(
            recipients = recipients,
            type = NotificationType.OWNERSHIP_TRANSFERRED,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoom.name,
            relatedId = groupRoomId,
            relatedType = "GROUP_ROOM"
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

        val groupRoom = groupRoomRepository.findById(groupRoomId)
            .orElseThrow { DigdaException(ErrorCode.GROUP_ROOM_NOT_FOUND) }

        val actor = userRepository.findById(actorUserId)
            .orElseThrow { DigdaException(ErrorCode.USER_NOT_FOUND) }

        val recipients = userRepository.findAllById(recipientIds).toList()

        val title = "일정 참가자 추가"
        val message = "${actor.name}님이 '${scheduleTitle}' 일정에 회원님을 참가자로 추가했습니다."

        saveNotifications(
            recipients = recipients,
            type = NotificationType.SCHEDULE_UPDATED,
            title = title,
            message = message,
            groupRoomId = groupRoomId,
            groupRoomName = groupRoom.name,
            relatedId = scheduleId,
            relatedType = "SCHEDULE"
        )
    }

    private fun saveNotifications(
        recipients: List<User>,
        type: NotificationType,
        title: String,
        message: String,
        groupRoomId: Long?,
        groupRoomName: String?,
        relatedId: Long?,
        relatedType: String?
    ) {
        if (recipients.isEmpty()) return

        val notifications = recipients.map { user ->
            Notification(
                user = user,
                type = type,
                title = title,
                message = message,
                groupRoomId = groupRoomId,
                groupRoomName = groupRoomName,
                relatedId = relatedId,
                relatedType = relatedType
            )
        }
        notificationRepository.saveAll(notifications)

        sendPushNotifications(recipients, type, title, message, groupRoomId, relatedId, relatedType)
    }

    private fun sendPushNotifications(
        recipients: List<User>,
        type: NotificationType,
        title: String,
        message: String,
        groupRoomId: Long?,
        relatedId: Long?,
        relatedType: String?
    ) {
        try {
            val eligibleUserIds = recipients
                .filter { shouldSendPush(it.notificationSetting, type) }
                .map { it.id }

            if (eligibleUserIds.isEmpty()) return

            val devices = deviceRepository.findAllByUserIdIn(eligibleUserIds)
            if (devices.isEmpty()) return

            val tokens = devices.map { it.token }
            val data = buildMap {
                put("type", type.name)
                groupRoomId?.let { put("groupRoomId", it.toString()) }
                relatedId?.let { put("relatedId", it.toString()) }
                relatedType?.let { put("relatedType", it) }
            }

            val result = fcmService.sendToTokens(tokens, title, message, data)

            if (result.invalidTokens.isNotEmpty()) {
                deviceRepository.deleteAllByTokenIn(result.invalidTokens)
            }
        } catch (e: Exception) {
            log.error("FCM push send failed for type={}: {}", type, e.message, e)
        }
    }

    private fun shouldSendPush(setting: UserNotificationSetting?, type: NotificationType): Boolean {
        if (setting == null || !setting.pushEnabled) return false
        return when (type) {
            NotificationType.SCHEDULE_CREATED,
            NotificationType.SCHEDULE_UPDATED -> setting.scheduleNotification
            NotificationType.DIARY_WRITTEN -> setting.diaryNotification
            NotificationType.COMMENT_ON_SCHEDULE,
            NotificationType.COMMENT_ON_DIARY -> setting.commentNotification
            NotificationType.MEMBER_JOINED,
            NotificationType.MEMBER_LEFT,
            NotificationType.MEMBER_REMOVED,
            NotificationType.OWNERSHIP_TRANSFERRED,
            NotificationType.GROUP_DELETE_SCHEDULED -> true
        }
    }
}
