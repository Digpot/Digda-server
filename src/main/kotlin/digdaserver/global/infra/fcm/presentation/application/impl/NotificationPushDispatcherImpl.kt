package digdaserver.global.infra.fcm.presentation.application.impl

import digdaserver.domain.device.domain.repository.DeviceRepository
import digdaserver.domain.notification.application.dto.NotificationPayload
import digdaserver.domain.notification.domain.entity.NotificationType
import digdaserver.domain.user.domain.entity.User
import digdaserver.domain.user.domain.entity.UserNotificationSetting
import digdaserver.global.infra.fcm.presentation.application.FcmService
import digdaserver.global.infra.fcm.presentation.application.NotificationPushDispatcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationPushDispatcherImpl(
    private val fcmService: FcmService,
    private val deviceRepository: DeviceRepository
) : NotificationPushDispatcher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun dispatch(recipients: List<User>, payload: NotificationPayload) {
        if (recipients.isEmpty()) return

        try {
            val eligibleUserIds = recipients
                .filter { isPushEligible(it.notificationSetting, payload.type) }
                .map { it.id }

            if (eligibleUserIds.isEmpty()) return

            val tokens = deviceRepository.findAllByUserIdIn(eligibleUserIds).map { it.token }
            if (tokens.isEmpty()) return

            val result = fcmService.sendToTokens(
                tokens = tokens,
                title = payload.title,
                body = payload.message,
                data = buildDataPayload(payload)
            )

            if (result.invalidTokens.isNotEmpty()) {
                log.warn(
                    "Removing {} invalid FCM token(s) after push dispatch (type={})",
                    result.invalidTokens.size,
                    payload.type
                )
                deviceRepository.deleteAllByTokenIn(result.invalidTokens)
            }
        } catch (e: Exception) {
            log.error("Push dispatch failed for type={}: {}", payload.type, e.message, e)
        }
    }

    private fun isPushEligible(setting: UserNotificationSetting?, type: NotificationType): Boolean {
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
            NotificationType.GROUP_DELETE_SCHEDULED,
            NotificationType.ANNOUNCEMENT -> true
        }
    }

    private fun buildDataPayload(payload: NotificationPayload): Map<String, String> = buildMap {
        put("type", payload.type.name)
        payload.groupRoomId?.let { put("groupRoomId", it.toString()) }
        payload.relatedId?.let { put("relatedId", it.toString()) }
        payload.relatedType?.let { put("relatedType", it) }
    }
}
