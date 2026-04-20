package digdaserver.domain.notification.presentation.dto.res

import digdaserver.domain.notification.domain.entity.Notification
import digdaserver.domain.notification.domain.entity.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val groupRoomId: Long?,
    val groupRoomName: String?,
    val relatedId: Long?,
    val relatedType: String?,
    val isRead: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(notification: Notification): NotificationResponse = NotificationResponse(
            id = notification.id,
            type = notification.type,
            title = notification.title,
            message = notification.message,
            groupRoomId = notification.groupRoomId,
            groupRoomName = notification.groupRoomName,
            relatedId = notification.relatedId,
            relatedType = notification.relatedType,
            isRead = notification.isRead,
            createdAt = notification.createdAt
        )
    }
}
